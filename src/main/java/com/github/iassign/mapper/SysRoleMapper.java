package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.github.iassign.entity.SysRole;
import com.github.iassign.vo.SysRoleQuery;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysRoleMapper extends BaseMapper<SysRole> {
    /**
     * 分页查询
     */
    @Select({"<script>",
            "SELECT * FROM sys_role ",
            "<where> ",
            "    <if test=\"query.id != null\"> ",
            "        and id = #{query.id} ",
            "    </if> ",
            "    <if test=\"query.name!=null and query.name!=''\"> ",
            "        and `name` like CONCAT('%',#{query.name},'%') ",
            "    </if> ",
            "</where>",
            "</script>"})
    Page<SysRole> selectByPage(@Param("query") SysRoleQuery query);

    @Select("select r.* from sys_role r right join  sys_user_role ur on r.id = ur.role_id where ur.user_id = #{sysUserId}")
    List<SysRole> selectBySysUserId(@Param("sysUserId") String sysUserId);

    // 绑定权限
    @Insert({"<script>",
            "insert into sys_role_permission values ",
            "<foreach collection=\"permissionIds\" item=\"item\" separator=\",\">(#{sysRoleId},#{item})</foreach> ",
            "</script>"})
    int bindPermission(@Param("sysRoleId") String sysRoleId, @Param("permissionIds") List<String> permissionIds);

    // 解绑权限
    @Delete("delete from sys_role_permission where role_id =#{id}")
    int unBindPermission(@Param("id") String roleId);

    // 解绑菜单
    @Delete("delete from sys_role_menu where role_id = #{id}")
    void unBindMenu(@Param("id") String id);

    @Insert({"<script>",
            "insert into sys_role_menu values ",
            "<foreach collection=\"menuIds\" item=\"item\" separator=\",\">(#{id},#{item})</foreach>",
            "</script>"})
    void bindMenu(@Param("id") String id, @Param("menuIds") List<String> menuIds);

    // 解绑用户
    @Delete("delete from sys_user_role where role_id = #{id}")
    void unBindUser(@Param("id") String id);
}
