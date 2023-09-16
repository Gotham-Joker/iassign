package com.github.iassign.service;

import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.mapper.ProcessDefinitionRuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class ProcessDefinitionRuService {
    @Autowired
    private ProcessDefinitionRuMapper processDefinitionRuMapper;

    @Transactional
    public ProcessDefinitionRu saveIfAbsent(ProcessDefinition definition) {
        String md5 = DigestUtils.md5DigestAsHex(definition.dag.getBytes(StandardCharsets.UTF_8));
        ProcessDefinitionRu processDefinitionRu = processDefinitionRuMapper.selectById(md5);
        if (processDefinitionRu == null) {
            processDefinitionRu = new ProcessDefinitionRu();
            processDefinitionRu.id = md5;
            processDefinitionRu.dag = definition.dag;
            processDefinitionRu.createTime = new Date();
            processDefinitionRu.definitionId = definition.id;
            processDefinitionRuMapper.insert(processDefinitionRu);
        }
        return processDefinitionRu;
    }

    public ProcessDefinitionRu selectById(String id) {
        return processDefinitionRuMapper.selectById(id);
    }

    public void deleteByDefId(Serializable id) {
        processDefinitionRuMapper.deleteByDefId(id);
    }
}
