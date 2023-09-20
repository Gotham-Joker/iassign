package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.GlobalIdGenerator;
import com.github.core.Result;
import com.github.iassign.Constants;
import com.github.iassign.ProcessLogger;
import com.github.iassign.core.dag.DagEdge;
import com.github.iassign.core.dag.node.DagNode;
import com.github.iassign.core.dag.node.ExecutableNode;
import com.github.iassign.core.dag.node.UserTaskNode;
import com.github.iassign.dto.ProcessInstanceSnapshot;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.enums.ProcessInstanceStatus;
import com.github.iassign.enums.ProcessTaskStatus;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.iassign.mapper.ProcessDefinitionRuMapper;
import com.github.iassign.mapper.ProcessInstanceMapper;
import com.github.iassign.dto.ProcessInstanceDetailDTO;
import com.github.base.BaseService;
import com.github.core.PageResult;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProcessInstanceService {
    @Autowired
    private ProcessInstanceMapper processInstanceMapper;
    @Autowired
    private FormInstanceMapper formInstanceMapper;
    @Autowired
    private ProcessDefinitionRuMapper processDefinitionRuMapper;
    @Autowired
    private GlobalIdGenerator globalIdGenerator;
    @Autowired
    private ProcessTaskService processTaskService;
    @Autowired
    private ProcessMailService processMailService;
    @Autowired
    private SysMessageService sysMessageService;
    @Autowired
    private ProcessInstanceIndexService processInstanceIndexService;

    public PageResult pageQuery(Map<String, String> params) {
        BaseService.pageHelper(params);
        QueryWrapper<ProcessInstance> wrapper = BaseService.wrapper(params);
        List<ProcessInstance> list = processInstanceMapper.selectList(wrapper);
        return PageResult.of(list);
    }


    /**
     * 查找流程实例详情
     *
     * @param id 流程实例id
     * @return
     */
    public Result<ProcessInstanceDetailDTO> findDetail(String id) {
        ProcessInstanceDetailDTO vo = new ProcessInstanceDetailDTO();
        ProcessInstance instance = processInstanceMapper.selectById(id);
        if (instance == null) {
            return Result.error(404, "数据不存在，此数据可能已被删除");
        }
        BeanUtils.copyProperties(instance, vo);
        vo.dag = processDefinitionRuMapper.selectById(instance.ruId).dag;
        vo.formData = formInstanceMapper.selectById(instance.formInstanceId).data;
        return Result.success(vo);
    }

    public ProcessInstance create(ProcessStartDTO dto, String definitionName, ProcessDefinitionRu definitionRu, UserDetails userDetails) {
        ProcessInstance instance = new ProcessInstance();
        // 插入数据库之前就应该生成这个id，后面要用
        instance.id = globalIdGenerator.nextIdStr();
        instance.definitionId = dto.definitionId;
        instance.name = definitionName;
        instance.ruId = definitionRu.id;
        instance.emails = dto.emails;
        instance.starter = dto.starter;
        instance.starterName = userDetails.username;
        instance.deptId = userDetails.deptId;
        instance.handlerId = instance.starter;
        instance.handlerName = userDetails.username;
        instance.createTime = new Date();
        instance.status = ProcessInstanceStatus.RUNNING;
        return instance;
    }

    @Transactional
    public void save(ProcessInstance instance) {
        processInstanceMapper.insert(instance);
    }

    public ProcessInstance selectById(String instanceId) {
        return processInstanceMapper.selectById(instanceId);
    }

    public void updateById(ProcessInstance instance) {
        processInstanceMapper.updateById(instance);
    }

    @Transactional
    public void handleUserTaskNode(DagNode dagNode, DagEdge dagEdge, ProcessInstance instance, Map<String, Object> variables) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.PENDING);
        try {
            // 记录上一处理人,当前处理人应该是要等"受理"的时候才赋值,被指派人应该是"被指派"的时候才赋值
            instance.preHandlerId = instance.handlerId;
            instance.handlerId = ""; // 设为null的话mybatis-plus不会更新null的字段
            instance.handlerName = "";
            // 因为用户审批需要人操作，所以为下一个节点和用户创建好一个待办任务后就暂停
            processLogger.info("创建待审批任务:<{}>[{}]", task.name, task.id);
            // 每次都放入最新的实例快照到上下文中
            variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
            // 授权，谁可以审批
            Set<String> emailSet = processTaskService.authorize(instance, task, (UserTaskNode) dagNode, variables);
            processLogger.info("发送待审批邮件和站内信：{}", emailSet);
            // 通知负责审批的人有新的待办任务,等他们完成任务后才继续下一步
            processMailService.sendTodoMail(instance, emailSet);
            sysMessageService.sendAsyncTodoMsg(instance, task, emailSet);
        } catch (Exception e) {
            processLogger.error("创建任务" + dagNode.label + "失败", e);
            task.status = ProcessTaskStatus.FAILED;
            task.updateTime = new Date();
            instance.status = ProcessInstanceStatus.FAILED;
            processTaskService.updateById(task);
            processInstanceMapper.updateById(instance);
        }
    }

    @Transactional
    public void handleEndNode(DagEdge dagEdge, ProcessInstance instance) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        // 添加结束任务
        ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
        try {
            // 成功
            instance.status = ProcessInstanceStatus.SUCCESS;
            instance.dagNodeId = null;
            instance.updateTime = new Date();
            processLogger.info("流程[{}]结束", instance.id);
            // 通知发起人审批完结
            processMailService.sendEndMail(instance);
            sysMessageService.sendSuccessMsg(instance, AuthenticationContext.details());
            processInstanceIndexService.updateStatus(instance);
        } catch (Exception e) {
            processLogger.error("尝试完结任务时出现错误", e);
            task.status = ProcessTaskStatus.FAILED;
            task.updateTime = new Date();
            instance.status = ProcessInstanceStatus.FAILED;
            processTaskService.updateById(task);
            processInstanceMapper.updateById(instance);
        }
    }

    /**
     * 记录其他节点，例如网关节点
     *
     * @param dagNode
     * @param dagEdge
     * @param instance
     */
    @Transactional
    public void handleOtherNode(DagNode dagNode, DagEdge dagEdge, ProcessInstance instance) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("进入{}环节", dagNode.label);
        // 创建一个任务并尝试自动完成它
        instance.preHandlerId = instance.handlerId;
        processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
    }

    @Transactional
    public ProcessInstance handleExecutableNode(ExecutableNode executableNode, DagEdge dagEdge, ProcessInstance instance, Map<String, Object> variables) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        // 遇到系统节点，先创建一个“系统任务” 提交异步任务
        instance.preHandlerId = instance.handlerId;
        ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.RUNNING);
        processInstanceMapper.updateById(instance);
        processLogger.info("异步执行节点:<{}>[{}]", task.name, task.id);
        variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
        try {
            executableNode.execute(processLogger, variables);
        } catch (Exception e) {
            processLogger.error("流程运行异常", e);
            task.status = ProcessTaskStatus.FAILED;
            task.updateTime = new Date();
            instance.status = ProcessInstanceStatus.FAILED;
            processTaskService.updateById(task);
            processInstanceMapper.updateById(instance);
            return null;
        }
        // 流程是否已被撤回？如果被撤回那就没下文了，否则继续
        ProcessInstance recentInstance = processInstanceMapper.selectById(instance.id);
        if (recentInstance != null && recentInstance.status == ProcessInstanceStatus.CANCEL) {
            processLogger.warn("流程实例已被用户撤回，终止后续任务");
            processTaskService.cancelByInstanceId(recentInstance.id);
            return null;
        }
        task.status = ProcessTaskStatus.SUCCESS;
        processTaskService.updateById(task);
        return instance;
    }
}
