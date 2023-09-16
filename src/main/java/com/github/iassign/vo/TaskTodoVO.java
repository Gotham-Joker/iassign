package com.github.iassign.vo;

import com.github.iassign.enums.ProcessTaskStatus;
import lombok.Data;

import java.util.Date;

/**
 * 查询待办
 */
@Data
public class TaskTodoVO {
    public String instanceId; // 流程实例id
    public String definitionName; // 流程定义名
    public String starter; // 发起人ID
    public String starterName; // 发起人姓名
    public String taskId; // 任务ID
    public String taskName; // 任务名
    public ProcessTaskStatus status; // 状态
    public String formId; // 需要填写的表单ID
    // 创建时间
    public Date createTime;

}
