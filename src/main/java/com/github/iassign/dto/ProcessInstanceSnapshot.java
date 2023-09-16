package com.github.iassign.dto;

import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.enums.ProcessInstanceStatus;
import lombok.Data;

/**
 * 流程实例快照
 */
@Data
public class ProcessInstanceSnapshot {
    public String id;
    public String definitionId; // 流程定义ID
    public String name; // 流程实例名
    public String starter; // 发起人ID
    public String starterName; // 发起人姓名
    public String deptId; // 发起人所在部门ID
    public String handlerId; // 当前处理人用户ID
    public String handlerName; // 当前处理人用户名
    public String preHandlerId; // 上一处理人用户ID
    public ProcessInstanceStatus status; // 0-已撤回 1-执行中 2-成功 3-失败

    public ProcessInstanceSnapshot(ProcessInstance instance) {
        this.id = instance.id;
        this.definitionId = instance.definitionId;
        this.name = instance.name;
        this.starter = instance.starter;
        this.starterName = instance.starterName;
        this.handlerId = instance.handlerId;
        this.handlerName = instance.handlerName;
        this.preHandlerId = instance.preHandlerId;
        this.status = instance.status;
        this.deptId = instance.deptId;
    }
}
