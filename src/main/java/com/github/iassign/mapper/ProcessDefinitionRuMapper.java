package com.github.iassign.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.iassign.entity.ProcessDefinitionRu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface ProcessDefinitionRuMapper extends BaseMapper<ProcessDefinitionRu> {
    @Delete("delete from t_process_definition_ru where definition_id=#{defId}")
    void deleteByDefId(@Param("defId") Serializable id);
}
