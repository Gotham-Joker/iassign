package com.github.iassign.service;

import com.github.core.ApiException;
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
import java.util.Iterator;
import java.util.Set;

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


    /**
     * 删除未被引用的流程图
     *
     * @param definitionId
     * @param perceiveId   需要保留的id
     */
    @Transactional
    public void removeUnused(String definitionId, String perceiveId) {
        Set<String> idSet = processDefinitionRuMapper.selectUnUsed(definitionId);
        for (String id : idSet) {
            if (id.equals(perceiveId)) {
                idSet.remove(id);
                break;
            }
        }
        if (!idSet.isEmpty()) {
            processDefinitionRuMapper.deleteBatchIds(idSet);
        }
    }

    public void insert(ProcessDefinitionRu ru) {
        if (ru.id == null || ru.definitionId == null) {
            throw new ApiException(500, "流程图ID或者流程图绑定的流程定义ID必须事先提供");
        }
        processDefinitionRuMapper.insert(ru);
    }

    public void updateById(ProcessDefinitionRu ru) {
        processDefinitionRuMapper.updateById(ru);
    }
}
