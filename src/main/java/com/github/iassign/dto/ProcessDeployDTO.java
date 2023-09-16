package com.github.iassign.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProcessDeployDTO {
    public String id; // 流程定义ID
    public Boolean status; // 流程部署状态
    public List<String> deptIds;
}
