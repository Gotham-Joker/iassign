package com.github.iassign.service;

import com.github.base.BaseService;
import com.github.iassign.entity.SysPermission;
import com.github.iassign.event.PmChangeEvent;
import com.github.iassign.mapper.SysPermissionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SysPermissionService extends BaseService<SysPermission> {

    @Autowired
    private SysPermissionMapper sysPermissionMapper;


    public List<SysPermission> findAll() {
        List<SysPermission> permissions = sysPermissionMapper.selectList(null);
        if (permissions != null) {
            return permissions;
        }
        return Collections.EMPTY_LIST;
    }


    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
//        SysPermission oldPermission = sysPermissionMapper.selectById(id);
        sysPermissionMapper.unBindRole(id);
        sysPermissionMapper.deleteById(id);
        // 通知所有授权中心服务实例卸载旧权限
//        send(new PmChangeEvent(null, oldPermission));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchIds(List<Integer> ids) {
        if (ids != null) {
            ids.forEach(this::deleteById);
        }
    }

    public SysPermission selectById(Integer id) {
        return sysPermissionMapper.selectById(id);
    }

    public void save(SysPermission permission) {
        // 通知系统加载新权限
//        send(new PmChangeEvent(permission, null));
        super.save(permission);
    }

    public void update(SysPermission permission) {
//        SysPermission oldPermission = sysPermissionMapper.selectById(permission.getId());
        sysPermissionMapper.updateById(permission);
        // 通知系统权限有变更，重新加载该权限
//        send(new PmChangeEvent(permission, oldPermission));
    }


    /**
     * 查询用户的权限(标识)
     *
     * @param sysUserId
     * @return
     */
    public List<SysPermission> selectBySysUserId(Serializable sysUserId) {
        return sysPermissionMapper.selectBySysUserId(sysUserId);
    }

    // 角色权限查询
    public List<SysPermission> selectRolePermissions(Serializable roleId) {
        return sysPermissionMapper.selectRolePermissions(roleId);
    }

    public void send(PmChangeEvent event) {
    }

}
