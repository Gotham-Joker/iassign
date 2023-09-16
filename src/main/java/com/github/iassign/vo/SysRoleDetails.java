package com.github.iassign.vo;

import com.github.iassign.entity.SysRole;

import java.util.List;

public class SysRoleDetails extends SysRole {
    public List<String> menuIds;
    public List<String> permissionIds;

    public List<String> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<String> menuIds) {
        this.menuIds = menuIds;
    }

    public List<String> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<String> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
