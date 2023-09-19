package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.github.iassign.vo.MenuTree;
import com.github.iassign.vo.SysMenuQuery;
import com.github.iassign.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    /**
     * 分页查询
     */
    @Select({"SELECT * FROM sys_menu ",
            "<where> ",
            "    <if test=\"query.id!=null\">id = #{query.id}</if> ",
            "    <if test=\"query.text!=null and query.text!=''\">and text like CONCAT(#{query.text},'%')</if> ",
            "</where>"})
    Page<SysMenu> selectByPage(@Param("query") SysMenuQuery query);


    @Delete("delete from sys_role_menu where menu_id = #{id}")
    void unBindRole(@Param("id") Serializable id);

    @Select("select * from sys_menu where pid = #{pid}")
    List<SysMenu> selectByPid(@Param("pid") Serializable pid);

    @Select({"select m.* from sys_menu m where exists ",
            "(select 1 from sys_user_role sur inner join sys_role_menu srm on sur.role_id=srm.role_id ",
            " where sur.user_id=#{userId} and m.id=srm.menu_id )",
            " union select * from sys_menu where all_available=1"})
    List<MenuTree> selectByUserId(@Param("userId") String userId);

    @Select({"select * from sys_menu m where exists ",
            "(select 1 from sys_role_menu srm where srm.role_id=#{roleId} and srm.menu_id=m.id)"})
    List<SysMenu> selectByRoleId(@Param("roleId") String roleId);

}