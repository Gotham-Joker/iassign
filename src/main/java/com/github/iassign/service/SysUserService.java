package com.github.iassign.service;

import com.github.authorization.AuthenticationContext;
import com.github.base.BaseService;
import com.github.core.ApiException;
import com.github.core.PageResult;
import com.github.iassign.dto.RebindRoleDTO;
import com.github.iassign.dto.SysUserRoleDTO;
import com.github.iassign.entity.SysRole;
import com.github.iassign.entity.SysUser;
import com.github.iassign.mapper.SysRoleMapper;
import com.github.iassign.mapper.SysUserMapper;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SysUserService extends BaseService<SysUser> {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private UploadService uploadService;

    @Transactional(rollbackFor = Exception.class)
    public void save(SysUser user) {
        user.admin = false; // 新增用户默认不是管理员
        super.save(user);
        bindRoles(user.id, user.getRoleIds());
    }

    /**
     * 设为超级管理员
     *
     * @param userId
     * @param isAdmin
     */
    @Transactional
    public void setAdmin(String userId, Boolean isAdmin) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        sysUser.admin = isAdmin;
        sysUserMapper.updateById(sysUser);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateById(SysUser sysUser) {
        SysUser oldUser = Optional.ofNullable(sysUserMapper.selectById(sysUser.id))
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
        BeanUtils.copyProperties(sysUser, oldUser);
        oldUser.admin = null; // 不更新此字段
        super.updateById(oldUser);
        bindRoles(sysUser.id, sysUser.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindRoles(String id, Collection<String> roleIds) {
        //先解绑角色
        sysUserMapper.unBindRoles(id, null);
        if (roleIds != null && !roleIds.isEmpty()) {
            sysUserMapper.bindRoles(id, roleIds);
        }
    }

    public SysUser selectById(String id) {
        return sysUserMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        sysUserMapper.unBindRoles(id, null);
        sysUserMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchIds(List<String> ids) {
        ids.forEach(this::deleteById);
    }

    public List<SysUser> list() {
        return baseMapper.selectList(null);
    }

    /**
     * 上传头像
     *
     * @param file
     * @return
     */
    @Transactional
    public String uploadAvatar(MultipartFile file) throws IOException {
        String avatarUrl = uploadService.upload(file).get("url");
        String userId = AuthenticationContext.current().getId();
        SysUser sysUser = sysUserMapper.selectById(userId);
        sysUser.avatar = avatarUrl;
        sysUserMapper.updateById(sysUser);
        return avatarUrl;
    }

    public PageResult<SysUserRoleDTO> selectByUserRole(Integer page, Integer size, SysUserRoleDTO dto) {
        PageHelper.startPage(page, size);
        return PageResult.of(sysUserMapper.selectByUserRole(dto));
    }

    /**
     * 重新绑定角色
     *
     * @param dtos
     */
    @Transactional
    public void rebindRoles(List<RebindRoleDTO> dtos) {
        for (RebindRoleDTO dto : dtos) {
            if (StringUtils.hasText(dto.roleId)) {
                SysRole sysRole = sysRoleMapper.selectById(dto.roleId);
                if (sysRole == null) {
                    log.warn("rebind role error,role is not exists:{}", dto.roleId);
                    return;
                }
                if (!CollectionUtils.isEmpty(dto.delUserIds)) {
                    dto.delUserIds.forEach(userId -> sysUserMapper.unBindRoles(userId, Collections.singleton(dto.roleId)));
                }
                if (!CollectionUtils.isEmpty(dto.addUserIds)) {
                    dto.addUserIds.forEach(userId -> sysUserMapper.bindRoles(userId, Collections.singleton(dto.roleId)));
                }
            }
        }
    }
}
