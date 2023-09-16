package com.github.iassign.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.iassign.entity.SysPermission;
import lombok.Data;

/**
 * 权限变更事件，用于通知授权中心刷新权限
 */
@Data
public class PmChangeEvent {
    /**
     * 新权限会被加载
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SysPermission newPermission;
    /**
     * 旧权限会被移除
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SysPermission oldPermission;

    public PmChangeEvent() {

    }

    public PmChangeEvent(SysPermission newPermission, SysPermission oldPermission) {
        this.newPermission = newPermission;
        this.oldPermission = oldPermission;
    }
}
