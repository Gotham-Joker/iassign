package com.github.iassign.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.Result;
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
import com.github.iassign.mapper.SysUserMapper;
import com.github.iassign.vo.TaskAuthVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;


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
    private ProcessOpinionService processOpinionService;
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
    public void initEvaluator() {
        // 这玩意会将DAG流程图里面写的脚本编译成字节码并缓存，可能会导致jvm中的class爆满，是否应该定期清空一下？
        expressionEvaluator = new DefaultExpressionEvaluator(applicationContext);
        // interval(8,hour).subscribe(()=>expressionEvaluator.clear())
    }

    /**
     * 启动流程实例
     *
     * @return
     */
    @Transactional
    public ProcessInstance startInstance(ProcessStartDTO dto) throws Exception {
        UserDetails userDetails = AuthenticationContext.current().getDetails();
        // 流程发起人
        dto.starter = userDetails.getId();
        // 校验
        ProcessDefinition definition = processDefinitionMapper.selectById(dto.definitionId);
        if (definition == null) {
            throw new ApiException(500, "工作流不存在，可能已被删除，请刷新页面");
        }
        if (!Boolean.TRUE.equals(definition.status)) {
            throw new ApiException(500, "流程未部署，禁止启动");
        }
//        SysUser sysUser = sysUserMapper.selectById(dto.starter);

        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(definition.ruId);
        if (definitionRu == null) {
            throw new ApiException(404, "流程图不存在或已被删除，流程启动失败");
        }
        definition.dag = definitionRu.dag;

        ProcessInstance instance = processInstanceService.create(dto, definition, definitionRu, userDetails);

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
        ProcessVariables processVariables = new ProcessVariables();
        processVariables.instanceId = instance.id;
        processVariables.data = JsonUtil.toJson(contextVariables);
        processVariablesService.save(processVariables);
        instance.variableId = processVariables.id; // 绑定为全局变量 (整个流程都会生效)

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

        // 将当前流程放进去
        contextVariables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
        try {
            // 进入下一个环节
            move(dagGraph, instance, contextVariables);
        } catch (Exception e) {
            // 删除ES中的内容
            processInstanceIndexService.delete(instance);
            throw e;
        }

        // 发邮件
        processMailService.sendStartMail(instance);
        return instance;
    }

    /**
     * 退回申请人之后重启流程
     *
     * @param dto
     */
    @Transactional
    public Result restartInstance(ProcessStartDTO dto) {
        // 先找出流程实例
        ProcessInstance instance = processInstanceService.selectById(dto.instanceId);
        // 进行各种校验
        if (!Boolean.TRUE.equals(instance.returnable)) {
            return Result.error(500, "流程不支持退回至发起人，禁止修改");
        }
        if (instance.status != ProcessInstanceStatus.RETURN) {
            return Result.error(500, "流程不是退回发起人状态，禁止修改");
        }
        UserDetails userDetails = AuthenticationContext.details();
        if (!instance.starter.equals(userDetails.id)) {
            return Result.error(500, "只有发起人可以重新发起流程");
        }
        final Logger processLogger = ProcessLogger.logger(dto.instanceId);
        processLogger.error("申请人重新发起流程，表单:{}", dto.formData);
        // 保存表单变量，覆盖一些全局流程变量，并且更新流程实例的表单变量id
        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(instance.ruId);
        if (definitionRu == null) {
            return Result.error(404, "流程图不存在或已被删除，流程启动失败");
        }
        // 继续往下执行
        // 准备流转用的全局上下文变量
        ProcessVariables processVariables = processVariablesService.selectById(instance.variableId);
        Map<String, Object> contextVariables = JsonUtil.readValue(processVariables.data, Map.class);

        // 如果有表单，就保存表单，并且解析里面的field和value
        if (dto.formData != null && !dto.formData.isEmpty()) {
            formService.updateInstance(instance.formInstanceId, dto.formData, 1);
            // 解析出来的表单变量化为流程变量
            formService.mergeVariables(instance.formInstanceId, contextVariables);
        }
        // 如果存在其他全局变量，也放进上下文中，将其和表单变量合并起来，存到数据库
        if (dto.variables != null && !dto.variables.isEmpty()) {
            contextVariables.putAll(dto.variables);
        }
        processVariables.data = JsonUtil.toJson(contextVariables);
        processVariablesService.updateById(processVariables);

        // 创建表达式计算器，用于处理各种动态条件
        ExpressionEvaluator expressionEvaluator = new DefaultExpressionEvaluator(applicationContext);
        // 初始化流程图，并获取开始节点
        DagGraph dagGraph = DagGraph.init(JsonUtil.readValue(definitionRu.dag, ArrayNode.class), expressionEvaluator);
        StartNode startNode = dagGraph.startNode;

        // 设置当前节点，刚启动的时候肯定是开始节点
        instance.dagNodeId = startNode.id;
        instance.status = ProcessInstanceStatus.RUNNING;
        // 在ES中更新索引
        try {
            processInstanceIndexService.update(instance, contextVariables);
        } catch (IOException e) {
            processLogger.error("更新ES索引失败", e);
            // 回滚事务
            throw new RuntimeException("系统异常");
        }
        // 日志路由
        processLogger.info("用户:{}[{}]，重新启动了流程，申请编号:{}", userDetails.username, userDetails.username, instance.id);
        // 将当前流程放进去
        contextVariables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
        // 进入下一个环节
        move(dagGraph, instance, contextVariables);
        // 发邮件
        processMailService.sendStartMail(instance);
        return Result.success(dto.instanceId);
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
        if (instance.status != ProcessInstanceStatus.RUNNING) {
            throw new ApiException(500, "流程不是运行状态");
        }
        if (task.status != ProcessTaskStatus.ASSIGNED
                && task.status != ProcessTaskStatus.CLAIMED) {
            throw new ApiException(500, "操作无效，当前审批任务可能已被其他人处理，请刷新");
        }
        // 判断当前用户是否有权限审批
        UserDetails currentUser = AuthenticationContext.current().getDetails();
        TaskAuthVO taskAuthVO = processTaskService.validateAuthorize(currentUser.getId(), task);
        if (!taskAuthVO.canAudit) {
            throw new ApiException(500, "当前用户无权审批");
        }
        Logger processLogger = ProcessLogger.logger(instance.id);
        // 设置邮件接收人并批量发送
        instance.emails = dto.emails == null ? "" : dto.emails;

        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(instance.ruId);
        DagGraph dagGraph = DagGraph.init(JsonUtil.readValue(definitionRu.dag, ArrayNode.class), expressionEvaluator);
        Map<String, Object> contextVariables = processVariablesService.getVariables(instance.variableId);

        // 变量优先级： 全局变量 < 表单变量 < 临时变量
        // 表单变量和临时变量去到下一个“用户审批”环节就会失效(网关、系统处理环节会保持有效)
        if (dto.variables != null && !dto.variables.isEmpty()) {
            ProcessVariables processVariables = new ProcessVariables();
            processVariables.instanceId = task.instanceId;
            processVariables.data = JsonUtil.toJson(dto.variables);
            processVariablesService.save(processVariables);
            task.variableId = processVariables.id;
            contextVariables.putAll(dto.variables);
        }
        // 提交审批意见 并发送邮件提醒，注意，此处是的邮件内容是 ”审批意见“
        ProcessOpinion processOpinion = processOpinionService.submitOpinion(currentUser, instance, task, dto);
        // 发起人详细信息，有可能会需要到
        SysUser sysUser;
        switch (dto.operation) {
            case APPROVE:
                // ********* 把表单中的变量整合到上下文变量中(表单临时变量优先级低于任务临时变量dto.variables) *********
                if (dto.formData != null && !dto.formData.isEmpty()) {
                    String formDefinitionId = (String) dto.formData.get("id");
                    FormInstance formInstance;
                    if (StringUtils.hasText(task.formInstanceId)) {
                        formInstance = formService.updateInstance(task.formInstanceId, dto.formData, 1);
                    } else {
                        formInstance = formService.saveInstance(formDefinitionId, dto.formData, 1);
                        task.formInstanceId = formInstance.id;
                    }
                    Map<String, Object> formVariables = JsonUtil.readValue(formInstance.variables, Map.class);
                    // 表单变量优先级较临时变量低
                    if (dto.variables != null) {
                        formVariables.forEach((k, v) -> {
                            if (!dto.variables.containsKey(k)) {
                                contextVariables.put(k, v);
                            }
                        });
                    } else {
                        contextVariables.putAll(formVariables);
                    }
                }
                // 发送邮件提醒
                processMailService.sendApproveMail(instance, task, processOpinion);
                // 等待会签结束，当前任务节点结束才继续往下执行
                if (!processTaskService.canFinish(task)) {
                    processLogger.info("当前用户<{}>[{}]已提交审批意见，但当前审批环节<{}>[{}] 还处于'会签'状态（需要等其他人提交审批意见）。",
                            currentUser.username, currentUser.id, task.name, task.id);
                    instance.handlerId = currentUser.id;
                    processTaskService.updateById(task);
                    processInstanceService.updateById(instance);
                    return processTaskService.auditListAfter(task.instanceId, task.id);
                }
                task.status = ProcessTaskStatus.SUCCESS;
                task.updateTime = new Date();
                processTaskService.updateById(task);
                processLogger.info("<{}>[{}] 审批通过，进入下一环节", task.name, task.id);
                contextVariables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                // 继续往下执行
                move(dagGraph, instance, contextVariables);
                break;
            case REJECT:
                // 拒绝的话那流程就结束了
                processTaskService.reject(task);
                instance.status = ProcessInstanceStatus.REJECTED;
                instance.updateTime = new Date();
                processInstanceService.updateById(instance);
                processInstanceIndexService.updateStatus(instance);
                processLogger.info("审批环节<{}>[{}]被<{}>[{}]拒绝", task.name, task.id, currentUser.username, currentUser.id);
                // 把流程实例归档
                // 给申请人发送通知，被拒绝了
                sysUser = sysUserMapper.selectById(instance.starter);
                processMailService.sendRejectMail(instance, task, processOpinion, sysUser.email);
                sysMessageService.sendRejectMsg(instance, task, AuthenticationContext.details());
                fallback(instance);
                break;
            case BACK:
                // 退回到指定的环节
                ProcessTask backwardTask = processTaskService.back(task, dto.backwardTaskId);
                // 流程实例也回退部分信息
                instance.dagNodeId = backwardTask.dagNodeId;
                instance.preHandlerId = backwardTask.preHandlerId;
                instance.handlerId = ""; // 清空当前审批人信息
                instance.handlerName = ""; // 清空当前审批人信息
                processInstanceService.updateById(instance);
                processLogger.info("审批环节{}[{}]被<{}>[{}]回退: ==> 回退至:{}[{}]", task.name, task.id,
                        currentUser.username, currentUser.id, backwardTask.name, backwardTask.id);
                UserTaskNode userTaskNode = dagGraph.obtainUserTaskNode(backwardTask.dagNodeId);
                // 授权，哪些人能收到退回的任务
                contextVariables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                Set<String> emailSet = processTaskService.authorize(instance, backwardTask, userTaskNode, contextVariables);
                // 给这些人发送通知，有一个回退的任务待受理
                processMailService.sendBackMail(instance, task, processOpinion, emailSet);
                // 发送站内信，有一个待办事项
                sysMessageService.sendAsyncTodoMsg(instance, backwardTask, emailSet);
                break;
            case RETURN:
                // 退回至发起人
                if (!Boolean.TRUE.equals(instance.returnable)) {
                    throw new ApiException(500, "该流程不支持回退至发起人");
                }
                processTaskService.returnToStarter(task);
                instance.preHandlerId = currentUser.id;
                instance.dagNodeId = dagGraph.startNode.id;
                instance.status = ProcessInstanceStatus.RETURN;
                instance.handlerId = instance.starter;
                instance.handlerName = instance.starterName;
                processInstanceService.updateById(instance);
                // 给发起人发送一封回退邮件
                sysUser = sysUserMapper.selectById(instance.starter);
                processMailService.sendBackMail(instance, task, processOpinion, Collections.singleton(sysUser.email));
                fallback(instance);
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
            throw new ApiException(500, "流程不是运行状态");
        }
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
                // 处理用户审批节点
                processInstanceService.handleUserTaskNode(dagNode, dagEdge, instance, variables);
                break;
            } else if (dagNode instanceof EndNode) {
                processInstanceService.handleEndNode(dagEdge, instance);
                // TODO 把任务和流程实例迁移到历史表
                break; // 终止
            } else if (dagNode instanceof ExecutableNode) {
                // MARK-01 移动指针，当前处理人变成了上一处理人，并创建相关的任务
                instance.preHandlerId = instance.handlerId;
                ProcessTask task = processTaskService.createTask(instance, dagEdge, ProcessTaskStatus.RUNNING);
                executeAsync(dagGraph, (ExecutableNode) dagNode, task, instance, variables, AuthenticationContext.current());
                // 不要阻塞
                return;
            } else {
                processInstanceService.handleOtherNode(dagNode, dagEdge, instance);
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

    private void executeAsync(DagGraph dagGraph, ExecutableNode executableNode, ProcessTask task, ProcessInstance instance, Map<String, Object> variables, Authentication authentication) {
        CompletableFuture.supplyAsync(() -> processInstanceService.handleExecutableNode(executableNode, task, instance, variables),
                threadPoolTaskExecutor).whenComplete((result, err) -> {
            if (err == null) {
                if (result != null) {
                    try {
                        processInstanceService.updateById(instance);
                        AuthenticationContext.setAuthentication(authentication);
                        variables.put(Constants.INSTANCE, new ProcessInstanceSnapshot(instance));
                        move(dagGraph, instance, variables);
                    } finally {
                        AuthenticationContext.clearContext();
                    }
                }
            } else {
                ProcessLogger.logger(instance.id).error("流程运行异常", err);
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
        fallback(processInstance);
    }

    /**
     * 拒绝、失败、撤回时，执行fallback
     *
     * @param processInstance
     */
    public void fallback(ProcessInstance processInstance) {
        CompletableFuture.runAsync(() -> {
            Logger processLogger = ProcessLogger.logger(processInstance.id);
            // 执行流程回调
            ProcessDefinition definition = processDefinitionMapper.selectById(processInstance.definitionId);
            if (StringUtils.hasText(definition.fallback)) {
                ClassicHttpRequest get = ClassicRequestBuilder.get(definition.fallback)
                        .addParameter("defId", definition.id)
                        .addParameter("instId", processInstance.id)
                        .addParameter("status", processInstance.status.name())
                        .addParameter("starter", processInstance.starter).build();
                try (CloseableHttpClient client = HttpClients.createDefault();
                     CloseableHttpResponse response = client.execute(get)) {
                    if (response.getCode() == 200) {
                        HttpEntity responseEntity = response.getEntity();
                        String string = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                        if (StringUtils.hasText(string) && string.contains("\"code\":0")) {
                            log.info("withdraw process，fallback:{}", string);
                        } else {
                            processLogger.error("withdraw process,executing fallback error,result:{}", string);
                        }
                    } else {
                        processLogger.error("withdraw process,executing fallback error,http code:{}", response.getCode());
                    }
                } catch (Exception e) {
                    processLogger.error("withdraw process,executing fallback error", e);
                }
            }
        }, threadPoolTaskExecutor);
    }

    @Transactional
    public List<ProcessTask> recover(String taskId) {
        ProcessTask task = processTaskService.selectById(taskId);
        if (task == null) {
            throw new ApiException(404, "任务不存在:" + taskId);
        }
        if (task.status != ProcessTaskStatus.FAILED) {
            throw new ApiException(500, "失败的任务才能恢复");
        }
        ProcessInstance instance = processInstanceService.selectById(task.instanceId);
        if (instance.status != ProcessInstanceStatus.FAILED) {
            throw new ApiException(500, "失败的流程才能恢复");
        }

        // 取出流程图
        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(instance.ruId);
        if (definitionRu == null) {
            throw new ApiException(500, "恢复失败，可能时因为流程图已被管理员删除");
        }
        Logger processLogger = ProcessLogger.logger(instance.id);
        DagGraph dagGraph = DagGraph.init(JsonUtil.readValue(definitionRu.dag, ArrayNode.class), expressionEvaluator);
        final Map context = new HashMap<>();
        // 取出上下文变量
        if (StringUtils.hasText(task.variableId)) {
            ProcessVariables variables = processVariablesService.selectById(task.variableId);
            context.putAll(JsonUtil.readValue(variables.data, Map.class));
        }
        // 恢复作业为运行中
        task.status = ProcessTaskStatus.RUNNING;
        processTaskService.updateById(task);
        // 恢复流程状态为“运行中”
        instance.status = ProcessInstanceStatus.RUNNING;
        processInstanceService.updateById(instance);
        DagNode dagNode = dagGraph.obtainDagNode(instance.dagNodeId);
        // 开搞，恢复作业
        Authentication authentication = AuthenticationContext.current();
        processLogger.warn("用户[{}]正在尝试恢复失败的作业", authentication.getId());
        executeAsync(dagGraph, (ExecutableNode) dagNode, task, instance, context, authentication);
        return processTaskService.auditListAfter(instance.id, taskId);
    }


}
