/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
