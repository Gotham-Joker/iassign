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


    /**
     * 删除未被引用的流程图
     *
     * @param definitionId
     * @param reserveId   需要保留的id
     */
    @Transactional
    public void removeUnused(String definitionId, String reserveId) {
        Set<String> idSet = processDefinitionRuMapper.selectUnUsed(definitionId);
        for (String id : idSet) {
            if (id.equals(reserveId)) {
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
