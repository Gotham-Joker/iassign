package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.entity.ProcessDefinition;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
    @Delete("delete from t_process_definition_auth where definition_id=#{definitionId}")
    void unbind(@Param("definitionId") String definitionId);

    @Insert({"<script>",
            "insert into t_process_definition_auth values ",
            "<foreach collection='deptIds' item='item' separator=','>(#{definitionId},#{item})</foreach>",
            "</script>"})
    void bind(@Param("definitionId") String definitionId, @Param("deptIds") List<String> deptIds);

    @Select("select dept_id from t_process_definition_auth where definition_id=#{definitionId}")
    List<String> selectDeployDepartments(@Param("definitionId") String id);

    @Select({"<script>",
            "select d.id,d.name,d.description,d.form_id,d.ru_id,d.group_name,d.seq_no,d.icon from t_process_definition d ",
            "<where> ",
            " exists (select 1 from t_process_definition_auth dd where dd.definition_id=d.id ",
            " and dd.dept_id in ",
            "(<foreach collection='deptIds' item='item' separator=','>#{item}</foreach>)",
            " and d.status=1)",
            "<if test='keyword!=null and keyword!=\"\"'> and (",
            "d.seq_no like CONCAT('%',#{keyword},'%') or d.name like CONCAT('%',#{keyword},'%') or d.group_name like CONCAT('%',#{keyword},'%')",
            " or d.description like CONCAT('%',#{keyword},'%')",
            ")</if>",
            "</where>",
            "</script>"})
    List<ProcessDefinition> selectUsersDefinitions(@Param("keyword") String keyword, @Param("deptIds") List<String> deptId);

    @Delete("delete from t_process_definition_auth where definition_id=#{definitionId}")
    void deleteAuth(@Param("definitionId") Serializable id);

}
