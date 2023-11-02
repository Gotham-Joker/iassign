package com.github.iassign.entity;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 这个表存到ES中
 */
@Data
public class ProcessInstanceIndex {
    public String id;
    public String definitionId; // 流程定义ID
    public String name; // 流程实例名
    public String starter; // 发起人ID
    public String starterName; // 发起人姓名
    public String deptId; // 发起人部门ID
    public String deptName; // 发起人部门
    public String status; // 流程状态
    public String createTime; // 发起时间
    public Map<String, Object> variables; // 发起人填写的表单
    public String content; // 内容(将变量的值提取之后统一存起来)
}
