package com.github.iassign.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

/**
 * 包装着启动流程实例所需的参数
 */
public class ProcessStartDTO {
    // 需要启动的流程定义ID
    @NotEmpty(message = "请提供流程定义ID")
    public String definitionId;
    // 流程变量
    public Map<String, Object> variables;
    // 表单数据，里面包含表单的渲染信息，不能直接拿来当流程变量
    // 需要特殊处理然后转换为流程变量
    public Map<String, Object> formData;
    // 流程发起人的ID
    public String starter;
    // 邮件接收人
    public String emails;
}
