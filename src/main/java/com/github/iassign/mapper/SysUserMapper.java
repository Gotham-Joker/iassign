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
import com.github.iassign.dto.SysUserRoleDTO;
import com.github.iassign.entity.SysUser;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SysUserMapper extends BaseMapper<SysUser> {
    //绑定角色
    @Insert({"<script>",
            "insert into sys_user_role values ",
            "<foreach collection=\"roleIds\" item=\"item\" separator=\",\"> ",
            "(#{sysUserId},#{item}) ",
            "</foreach> ",
            "</script>"})
    int bindRoles(@Param("sysUserId") String sysUserId, @Param("roleIds") Collection<String> roleIds);

    //解绑角色
    @Delete({"<script>",
            "delete from sys_user_role where user_id = #{sysUserId} ",
            "<if test='roleIds!=null and !roleIds.isEmpty()'>",
            " and role_id in ",
            "<foreach collection='roleIds' item='item' separator=',' open='(' close=')'> ",
            "#{item}",
            "</foreach> ",
            "</if>",
            "</script>"})
    int unBindRoles(@Param("sysUserId") String sysUserId, @Param("roleIds") Collection<String> roleIds);

    /**
     * 根据角色ID查找
     *
     * @param roleId
     * @return
     */
    @Select({"select * from sys_user u where u.dept_id=#{deptId} ",
            "and exists(select 1 from sys_user_role ur where ur.role_id=#{roleId} and u.id=ur.user_id)"})
    List<SysUser> selectByRoleId(@Param("roleId") String roleId);

    @Select({"<script>",
            "select id,username,email from sys_user <where> ",
            "<if test='id!=null and id!=\"\"'>and id=#{id}</if>",
            "<if test='username!=null and username!=\"\"'>and username like CONCAT('%',#{username},'%')</if>",
            "<if test='email!=null and email!=\"\"'>and email like CONCAT('%',#{email},'%')</if>",
            "<if test='roleId!=null and roleId!=\"\"'>",
            "and exists (select 1 from sys_user_role ur where ur.role_id=#{roleId} and ur.user_id=sys_user.id)",
            "</if>",
            "<if test='roleIdIn!=null and roleIdIn!=\"\"'>",
            "and exists (select 1 from sys_user_role ur where ur.role_id in ",
            "<foreach collection=\"roleIds\" item=\"item\" separator=\",\" open='(' close=')'> ",
            "#{item}",
            "</foreach> ",
            "and ur.user_id=sys_user.id)",
            "</if>",
            "<if test='roleIdLike!=null and roleIdLike!=\"\"'>and exists (select 1 from sys_user_role ur where ur.role_id like CONCAT('%',#{roleIdLike},'%') and ur.user_id=sys_user.id)</if>",
            "<if test='roleIdLikeLeft!=null and roleIdLikeLeft!=\"\"'>and exists (select 1 from sys_user_role ur where ur.role_id like CONCAT('%',#{roleIdLikeLeft}) and ur.user_id=sys_user.id)</if>",
            "<if test='roleIdLikeRight!=null and roleIdLikeRight!=\"\"'>and exists (select 1 from sys_user_role ur where ur.role_id like CONCAT(#{roleIdLikeRight},'%') and ur.user_id=sys_user.id)</if>",
            "</where></script>"})
    List<SysUser> selectByUserRole(SysUserRoleDTO dto);

}
