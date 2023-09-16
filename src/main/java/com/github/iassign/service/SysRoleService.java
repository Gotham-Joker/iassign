package com.github.iassign.service;

import com.github.base.BaseService;
import com.github.core.ApiException;
import com.github.iassign.entity.SysMenu;
import com.github.iassign.entity.SysPermission;
import com.github.iassign.entity.SysRole;
import com.github.iassign.vo.SysRoleDetails;
import com.github.iassign.vo.SysRoleRequest;
import com.github.iassign.mapper.SysMenuMapper;
import com.github.iassign.mapper.SysPermissionMapper;
import com.github.iassign.mapper.SysRoleMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SysRoleService extends BaseService<SysRole> {
    @Autowired
    private SysRoleMapper baseMapper;
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Transactional(rollbackFor = Exception.class)
    public void save(SysRoleRequest request) {
        SysRole sysRole = new SysRole();
        sysRole.setName(request.getName());
        baseMapper.insert(sysRole);
        // 绑定权限
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            baseMapper.bindPermission(sysRole.getId(), request.getPermissionIds());
        }
        // 绑定菜单
        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            baseMapper.bindMenu(sysRole.getId(), request.getMenuIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Serializable id) {
        // 解绑权限
        baseMapper.unBindPermission((String) id);
        // 解绑菜单
        baseMapper.unBindMenu((String) id);
        // 解绑用户
        baseMapper.unBindUser((String) id);
        // 删除角色
        super.delete(id);
    }

    public List<SysRole> selectBySysUserId(String sysUserId) {
        return baseMapper.selectBySysUserId(sysUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleRequest request) {
        String id = request.id;
        SysRole sysRole = Optional.ofNullable(baseMapper.selectById(id)).orElseThrow(() -> new ApiException(404, "指定的角色不存在"));
        sysRole.setName(request.getName());
        baseMapper.updateById(sysRole);

        if (request.getPermissionIds() != null) {
            baseMapper.unBindPermission(id);
            if (!request.getPermissionIds().isEmpty()) {
                baseMapper.bindPermission(sysRole.getId(), request.getPermissionIds());
            }
        }

        if (request.getMenuIds() != null) {
            baseMapper.unBindMenu(id);
            if (!request.getMenuIds().isEmpty()) {
                baseMapper.bindMenu(sysRole.getId(), request.getMenuIds());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchIds(List<Integer> ids) {
        ids.forEach(this::delete);
    }

    public List<SysRole> selectAll() {
        return baseMapper.selectList(null);
    }

    /**
     * 查询角色详情
     *
     * @param id
     * @return
     */
    public SysRoleDetails selectDetails(String id) {
        SysRole sysRole = baseMapper.selectById(id);
        SysRoleDetails details = new SysRoleDetails();
        BeanUtils.copyProperties(sysRole, details);

        details.menuIds = sysMenuMapper.selectByRoleId(id).stream()
                .map(SysMenu::getId).collect(Collectors.toList());

        details.permissionIds = sysPermissionMapper.selectRolePermissions(id).stream()
                .map(SysPermission::getId).collect(Collectors.toList());

        return details;
    }

    public List<SysRole> list() {
        return baseMapper.selectList(null);
    }
}
