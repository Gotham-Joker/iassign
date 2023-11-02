package com.github.iassign.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.authorization.UserDetails;
import com.github.core.GlobalIdGenerator;
import com.github.core.JsonUtil;
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
import com.github.iassign.entity.ProcessVariables;
import com.github.iassign.enums.ProcessInstanceStatus;
import com.github.iassign.enums.ProcessTaskStatus;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.iassign.mapper.ProcessDefinitionMapper;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class ProcessInstanceService {
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
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
    @Autowired
    private ProcessVariablesService processVariablesService;

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
        instance.emails = dto.emails == null ? "" : dto.emails;
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
        // 记录上一处理人,当前处理人应该是要等"受理"的时候才赋值,被指派人应该是"被指派"的时候才赋值
        instance.preHandlerId = instance.handlerId;
        instance.handlerId = ""; // 设为null的话mybatis-plus不会更新null的字段
        instance.handlerName = "";
        UserTaskNode userTaskNode = (UserTaskNode) dagNode;
        ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.PENDING);
        // 标记为人员处理
        task.userNode = true;
        // 放入表单ID
        task.formId = userTaskNode.formId;
        // 处理会签标志
        task.countersign = Boolean.TRUE.equals(userTaskNode.countersign);
        // 会签节点自动标记为已认领
        if (task.countersign) {
            task.status = ProcessTaskStatus.CLAIMED;
        }
        processTaskService.updateById(task);
        processLogger.info("创建待审批任务:<{}>[{}]，是否是'会签':{}", task.name, task.id, task.countersign);
        variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
        // 授权，谁可以审批
        Set<String> emailSet = processTaskService.authorize(instance, task, userTaskNode, variables);
        processLogger.info("发送待审批邮件和站内信：{}", emailSet);
        // 通知负责审批的人有新的待办任务,等他们完成任务后才继续下一步
        processMailService.sendTodoMail(instance, emailSet);
        sysMessageService.sendAsyncTodoMsg(instance, task, emailSet);
    }

    @Transactional
    public void handleEndNode(DagEdge dagEdge, ProcessInstance instance) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
        // 成功
        instance.status = ProcessInstanceStatus.SUCCESS;
        instance.dagNodeId = null;
        instance.updateTime = new Date();
        // 添加结束的任务
        processLogger.info("流程[{}]结束", instance.id);
        // 通知发起人审批完结
        processMailService.sendEndMail(instance);
        sysMessageService.sendSuccessMsg(instance);
        processInstanceIndexService.updateStatus(instance);

    }

    @Transactional
    public void handleOtherNode(DagNode dagNode, DagEdge dagEdge, ProcessInstance instance) {
        Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("进入{}环节", dagNode.label);
        // 创建一个任务并尝试自动完成它
        instance.preHandlerId = instance.handlerId;
        processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
    }

    @Transactional
    public ProcessInstance handleExecutableNode(ExecutableNode executableNode, ProcessTask task, ProcessInstance instance, Map<String, Object> variables) {
        // 先尝试更新流程实例
        processInstanceMapper.updateById(instance);
        Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("异步执行节点:<{}>[{}]", task.name, task.id);
        variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
        try {
            // 先移除和"Constants.PROCESS_CONTEXT"命名冲突的变量
            variables.remove(Constants.PROCESS_CONTEXT);
            executableNode.execute(processLogger, variables);
            // 如果依然存在"Constants.PROCESS_CONTEXT"的变量，那么说明这些参数要作为全局变量
            Map<String, Object> variablesIN = (Map<String, Object>) variables.get(Constants.PROCESS_CONTEXT);
            if (!CollectionUtils.isEmpty(variablesIN)) {
                processLogger.info("<{}>[{}]全局变量输入:{}", task.name, task.id, variablesIN);
                // 更新全局流程变量
                // 删除那些有冲突的变量名
                variablesIN.remove(Constants.INSTANCE);
                variablesIN.remove(Constants.PROCESS_CONTEXT);
                processVariablesService.mergeVariables(instance.variableId, variablesIN);
            }
        } catch (Exception e) {
            processLogger.error("流程运行异常", e);
            task.status = ProcessTaskStatus.FAILED;
            task.updateTime = new Date();
            instance.status = ProcessInstanceStatus.FAILED;
            if (!StringUtils.hasText(task.variableId)) {
                // 将临时和全局上下文变量全部保存起来，以便恢复失败的作业时使用
                ProcessVariables processVariables = new ProcessVariables();
                processVariables.data = JsonUtil.toJson(variables);
                processVariablesService.save(processVariables);
                task.variableId = processVariables.id;
            }
            processTaskService.updateById(task);
            processInstanceMapper.updateById(instance);
            return null;
        }
        // 流程是否已被撤回？
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
