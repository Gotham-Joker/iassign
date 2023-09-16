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