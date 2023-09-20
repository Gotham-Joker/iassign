package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.iassign.entity.SysRole;
import com.github.iassign.entity.SysUser;
import com.github.iassign.mapper.SysRoleMapper;
import com.github.iassign.mapper.SysUserMapper;
import com.github.authorization.UserDetails;
import com.github.iassign.ProcessLogger;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.entity.ProcessTaskAuth;
import com.github.iassign.enums.ProcessInstanceStatus;
import com.github.iassign.mapper.*;
import com.github.iassign.vo.ProcessTaskTodoQuery;
import com.github.iassign.enums.ProcessTaskStatus;
import com.github.iassign.vo.TaskAuthVO;
import com.github.iassign.vo.TaskTodoVO;
import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.base.BaseService;
import com.github.core.ApiException;
import com.github.core.JsonUtil;
import com.github.iassign.core.dag.DagEdge;
import com.github.iassign.core.dag.node.DagNode;
import com.github.iassign.core.dag.node.StartNode;
import com.github.iassign.core.dag.node.UserTaskNode;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.entity.ProcessVariables;
import com.github.core.PageResult;
import com.github.core.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.iassign.Constants.MAIL_PATTERN;
import static com.github.iassign.enums.ProcessTaskStatus.*;

@Slf4j
@Service
public class ProcessTaskService extends BaseService<ProcessTask> {
    @Autowired
    private ProcessTaskMapper processTaskMapper;
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private ProcessVariablesService processVariablesService;
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private FormInstanceMapper formInstanceMapper;
    @Autowired
    private ProcessTaskAuthMapper processTaskAuthMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private ProcessMailService processMailService;
    @Autowired
    private SysMessageService sysMessageService;


    /**
     * 退回
     * 退回也有可能上传附件，添加备注之类的，因此也要记录下来
     * 退回不应该包含任务变量，会忽略
     * 还有，所谓退回到指定环节，其实是将指定环节复制一份，然后回填基本信息
     *
     * @param task
     * @param dto
     * @return 退回到指定的任务
     */
    public ProcessTask back(ProcessTask task, ProcessTaskDTO dto) {
        if (!StringUtils.hasText(dto.backwardTaskId)) {
            throw new ApiException(422, "请指定回退环节");
        }
        task.remark = dto.safeRemark();
        task.attachments = dto.attachments;
        // 当前任务标记为退回
        task.status = ProcessTaskStatus.BACK;
        updateById(task);

        // 从指定的任务复制一个新任务出来
        ProcessTask backwardTask = processTaskMapper.selectById(dto.backwardTaskId);
        ProcessTask copyTask = new ProcessTask();
        copyTask.definitionId = backwardTask.definitionId;
        copyTask.instanceId = backwardTask.instanceId;
        copyTask.formId = backwardTask.formId;
        copyTask.dagNodeId = backwardTask.dagNodeId;
        copyTask.incomeId = backwardTask.incomeId;
        copyTask.preHandlerId = backwardTask.preHandlerId;
        copyTask.name = backwardTask.name;
        copyTask.status = PENDING; // 待受理状态
        copyTask.userNode = backwardTask.userNode;
        copyTask.createTime = new Date();
        processTaskMapper.insert(copyTask);
        return copyTask;
    }

    /**
     * 直接拒绝这个任务，对应的流程实例应当取消
     */
    public void reject(ProcessTask task, ProcessTaskDTO dto) {
        task.remark = dto.safeRemark();
        task.attachments = dto.attachments;
        task.status = ProcessTaskStatus.REJECTED;
        updateById(task);
    }

    /**
     * 受理，受理之后发通知？
     */
    public Result claim(ProcessClaimAssignDTO dto) {
        // 校验
        ProcessTask task = processTaskMapper.selectById(dto.taskId);
        if (task.status != PENDING) {
            return Result.error("任务已被受理或指派，不能重复受理"); // 不能重复受理
        }
        ProcessInstance instance = processInstanceMapper.selectById(task.instanceId);
        if (instance.status != ProcessInstanceStatus.RUNNING) {
            return Result.error("流程不处于运行状态，无法受理");
        }
        task.handlerId = dto.userId;
        task.status = ProcessTaskStatus.CLAIMED;
        task.handlerName = dto.username;
        task.handlerAvatar = dto.avatar;
        task.handlerEmail = dto.email;
        task.updateTime = new Date();
        updateById(task);
        instance.handlerId = dto.userId;
        instance.handlerName = dto.username;
        processInstanceMapper.updateById(instance);
        Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("用户{}[{}]受理了任务{}[{}]", dto.username, dto.userId, task.name, task.id);
        return Result.success(task);
    }

    // 取消受理，挺麻烦的，暂时不给取消受理
  /*  public void disclaim() {

    }*/

    /**
     * 指派，应该发个通知给被指派的人
     */
    public Result assign(ProcessClaimAssignDTO dto) {
        ProcessTask task = processTaskMapper.selectById(dto.taskId);
        if (task.status != CLAIMED) {
            return Result.error("只有认领了任务才能指派");
        }
        UserDetails currentUserDetails = AuthenticationContext.current().getDetails();
        Logger processLogger = ProcessLogger.logger(task.instanceId);
        processLogger.info("{}将任务[{}]指派给了{}", currentUserDetails.username, task.name, dto.username);
        task.assignId = dto.userId;
        task.assignAvatar = dto.avatar;
        task.assignName = dto.username;
        task.assignEmail = dto.email;
        task.status = ASSIGNED;  // 标记为已指派
        task.remark = dto.remark;
        task.assignTime = new Date();
        updateById(task);
        // 指派成功，通知被指派的人
        processMailService.sendAssignMail(currentUserDetails.username, dto, task);
        sysMessageService.sendAssignMsg(dto, task, currentUserDetails);
        return Result.success(task);
    }

    /**
     * 同意
     */
    public void approve(ProcessTask task, ProcessTaskDTO dto) {
        if (task.status != CLAIMED && task.status != ASSIGNED) {
            throw new ApiException(500, "操作无效，此任务可能已被其他人处理，请刷新页面");
        }
        task.attachments = dto.attachments;
        task.status = ProcessTaskStatus.SUCCESS;
        if (dto.variables != null && !dto.variables.isEmpty()) {
            ProcessVariables processVariables = new ProcessVariables();
            processVariables.instanceId = task.instanceId;
            processVariables.data = JsonUtil.toJson(dto.variables);
            processVariablesService.save(processVariables);
            task.variableId = processVariables.id;
        }
        updateById(task);
    }

    public ProcessTask createStartTask(ProcessInstance instance, StartNode startNode) {
        ProcessTask task = new ProcessTask();
        task.definitionId = instance.definitionId;
        task.instanceId = instance.id;
        task.dagNodeId = startNode.id;
        task.userNode = false; // 开始节点一定不是用户环节，而是系统环节
        task.handlerId = instance.starter; // 开始节点肯定是发起人提交的
        task.name = startNode.label;
        task.status = ProcessTaskStatus.SUCCESS;
        task.createTime = new Date();
        processTaskMapper.insert(task);
        return task;
    }

    /**
     * 创建一个任务
     *
     * @param instance 流程实例
     * @param dagEdge  dag选中的路线
     * @param status   状态
     */
    @Transactional
    public ProcessTask createTask(ProcessInstance instance, DagEdge dagEdge, ProcessTaskStatus status) {
        DagNode node = dagEdge.targetNode;
        ProcessTask task = new ProcessTask();
        task.definitionId = instance.definitionId;
        task.instanceId = instance.id;
        task.preHandlerId = instance.preHandlerId;
        if (node instanceof UserTaskNode) {
            task.formId = ((UserTaskNode) node).formId;
            task.userNode = true;
        } else {
            task.userNode = false;
        }
        task.dagNodeId = node.id;
        task.incomeId = dagEdge.id;
        task.name = node.label;
        task.status = status;
        task.createTime = new Date();
        processTaskMapper.insert(task);
        return task;
    }

    /**
     * 授权任务,谁可以审批
     *
     * @param instance
     * @param task
     * @param userTaskNode
     * @param variables
     * @return 返回可以审批的人的邮箱清单
     */
    @Transactional
    public Set<String> authorize(ProcessInstance instance, ProcessTask task, UserTaskNode userTaskNode, Map<String, Object> variables) {
        // 准备日志路由
        Logger processLogger = ProcessLogger.logger(instance.id);
        // 可审批用户
        List<String> candidateUsers = userTaskNode.candidateUsers(variables);
        // 可审批角色
        List<String> candidateRoles = userTaskNode.candidateRoles(variables);

        // 处理申请人可审批的场景，并替换成实际的值
        int starterIndex = candidateUsers.indexOf("{starter}");
        if (starterIndex != -1) {
            candidateUsers.set(starterIndex, instance.starter);
            processLogger.info("任务:{}[{}]，申请人可审批:[{}]", task.name, task.id, instance.starter);
        }

        // 创建邮件抄送集合，同时用于去除重复的审批人员 (reduplicate candidateUser)
        Set<String> emailSet = new HashSet<>();

        // 授予审批权限给角色
        authorizeRoles(task, candidateRoles, emailSet, processLogger);
        // 授予审批权限给用户
        authorizeUsers(task, candidateUsers, emailSet, processLogger);
        return emailSet;
    }

    /**
     * 授权可以审批的角色
     *
     * @param task
     * @param candidateRoles
     * @param emailSet
     */
    private void authorizeRoles(ProcessTask task, List<String> candidateRoles, Set<String> emailSet, Logger processLogger) {
        if (!candidateRoles.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(candidateRoles);
            processLogger.info("任务:{}[{}]，可审批角色:{}", task.name, task.id, candidateRoles);
            roles.forEach(role -> {
                ProcessTaskAuth auth = new ProcessTaskAuth(role, task.id);
                processTaskAuthMapper.insert(auth);
            });
        }
        // 查找已被授权的用户的邮箱
        Set<String> mails = processTaskAuthMapper.selectRoleUserMailByTaskId(task.id);
        mails.forEach(mail -> {
            if (mail != null && MAIL_PATTERN.matcher(mail).find()) {
                // 准备好邮箱地址，用于发送电邮
                emailSet.add(mail);
            } else {
                processLogger.warn("存在非法邮箱：{}", mail);
            }
        });
    }

    /**
     * 授权可审批的用户
     *
     * @param task
     * @param candidateUsers
     * @param emailSet
     */
    private void authorizeUsers(ProcessTask task, List<String> candidateUsers, Set<String> emailSet, Logger processLogger) {

        List<SysUser> users = new ArrayList<>();

        // 处理“上一处理人主管”审批的场景，并找出成实际主管
        int masterIndex = candidateUsers.indexOf("{master}");
        if (masterIndex != -1) {
            candidateUsers.remove(masterIndex);
            SysUser sysUser = sysUserMapper.selectById(task.preHandlerId);
            List<SysUser> masters = sysUserMapper.selectMasters(sysUser.deptId);
            processLogger.info("任务:{}[{}]，上一处理人的主管可审批:{}", task.name, task.id,
                    masters.stream().map(SysUser::getId).collect(Collectors.toSet()));
            users.addAll(masters);
        }

        if (!candidateUsers.isEmpty()) {
            List<SysUser> sysUsers = sysUserMapper.selectBatchIds(candidateUsers);
            if (sysUsers != null) {
                users.addAll(sysUsers);
            }
        }
        users.forEach(user -> {
            if (user.email != null && MAIL_PATTERN.matcher(user.email).find() && !emailSet.contains(user.email)) {
                emailSet.add(user.email);
                ProcessTaskAuth auth = new ProcessTaskAuth(user, task.id);
                processTaskAuthMapper.insert(auth);
            } else {
                processLogger.warn("用户{}[{}]的邮箱地址{}不合法，不允许加入可审批人员清单", user.username, user.id, user.email);
            }
        });
    }


    /**
     * 需要加上权限控制，除了管理员以外，其他人只能查看自己能处理的任务
     *
     * @param queryParams
     * @return
     */
    @Override
    public PageResult<ProcessTask> pageQuery(Map<String, String> queryParams) {
        return super.pageQuery(queryParams);
    }


    /**
     * 查找当前审批节点之后的审批环节
     *
     * @param instanceId
     * @param taskIdGe
     * @return
     */
    public List<ProcessTask> auditListAfter(String instanceId, String taskIdGe) {
        QueryWrapper<ProcessTask> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("instance_id", instanceId);
        if (StringUtils.hasText(taskIdGe)) {
            // 查找id大于指定taskId的任务清单
            queryWrapper.ge("id", taskIdGe);
        }
        queryWrapper.orderByAsc("id");
        return processTaskMapper.selectList(queryWrapper);
    }

    /**
     * 查询待办清单(只能查询自己的: 自己可以认领的，被指派给自己的)
     *
     * @param processTaskTodoQuery
     * @return
     */
    public List<TaskTodoVO> queryTodoList(ProcessTaskTodoQuery processTaskTodoQuery) {
        // 查询当前用户的ID和角色
        Authentication authentication = AuthenticationContext.current();
        Set<String> roleIds = sysRoleMapper.selectBySysUserId(authentication.getId())
                .stream().map(SysRole::getId).collect(Collectors.toSet());
        processTaskTodoQuery.referenceIds = new HashSet<>(roleIds);
        processTaskTodoQuery.referenceIds.add(authentication.getId());
        // 查询待办
        List<TaskTodoVO> list = processTaskMapper.selectTodoList(processTaskTodoQuery);
        processTaskTodoQuery.userId = authentication.getId();
        // 指派给自己的任务也是代办
        List<TaskTodoVO> assignList = processTaskMapper.selectAssign(processTaskTodoQuery);
        list.addAll(assignList);
        // 按时间倒序排序
        list.sort(Comparator.comparing(TaskTodoVO::getCreateTime).reversed());
        return list;
    }

    public Result evaluatePermission(String id) {
        TaskAuthVO vo = new TaskAuthVO();
        // 判断当前用户是否可以审批 先找出正在执行的任务
        Authentication authentication = AuthenticationContext.current();
        String userId = authentication.getId();
        ProcessTask task = processTaskMapper.selectById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        if (task.status != PENDING && task.status != CLAIMED && task.status != ASSIGNED) {
            vo.canAudit = false;
            return Result.success(vo);
        }

        if (userId.equals(task.handlerId) || userId.equals(task.assignId)) {
            vo.canAudit = true;
        }

        if (task.status == PENDING) {
            // 填写任务的可审批人员和可审批角色清单
            List<ProcessTaskAuth> list = processTaskAuthMapper.selectAuthByTaskId(task.id);
            Set<String> referenceIds = new HashSet<>();
            list.forEach(item -> {
                if (item.type == 0) {
                    vo.users.add(item);
                } else {
                    vo.roles.add(item);
                }
                referenceIds.add(item.referenceId);
            });
            if (referenceIds.contains(userId)) {
                vo.canAudit = true;
            }
            Set<String> roleIds = sysRoleMapper.selectBySysUserId(authentication.getId())
                    .stream().map(SysRole::getId).collect(Collectors.toSet());
            if (referenceIds.stream().anyMatch(roleIds::contains)) {
                vo.canAudit = true;
            }
        }

        return Result.success(vo);
    }

    /**
     * 取消任务
     *
     * @param instanceId
     */
    public void cancelByInstanceId(String instanceId) {
        processTaskMapper.cancelByInstanceId(instanceId);
    }
}
