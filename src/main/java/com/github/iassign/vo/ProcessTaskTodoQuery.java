package com.github.iassign.vo;

import lombok.Data;

import java.util.Set;

@Data
public class ProcessTaskTodoQuery {
    // 发起人统一认证
    public String starter;
    // 申请编号
    public String instanceId;
    // 流程名
    public String definitionName;
    // 申请时间 起
    public String createTime_ge;
    // 申请时间 止
    public String createTime_le;
    public String userId;
    public Set<String> referenceIds;
}
