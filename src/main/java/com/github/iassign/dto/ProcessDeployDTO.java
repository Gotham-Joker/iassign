package com.github.iassign.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProcessDeployDTO {
    public String id; // 流程定义ID
    public List<String> deptIds;
}
