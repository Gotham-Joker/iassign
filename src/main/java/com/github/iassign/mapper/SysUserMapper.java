package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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

}
