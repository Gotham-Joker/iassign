package com.github.iassign.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
public class RoleMenuRequest {
    private Integer roleId;
    @NotNull(message = "pid不能为空")
    private Integer pid;
}
