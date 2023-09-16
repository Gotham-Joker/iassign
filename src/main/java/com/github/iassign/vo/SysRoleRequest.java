package com.github.iassign.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 添加角色请求
 */
@Data
public class SysRoleRequest {
    public String id;
    @NotNull
    public String name;
    public List<String> permissionIds;
    public List<String> menuIds;
}
