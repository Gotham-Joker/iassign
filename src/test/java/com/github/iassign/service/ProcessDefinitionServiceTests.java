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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.DatasourceTests;
import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.core.JsonUtil;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.*;


/**
 * 测试流程定义
 */
class ProcessDefinitionServiceTests extends DatasourceTests {
    @Spy
    ProcessDefinitionMapper processDefinitionMapper;
    @InjectMocks
    ProcessDefinitionService processDefinitionService;

    @Test
    public void testSave() throws Exception {
        this.processDefinitionMapper = getMapper(ProcessDefinitionMapper.class);
        MockitoAnnotations.openMocks(this);

        ProcessDefinition processDefinition = new ProcessDefinition();

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dag1.json")) {
            byte[] buffer = new byte[8192];
            int len;
            StringBuilder sb = new StringBuilder();
            while ((len = in.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
            processDefinition.dag = JsonUtil.toJson(JsonUtil.readValue(sb.toString(), JsonNode.class));

        }
        processDefinition.name = "客户电邮服务";
        processDefinition.status = true;
        processDefinition.formId = null;
        processDefinitionService.save(processDefinition);
        System.out.println(processDefinition.id);
    }
}