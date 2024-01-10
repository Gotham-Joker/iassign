package com.github.iassign.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.iassign.enums.ProcessInstanceStatus;
import lombok.Data;

import java.util.Date;

/**
 * 流程实例
 */
@Data
@TableName("t_process_instance")
public class ProcessInstance {
    @TableId(type = IdType.INPUT)
    public String id;
    public String definitionId; // 流程定义ID
    public String name; // 流程实例名
    public String starter; // 发起人ID
    public String starterName; // 发起人姓名
    public String deptId; // 发起人所在部门ID
    public String handlerId; // 当前处理人用户ID
    public String handlerName; // 当前处理人用户名
    public String preHandlerId; // 上一处理人用户ID
    public String formInstanceId; // 发起人提交的表单实例ID
    public String variableId; // 全局变量ID
    public String dagNodeId; // 当前执行中的节点ID
    public Boolean returnable; // 是否允许回退至发起人 0-不允许 1-允许
    public String emails; // 消息接收人id清单
    public ProcessInstanceStatus status; // 0-已撤回 1-执行中 2-成功 3-失败
    public Date createTime;
    public Date updateTime;
    public String ruId; // 运行时definitionRuId，关联ProcessDefinitionRu
}
