package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.iassign.entity.*;
import com.github.pagehelper.PageHelper;
import com.github.authorization.UserDetails;
import com.github.iassign.ProcessLogger;
import com.github.iassign.dto.ProcessClaimAssignDTO;
import com.github.iassign.dto.Tuple;
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
import com.github.iassign.core.dag.DagEdge;
import com.github.iassign.core.dag.node.DagNode;
import com.github.iassign.core.dag.node.StartNode;
import com.github.iassign.core.dag.node.UserTaskNode;
import com.github.core.PageResult;
import com.github.core.Result;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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
    private ProcessTaskAuthService processTaskAuthService;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private ProcessOpinionService processOpinionService;
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
     * @param backwardTaskId
     * @return 退回到指定的任务
     */
    public ProcessTask back(ProcessTask task, String backwardTaskId) {
        // 执行校验
        if (!StringUtils.hasText(backwardTaskId)) {
            throw new ApiException(422, "请指定回退环节");
        }
        // 从指定的任务复制一个新任务出来
        ProcessTask backwardTask = processTaskMapper.selectById(backwardTaskId);
        if (!Boolean.TRUE.equals(backwardTask.userNode)) {
            throw new ApiException(500, "无法回退至非用户节点");
        }
        if (backwardTask.status == PENDING || backwardTask.status == CLAIMED || backwardTask.status == ASSIGNED) {
            throw new ApiException(500, "无法回退至正在审批的环节");
        }
        // 校验结束

        // 当前任务标记为退回
        task.status = ProcessTaskStatus.BACK;
        task.updateTime = new Date();
        updateById(task);

        ProcessTask copyTask = new ProcessTask();
        copyTask.definitionId = backwardTask.definitionId;
        copyTask.instanceId = backwardTask.instanceId;
        copyTask.formId = backwardTask.formId;
        copyTask.dagNodeId = backwardTask.dagNodeId;
        copyTask.incomeId = backwardTask.incomeId;
        copyTask.preHandlerId = backwardTask.preHandlerId;
        copyTask.name = backwardTask.name;
        copyTask.status = PENDING; // 新的回退任务是待受理状态
        copyTask.userNode = backwardTask.userNode;
        copyTask.fileRequired = backwardTask.fileRequired;
        copyTask.assign = backwardTask.assign;
        copyTask.createTime = new Date();
        copyTask.countersign = backwardTask.countersign;
        // 如果是会签，则自动受理
        if (copyTask.countersign) {
            copyTask.status = CLAIMED;
        }
        processTaskMapper.insert(copyTask);
        return copyTask;
    }

    /**
     * 直接拒绝这个任务，对应的流程实例应当取消
     */
    public void reject(ProcessTask task) {
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
        task.updateTime = new Date();
        updateById(task);
        instance.handlerId = dto.userId;
        instance.handlerName = dto.username;
        processInstanceMapper.updateById(instance);
        Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("用户{}[{}]确认受理<{}>[{}]", dto.username, dto.userId, task.name, task.id);
        return Result.success(task);
    }


    /**
     * 指派，应该发个通知给被指派的人
     */
    @Transactional
    public Result assign(ProcessClaimAssignDTO dto) {
        ProcessTask task = processTaskMapper.selectById(dto.taskId);
        if (task.status != CLAIMED) {
            return Result.error("只有认领了任务才能指派");
        }
        Logger processLogger = ProcessLogger.logger(task.instanceId);
        // 当前登录用户信息
        UserDetails userDetails = AuthenticationContext.current().getDetails();
        // 判断是否可以指派，指派有几个原则：
        // 1.拥有审批权限
        Set<String> taskAuthIds = processTaskAuthService.selectAuthorize(task.id, userDetails.id);
        if (CollectionUtils.isEmpty(taskAuthIds)) {
            // 没有审批权限
            processLogger.error("<{}>[{}]不具备审批环节<{}>[{}]的审批权限，不能执行指派操作", userDetails.id, userDetails.username, task.name, task.id);
            return Result.error("您不具备审批权限，不能执行指派操作");
        }
        // 2. 被指派人不能在已授权清单中(忽略被指派人的角色)
        String referenceId = processTaskAuthService.selectAuthorizedUser(task.id, dto.userId);
        if (StringUtils.hasText(referenceId)) {
            // 不允许审批
            processLogger.error("审批人<{}>[{}]指派失败，被指派人<{}>[{}]已具备审批权限。审批环节：<{}>[{}]",
                    userDetails.id, userDetails.username,
                    dto.userId, dto.username, task.name, task.id);
            return Result.error("指派失败，被指派人" + dto.username + "已具备审批权限");
        }
        // 指派，就是给当前任务加个授权人，允许其审批
        processLogger.info("{}将任务[{}]指派给了{}", userDetails.username, task.name, dto.username);
        // 被指派人加入授权清单
        processTaskAuthService.addAuthorize(dto, task);
        processOpinionService.submitAssignOpinion(userDetails, task, dto);
        updateById(task);
        // 指派成功，通知被指派的人
        processMailService.sendAssignMail(userDetails.username, dto, task);
        sysMessageService.sendAssignMsg(dto, task, userDetails);
        return Result.success(auditListAfter(task.instanceId, task.id));
    }

    public ProcessTask createStartTask(ProcessInstance instance, StartNode startNode) {
        ProcessTask task = new ProcessTask();
        task.definitionId = instance.definitionId;
        task.instanceId = instance.id;
        task.dagNodeId = startNode.id;
        task.userNode = false; // 开始节点一定不是用户环节，而是系统环节
        task.assign = false;
        task.countersign = false;
        task.fileRequired = false;
        task.handlerId = instance.starter; // 开始节点肯定是发起人提交的
        task.name = startNode.label;
        task.status = ProcessTaskStatus.SUCCESS;
        task.createTime = new Date();
        processTaskMapper.insert(task);
        return task;
    }

    /**
     * 创建回退至发起人的审批任务
     *
     * @return
     */
    public ProcessTask createReturnStartTask(ProcessInstance instance, StartNode startNode) {
        ProcessTask task = new ProcessTask();
        task.definitionId = instance.definitionId;
        task.instanceId = instance.id;
        task.dagNodeId = startNode.id;
        task.userNode = true; // 允许审批
        task.handlerId = instance.starter; // 开始节点肯定是发起人提交的
        task.name = startNode.label;
        task.status = CLAIMED; // 已受理，等待审批
        task.createTime = new Date();
        task.countersign = false;
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
        task.dagNodeId = node.id;
        task.incomeId = dagEdge.id;
        task.name = node.label;
        task.status = status;
        task.createTime = new Date();
        task.countersign = false;
        task.assign = false;
        task.userNode = false;
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
        // 处理“上一处理人主管”审批这些特殊的场景，并找出成实际的角色
        // 分别代表主管、副主管、分管、副分管
        String[] expressOptions = new String[]{"{master}", "{vMaster}", "{leader}", "{vLeader}"};
        String[] roleSuffixes = new String[]{"MA01", "VMA01", "MA02", "VMA02"};
        for (int i = 0; i < expressOptions.length; i++) {
            String expressOption = expressOptions[i];
            int idx = candidateRoles.indexOf(expressOption);
            if (idx != -1) {
                candidateRoles.remove(idx);
                SysUser sysUser = sysUserMapper.selectById(task.preHandlerId);
                String roleId = sysUser.deptId + roleSuffixes[i];
                SysRole sysRole = sysRoleMapper.selectById(roleId);
                if (sysRole == null) {
                    processLogger.error("角色查找失败，原因：角色缺失，缺失的角色ID:{}", roleId);
                    throw new ApiException(404, "角色查找失败，原因：角色缺失");
                }
                processLogger.info("审批节点:<{}>[{}]，可审批角色:<{}>[{}]", task.name, task.id, sysRole.name, sysRole.id);
                ProcessTaskAuth auth = new ProcessTaskAuth(sysRole, task.id);
                processTaskAuthService.save(auth);
            }
        }

        if (!candidateRoles.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(candidateRoles);
            processLogger.info("审批节点:<{}>[{}]，可审批角色:{}", task.name, task.id, candidateRoles);
            roles.forEach(role -> {
                ProcessTaskAuth auth = new ProcessTaskAuth(role, task.id);
                processTaskAuthService.save(auth);
            });
        }
        // 查找已被授权的用户的邮箱
        Set<Tuple<String, String>> tuples = processTaskAuthService.selectRoleUserMailByTaskId(task.id);
        tuples.forEach(tuple -> {
            String mail = tuple.v;
            if (mail != null && MAIL_PATTERN.matcher(mail).find()) {
                // 准备好邮箱地址，用于发送电邮
                emailSet.add(mail);
            } else {
                processLogger.warn("存在非法邮箱：userId:{},email:{}", tuple.k, mail);
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
                processTaskAuthService.save(auth);
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
     * 查找当前审批节点以及之后的审批节点
     *
     * @param instanceId
     * @param taskIdGe   可以为null，null表示将流程实例的任务全都筛选出来
     * @return
     */
    public List<ProcessTask> auditListAfter(String instanceId, String taskIdGe) {
        // 查找id大于指定taskId的任务清单
        List<ProcessTask> processTasks = processTaskMapper.selectList(
                new QueryWrapper<ProcessTask>()
                        .eq("instance_id", instanceId)
                        .ge(StringUtils.hasText(taskIdGe), "id", taskIdGe)
                        .orderByAsc("id")
        );

        // 将审批意见按照任务ID进行分组，然后填充审批意见
        Map<String, List<ProcessOpinion>> opinions = processOpinionService.selectOpinions(instanceId, taskIdGe);

        processTasks.forEach(task -> task.opinions = opinions.get(task.id));
        return processTasks;
    }

    /**
     * 查询待办清单(只能查询自己的: 自己可以认领的，被指派给自己的)
     *
     * @param processTaskTodoQuery
     * @return
     */
    public PageResult<TaskTodoVO> queryTodoList(Integer page, Integer size, ProcessTaskTodoQuery processTaskTodoQuery) {
        // 查询当前用户的ID和角色
        Authentication authentication = AuthenticationContext.current();
        processTaskTodoQuery.referenceIds = sysRoleMapper.selectBySysUserId(authentication.getId())
                .stream().map(SysRole::getId).collect(Collectors.toSet());
        processTaskTodoQuery.referenceIds.add(authentication.getId());
        // 查询待办
        PageHelper.startPage(page, size);
        return PageResult.of(processTaskMapper.selectTodoList(processTaskTodoQuery));
    }

    /**
     * 校验审批权限，当前用户如果不能审批，那么返回的VO对象的canAudit为false
     *
     * @param userId
     * @param task
     * @return
     */
    public TaskAuthVO validateAuthorize(String userId, ProcessTask task) {
        TaskAuthVO vo = new TaskAuthVO();

        if (task.status != PENDING && task.status != CLAIMED && task.status != ASSIGNED) {
            vo.canAudit = false;
            return vo;
        }

        // 判断是否可以审批：
        // 1. 不在审批授权清单中不能审批
        Set<String> authorizeSet = processTaskAuthService.selectAuthorize(task.id, userId);
        if (CollectionUtils.isEmpty(authorizeSet)) {
            vo.canAudit = false;
            completeCandidates(task, vo);
            return vo;
        }

        // 2. 同一个用户不能重复审批
        ProcessOpinion processOpinion = processOpinionService.selectByTaskIdAndUserId(task.id, userId);
        if (processOpinion != null) {
            vo.canAudit = false;
            completeCandidates(task, vo);
            return vo;
        }

        vo.canAudit = true;

        completeCandidates(task, vo);
        return vo;
    }

    /**
     * 将可审批人信息，补充完整
     * 如果不是会签环节，已受理的时候没必要知道谁可以审批。如果是会签，那所有人都要知道，还有谁没审批
     * 未受理状态，也显示谁能受理
     *
     * @param task
     * @param vo
     */
    private void completeCandidates(ProcessTask task, TaskAuthVO vo) {
        if (task.status == PENDING || task.status == CLAIMED) {
            // 构造可审批人员和角色清单，让用户能知道现在该由谁审批
            // 填写任务的可审批人员和可审批角色清单
            List<ProcessTaskAuth> list = processTaskAuthService.selectAuthByTaskId(task.id);
            list.forEach(item -> {
                if (item.type == 0) {
                    vo.users.add(item);
                } else {
                    vo.roles.add(item);
                }
            });
        }
    }

    /**
     * 取消任务
     *
     * @param instanceId
     */
    public void cancelByInstanceId(String instanceId) {
        processTaskMapper.cancelByInstanceId(instanceId);
    }


    /**
     * 判断任务是否可以完成
     *
     * @param task
     * @return
     */
    public boolean canFinish(ProcessTask task) {
        if (task.countersign) {
            return countersignFinish(task.id);
        }
        return true;
    }

    /**
     * 判断会签是否已经完成
     * 会签结束的条件：所有人都同意，如果包含角色，那么起码有一个角色必须审批参与审批通过，
     * 只要有一个人不同意流程失败，只要有一个人回退，那么流程回退
     *
     * @return
     */
    public boolean countersignFinish(String taskId) {
        // 取出会签环节中，必须参与会签的人和角色
        Set<String> participantUserIds = new HashSet<>();
        Set<String> participantRoleIds = new HashSet<>();
        processTaskAuthService.selectAuthByTaskId(taskId).stream()
                .forEach(processTaskAuth -> {
                    // 参与者是人
                    if (processTaskAuth.type == 0) {
                        participantUserIds.add(processTaskAuth.referenceId);
                    } else {
                        // 参与者是角色
                        participantRoleIds.add(processTaskAuth.referenceId);
                    }
                });
        // 取出会签环节，已参与会签的人员
        Set<String> userIds = processOpinionService.selectByTaskId(taskId).stream()
                .map(ProcessOpinion::getUserId).collect(Collectors.toSet());
        for (String userId : participantUserIds) {
            // 说明还有人员未参与审批
            if (!userIds.contains(userId)) {
                return false;
            }
        }
        if (!participantRoleIds.isEmpty()) {
            // 取出已参与会签的角色
            Set<String> roleIds = processOpinionService.selectParticipantRoles(taskId, participantRoleIds);
            // 说明还有角色未审批
            if (roleIds.size() < participantRoleIds.size()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 取回任务
     *
     * @param userDetails
     * @param taskId
     */
    @Transactional
    public Result reclaim(UserDetails userDetails, String taskId) {
        ProcessTask task = processTaskMapper.selectById(taskId);
        ProcessInstance instance = processInstanceMapper.selectById(task.instanceId);
        if (instance.status != ProcessInstanceStatus.RUNNING) {
            return Result.error(500, "流程不是运行状态，取回失败");
        }
        if (Boolean.TRUE.equals(task.countersign)) {
            return Result.error(500, "该审批节点[" + task.name + "]属于会签环节，无法取回");
        }
        Logger processLogger = ProcessLogger.logger(task.instanceId);
        ProcessTask nextUserTask = processTaskMapper.selectUserTaskAfter(instance.id, taskId);
        if (nextUserTask == null) {
            return Result.error(500, "下一个经办节点不存在，无法取回");
        }
        // 找出下一个用户审批节点，如果不存在下一审批节点、下一审批节点不是待受理状态或者下一审批节点是会签节点，不允许取回
        if (nextUserTask.status != PENDING || Boolean.TRUE.equals(nextUserTask.countersign)) {
            return Result.error(500, "下一个经办节点[" + nextUserTask.name + "]已被受理或者已审批，无法取回");
        }
        processLogger.info("用户{}正在尝试取回已办事项:[{}]{},系统尝试关闭下一待办事项:[{}]<{}>", userDetails.id, task.id, task.name,
                nextUserTask.id, nextUserTask.name);
        // 1. 给下一环节的处理人发送被取回的通知
        // 1.1 先查找可审批的角色，提取这些人的邮箱
        Set<Tuple<String, String>> tuples = processTaskAuthService.selectRoleUserMailByTaskId(nextUserTask.id);
        Set<String> emailSet = tuples.stream().filter(tuple -> tuple.v != null && MAIL_PATTERN.matcher(tuple.v).find())
                .map(tuple -> tuple.v).collect(Collectors.toSet());
        // 1.2 再查找可审批的用户，同样是提取邮箱
        tuples = processTaskAuthService.selectUserMailByTaskId(taskId);
        tuples.stream().filter(tuple -> tuple.v != null && MAIL_PATTERN.matcher(tuple.v).find())
                .forEach(tuple -> emailSet.add(tuple.v));

        // 2. 删除由当前节点产生的所有后续节点
        processTaskMapper.deleteAfter(instance.id, taskId);
        // 3. 删除下一审批人环节和相关授权
        processTaskAuthService.deleteByTaskId(nextUserTask.id);
        // 4. 删除当前审批人提交的审批意见
        processOpinionService.deleteByTaskIdAndUserId(taskId, userDetails.id);
        // 5. 流程实例也进行回退
        instance.dagNodeId = task.dagNodeId;
        instance.preHandlerId = task.preHandlerId;
        instance.handlerId = userDetails.id; // 当前审批人信息
        instance.handlerName = userDetails.username; // 当前审批人信息
        processInstanceMapper.updateById(instance);
        task.status = CLAIMED;
        task.formInstanceId = null;
        processTaskMapper.updateById(task);
        // 发送邮件通知
        processLogger.info("删除下一经办节点<{}>[{}]，系统执行取回，并邮件通知以下人员:{}", nextUserTask.name, nextUserTask.name, emailSet);
        processMailService.sendReclaimMail(instance, task, emailSet);
        return Result.success();
    }

    public void returnToStarter(ProcessTask task) {
        task.status = RETURN;
        task.updateTime = new Date();
        updateById(task);
    }
}
