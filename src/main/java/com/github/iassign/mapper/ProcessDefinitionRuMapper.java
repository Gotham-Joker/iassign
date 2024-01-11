package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.entity.ProcessDefinitionRu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.Set;

@Repository
public interface ProcessDefinitionRuMapper extends BaseMapper<ProcessDefinitionRu> {
    @Select({"select ru.id from t_process_definition_ru ru where ru.definition_id=#{defId} and not exists ",
    "(select 1 from t_process_instance t where t.definition_id=#{defId} and t.ru_id=ru.id)"})
    Set<String> selectUnUsed(@Param("defId") String definitionId);

}
