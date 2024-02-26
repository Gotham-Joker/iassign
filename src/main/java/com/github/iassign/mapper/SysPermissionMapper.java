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

package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.github.iassign.vo.SysPermissionQuery;
import com.github.iassign.entity.SysPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    /**
     * 分页查询
     */
    @Select({"<script>",
            "SELECT * FROM sys_permission ",
            "<where> ",
            "    <if test=\"query.id != null\"> ",
            "        and id = #{query.id} ",
            "    </if> ",
            "    <if test=\"query.name!=null and query.name!=''\"> ",
            "        and `name` like CONCAT(\"%\",#{query.name},\"%\") ",
            "    </if> ",
            "</where> ",
            "order by create_time",
            "</script>"})
    Page<SysPermission> selectByPage(@Param("query") SysPermissionQuery query);

    @Select({"select sp.* from sys_permission sp ",
            " right join sys_role_permission srp on sp.id = srp.permission_id ",
            " left  join sys_user_role sur on sur.role_id = srp.role_id ",
            " where sur.user_id = #{sysUserId}"})
    List<SysPermission> selectBySysUserId(@Param("sysUserId") Serializable sysUserId);

    @Select("select * from sys_role_permission rp LEFT JOIN sys_permission p on rp.permission_id = p.id where rp.role_id = #{sysRoleId}")
    List<SysPermission> selectBySysRoleId(@Param("sysRoleId") Serializable sysRoleId);

    @Select("select * from sys_permission where pid = #{pid}")
    List<SysPermission> selectByPid(@Param("pid") Serializable pid);

    @Select("select count(role_id) from sys_role_permission where permission_id = #{id} and role_id=#{roleId}")
    Integer countByIdAndRoleId(@Param("id") Serializable id, @Param("roleId") Serializable roleId);

    // 解绑角色
    @Delete("delete from sys_role_permission where permission_id =#{id}")
    void unBindRole(@Param("id") Serializable id);

    // 查找角色拥有的权限(全量)
    @Select({"SELECT * FROM sys_permission sp ",
            "where exists (select 1 from sys_role_permission srp where srp.role_id=#{roleId} and srp.permission_id=sp.id)"})
    List<SysPermission> selectRolePermissions(@Param("roleId") Serializable roleId);
}
