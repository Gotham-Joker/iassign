package com.github.iassign.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.iassign.entity.SysUser;
import com.github.iassign.mapper.SysUserMapper;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.iassign.Constants;
import com.github.iassign.ProcessLogger;
import com.github.iassign.core.dag.node.*;
import com.github.iassign.dto.ProcessInstanceSnapshot;
import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.entity.*;
import com.github.iassign.enums.ProcessInstanceStatus;
import com.github.iassign.enums.ProcessTaskStatus;
import com.github.core.ApiException;
import com.github.core.JsonUtil;
import com.github.iassign.core.dag.DagEdge;
import com.github.iassign.core.dag.DagGraph;
import com.github.iassign.core.expression.DefaultExpressionEvaluator;
import com.github.iassign.core.expression.ExpressionEvaluator;
import com.github.iassign.dto.ProcessTaskDTO;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.io.IOException;
import java.util.*;


/**
 * 流程服务
 */
@Slf4j
@Service
public class ProcessService {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    private ProcessDefinitionRuService processDefinitionRuService;
    @Autowired
    private ProcessTaskService processTaskService;
    @Autowired
    private FormService formService;
    @Autowired
    private ProcessVariablesService processVariablesService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private ProcessInstanceIndexService processInstanceIndexService;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ProcessMailService processMailService;
    @Autowired
    private SysMessageService sysMessageService;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private ExpressionEvaluator expressionEvaluator;

    /**
     * 初始化计算器
     */
    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        // 这玩意会将DAG流程图里面写的脚本编译成字节码并缓存，可能会导致jvm中的class爆满，是否应该定期清空一下？
        expressionEvaluator = new DefaultExpressionEvaluator(applicationContext);
        // interval(8,hour).subscribe(()=>expressionEvaluator.clear())
    }

    /**
     * 启动流程实例
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance startInstance(ProcessStartDTO dto) throws Exception {
        UserDetails userDetails = AuthenticationContext.current().getDetails();
        // 流程发起人
        dto.starter = userDetails.getId();
        // 校验
        ProcessDefinition definition = processDefinitionMapper.selectById(dto.definitionId);
        if (definition == null) {
            throw new ApiException(500, "工作流不存在，可能已被删除，请刷新页面");
        }
//        SysUser sysUser = sysUserMapper.selectById(dto.starter);

        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(definition.ruId);
        if (definitionRu == null) {
            throw new ApiException(404, "流程图不存在或已被删除，流程启动失败");
        }
        definition.dag = definitionRu.dag;

        ProcessInstance instance = processInstanceService.create(dto, definition.name, definitionRu, userDetails);

        // 准备流转用的全局上下文变量
        Map<String, Object> contextVariables = new HashMap<>();

        // 如果有表单，就保存表单，并且解析里面的field和value
        if (dto.formData != null) {
            FormInstance formInstance = formService.saveInstance(definition.formId, dto.formData, 0);
            instance.formInstanceId = formInstance.id;
            Map<String, Object> formVariables = JsonUtil.readValue(formInstance.variables, Map.class);
            // 解析出来的表单变量化为流程变量
            contextVariables.putAll(formVariables);
        }
        // 如果存在其他全局变量，也放进上下文中，将其和表单变量合并起来，存到数据库
        if (dto.variables != null && !dto.variables.isEmpty()) {
            contextVariables.putAll(dto.variables);
        }
        // 保存流程变量到数据库
        if (!contextVariables.isEmpty()) {
            ProcessVariables processVariables = new ProcessVariables();
            processVariables.instanceId = instance.id;
            processVariables.data = JsonUtil.toJson(contextVariables);
            processVariablesService.save(processVariables);
            instance.variableId = processVariables.id; // 绑定为全局变量 (整个流程都会生效)
        }

        // 创建表达式计算器，用于处理各种动态条件
        ExpressionEvaluator expressionEvaluator = new DefaultExpressionEvaluator(applicationContext);
        // 初始化流程图，并获取开始节点
        DagGraph dagGraph = DagGraph.init(JsonUtil.readValue(definition.dag, ArrayNode.class), expressionEvaluator);
        StartNode startNode = dagGraph.startNode;

        // 设置当前节点，刚启动的时候肯定是开始节点
        instance.dagNodeId = startNode.id;
        processInstanceService.save(instance);

        // 在ES中插入索引
        processInstanceIndexService.save(instance, userDetails, contextVariables);

        // 日志路由
        final Logger processLogger = ProcessLogger.logger(instance.id);
        processLogger.info("用户:{}[{}]，启动了流程:{},defId:{},申请编号:{}", instance.starterName, instance.starter,
                definition.name, definition.id, instance.id);

        // 以任务的形式记录开始节点
        ProcessTask startTask = processTaskService.createStartTask(instance, startNode);
        processLogger.info("开始节点任务ID:{}", startTask.id);
        // 进入下一个环节
        move(dagGraph, instance, contextVariables);

        // 发邮件
        processMailService.sendStartMail(instance);
        return instance;
    }

    /**
     * 提交任务，进入下一个环节，返回新生成的任务
     *
     * @return 新生成的任务环节清单
     */
    @Transactional
    public List<ProcessTask> handleTask(ProcessTaskDTO dto) {
        ProcessTask task = processTaskService.selectById(dto.taskId);
        ProcessInstance instance = processInstanceService.selectById(task.instanceId);
        // 校验
        if (instance.status != ProcessInstanceStatus.RUNNING &&
                task.status != ProcessTaskStatus.ASSIGNED
                && task.status != ProcessTaskStatus.CLAIMED) {
            throw new ApiException(500, "操作无效，当前审批任务可能已被其他人处理，请刷新");
        }
        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(instance.ruId);
        UserDetails currentUser = AuthenticationContext.current().getDetails();
        String auditorId = currentUser.getId();
        Logger processLogger = ProcessLogger.logger(instance.id);
        String remark = dto.safeRemark();
        // 审批的时候，判断当前处理人是指派人还是受理人
        if (auditorId.equals(task.assignId)) {
            task.assignRemark = remark;
            task.assignTime = new Date();
        } else if (auditorId.equals(task.handlerId)) {
            task.remark = remark;
        } else { // 既不是受理人也不是指派人，抛出异常，不给审批
            processLogger.error("当前用户无法审批[{}]：任务已被受理，但是此用户既不是受理人也不是被指派人。", auditorId);
            throw new ApiException(500, "当前用户无法审批");
        }
        // 设置邮件接收人并批量发送
        instance.emails = dto.emails;

        DagGraph dagGraph = DagGraph.init(JsonUtil.readValue(definitionRu.dag, ArrayNode.class), expressionEvaluator);
        Map<String, Object> contextVariables = processVariablesService.getVariables(instance.variableId);

        // 任务变量如果和全局变量命名冲突，那么任务变量比全局变量优先级高
        // 但是任务变量去到下一个“用户审批”环节就会失效(网关、系统处理环节不会失效)
        if (dto.variables != null && !dto.variables.isEmpty()) {
            contextVariables.putAll(dto.variables);
        }
        switch (dto.operation) {
            case APPROVE:
                // 保存任务表单（审批人填写）
                if (dto.formData != null && !dto.formData.isEmpty()) {
                    String formDefinitionId = (String) dto.formData.get("id");
                    FormInstance formInstance = formService.saveInstance(formDefinitionId, dto.formData, 1);
                    task.formInstanceId = formInstance.id;
                    Map<String, Object> formVariables = JsonUtil.readValue(formInstance.variables, Map.class);
                    contextVariables.putAll(formVariables);
                }
                processTaskService.approve(task, dto);
                processLogger.info("任务: [{}]{} 审批通过，进入下一环节", task.id, task.name);
                move(dagGraph, instance, contextVariables);
                processMailService.sendApproveMail(currentUser, instance, task, remark);
                break;
            case REJECT: // 拒绝的话那流程就结束了
                processTaskService.reject(task, dto);
                instance.status = ProcessInstanceStatus.FAILED;
                instance.updateTime = new Date();
                processInstanceService.updateById(instance);
                processInstanceIndexService.updateStatus(instance);
                processLogger.info("任务被拒绝:[{}],{}", task.id, task.name);
                // 把流程实例归档
                // 给申请人发送通知，被拒绝了
                SysUser sysUser = sysUserMapper.selectById(instance.starter);
                processMailService.sendRejectMail(currentUser, instance, task, remark, sysUser.email);
                sysMessageService.sendRejectMsg(instance, task);
                break;
            case BACK:
                // 退回到指定的环节
                ProcessTask backwardTask = processTaskService.back(task, dto);
                // 流程实例也回退部分信息
                instance.dagNodeId = backwardTask.dagNodeId;
                instance.preHandlerId = backwardTask.preHandlerId;
                instance.handlerId = ""; // 清空当前审批人信息
                instance.handlerName = ""; // 清空当前审批人信息
                processInstanceService.updateById(instance);
                // TODO 流程变量preHandler之类的是不是应该放进去？
                processLogger.info("任务回退:{}[{}] ==> 回退至:{}[{}]", task.name, task.id, backwardTask.name, backwardTask.id);
                UserTaskNode userTaskNode = dagGraph.obtainUserTaskNode(backwardTask.dagNodeId);
                // 授权，哪些人能收到退回的任务
                contextVariables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                Set<String> emailSet = processTaskService.authorize(instance, backwardTask, userTaskNode, contextVariables);
                // 给这些人发送通知，有一个回退的任务待受理
                processMailService.sendBackMail(currentUser, instance, task, remark, emailSet);
                // 发送站内信，有一个待办事项
                sysMessageService.sendAsyncTodoMsg(instance, backwardTask, emailSet);
                break;
            default:
                throw new ApiException(500, "不支持的操作");
        }
        return processTaskService.auditListAfter(task.instanceId, task.id);
    }


    /**
     * 移动到下一步
     *
     * @return true 继续往下走 false 暂停
     */
    public void move(DagGraph dagGraph, ProcessInstance instance, Map<String, Object> variables) {
        if (instance.status != ProcessInstanceStatus.RUNNING) {
            throw new ApiException(500, "流程不能重复执行");
        }
        final Logger processLogger = ProcessLogger.logger(instance.id);
        // 加入当前流程实例这个变量
        while (true) {
            // 判断接下来往哪里走，并取出连接线
            DagEdge dagEdge = dagGraph.route(instance.dagNodeId, variables);
            // 连接线指向的节点，就是下一个步骤将要执行的节点
            DagNode dagNode = dagEdge.targetNode;
            // 移动指针，下一个节点作为当前节点
            instance.dagNodeId = dagNode.id;
            // 每次都放入最新的实例快照到上下文中
            if (dagNode instanceof UserTaskNode) {
                // 记录上一处理人,当前处理人应该是要等"受理"的时候才赋值,被指派人应该是"被指派"的时候才赋值
                instance.preHandlerId = instance.handlerId;
                instance.handlerId = ""; // 设为null的话mybatis-plus不会更新null的字段
                instance.handlerName = "";
                // 因为用户审批需要人操作，所以为下一个节点和用户创建好一个待办任务后就暂停
                UserTaskNode userTaskNode = (UserTaskNode) dagNode;
                ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.PENDING);
                processLogger.info("进入环节:{},创建待审批任务:[{}]", task.name, task.id);

                variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                // 授权，谁可以审批
                Set<String> emailSet = processTaskService.authorize(instance, task, userTaskNode, variables);
                processLogger.info("发送待审批邮件和站内信：{}", emailSet);
                // 通知负责审批的人有新的待办任务,等他们完成任务后才继续下一步
                processMailService.sendTodoMail(instance, emailSet);
                sysMessageService.sendAsyncTodoMsg(instance, task, emailSet);
                break;
            } else if (dagNode instanceof EndNode) {
                // 成功
                instance.status = ProcessInstanceStatus.SUCCESS;
                instance.dagNodeId = null;
                instance.updateTime = new Date();
                // 添加结束的任务
                processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
                processLogger.info("流程[{}]结束", instance.id);
                // 通知发起人审批完结
                processMailService.sendEndMail(instance);
                sysMessageService.sendSuccessMsg(instance);
                processInstanceIndexService.updateStatus(instance);
                // TODO 把任务和流程实例迁移到历史表
                break; // 终止
            } else if (dagNode instanceof ExecutableNode) {
                // 遇到系统节点，先创建一个“系统任务” 提交异步任务
                instance.preHandlerId = instance.handlerId;
                ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.RUNNING);
                processInstanceService.updateById(instance);
                processLogger.info("进入环节:{},创建任务:[{}]", task.name, task.id);
                variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                executeAsync(dagGraph, (ExecutableNode) dagNode, instance, task, variables);
                // 不要阻塞
                return;
            } else {
                processLogger.info("进入{}环节", dagNode.label);
                // 创建一个任务并尝试自动完成它
                instance.preHandlerId = instance.handlerId;
                processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.SUCCESS);
            }
        }
        ProcessInstance recentInstance = processInstanceService.selectById(instance.id);
        if (recentInstance != null && recentInstance.status == ProcessInstanceStatus.CANCEL) {
            /* 流程实例已被用户撤回，标记那些新创建的任务状态为失败。
            这里是为了处理漏网之鱼：虽说用户发起撤回的时候会将涉及到的运行中的任务标记为失败，但是
            系统还是有可能在此期间生成了新任务，那么这些新任务也标记为失败 */
            processTaskService.cancelByInstanceId(recentInstance.id);
            return;
        }
        processInstanceService.updateById(instance);
    }

    /**
     * 异步执行
     *
     * @param dagGraph
     * @param executableNode
     * @param instance
     * @param task
     * @param variables
     */
    private void executeAsync(DagGraph dagGraph, ExecutableNode executableNode, ProcessInstance instance,
                              ProcessTask task, Map<String, Object> variables) {
        final Logger processLogger = ProcessLogger.logger(instance.id);
        threadPoolTaskExecutor.submitListenable(() -> {
            try {
                processLogger.info("异步执行系统节点:{}[{}]", task.name, task.id);
                executableNode.execute(processLogger, variables);

                // 流程是否已被撤回？如果被撤回那就没下文了，否则继续
                ProcessInstance recentInstance = processInstanceService.selectById(instance.id);
                if (recentInstance != null && recentInstance.status == ProcessInstanceStatus.CANCEL) {
                    processLogger.warn("流程实例已被用户撤回，终止后续任务");
                    return null;
                }
                task.status = ProcessTaskStatus.SUCCESS;
                processTaskService.updateById(task);
                return instance;
            } catch (Exception e) {
                processLogger.error("流程运行异常", e);
                // 系统任务失败，应该回到上个环节？
                task.status = ProcessTaskStatus.FAILED;
                processTaskService.updateById(task);
                instance.status = ProcessInstanceStatus.FAILED;
                processInstanceService.updateById(instance);
                processInstanceIndexService.updateStatus(instance);
                return null;
            }
        }).addCallback(new ListenableFutureCallback<ProcessInstance>() {
            @Override
            public void onFailure(Throwable ex) {
                processLogger.error("流程运行异常", ex);
            }

            @Override
            public void onSuccess(ProcessInstance result) {
                if (result != null) {
                    try {
                        move(dagGraph, instance, variables);
                    } catch (Exception e) {
                        processLogger.error("流程运行异常", e);
                    }
                }
            }
        });
    }

    /**
     * 撤回流程申请
     *
     * @param id
     */
    @Transactional
    public void cancelInstance(String id) {
        ProcessInstance processInstance = processInstanceService.selectById(id);
        if (processInstance == null) {
            throw new ApiException(404, "流程不存在或已被删除，请刷新页面");
        }
        if (processInstance.status != ProcessInstanceStatus.RUNNING) {
            throw new ApiException(500, "流程状态是执行中才可以撤回");
        }
        String starter = AuthenticationContext.current().getId();
        if (!processInstance.starter.equals(starter)) {
            throw new ApiException(500, "申请人才可以撤回");
        }
        processInstance.status = ProcessInstanceStatus.CANCEL;
        Logger processLogger = ProcessLogger.logger(processInstance.id);
        processLogger.info("用户撤回了申请...");
        processInstanceService.updateById(processInstance);
        // 把正在运行的任务标记为失败
        processTaskService.cancelByInstanceId(processInstance.id);
        // 更新ES索引
        processInstanceIndexService.updateStatus(processInstance);
    }
}
