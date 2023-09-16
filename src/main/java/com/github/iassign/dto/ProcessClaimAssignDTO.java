package com.github.iassign.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

/**
 * 认领任务或者指派任务
 */
@Data
public class ProcessClaimAssignDTO {
    @NotEmpty(message = "请提供任务ID")
    public String taskId;
    public String userId; // 指派的时候需要
    public String avatar;
    public String username;
    public String email;
    public String remark;
}
