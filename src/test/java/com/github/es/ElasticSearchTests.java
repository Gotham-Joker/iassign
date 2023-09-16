package com.github.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.github.SpringApplicationTests;
import com.github.core.JsonUtil;
import com.github.iassign.entity.ProcessInstanceIndex;
import com.github.iassign.entity.FormInstance;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.iassign.mapper.ProcessInstanceMapper;
import jakarta.json.stream.JsonParser;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class ElasticSearchTests extends SpringApplicationTests {
    protected ElasticsearchClient esClient;
    @Autowired
    RestClientBuilder restClientBuilder;
    @Autowired
    ProcessDefinitionMapper processDefinitionMapper;
    @Autowired
    ProcessInstanceMapper processInstanceMapper;
    @Autowired
    FormInstanceMapper formInstanceMapper;

    @BeforeEach
    public void init() {
        RestClient restClient = restClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);
    }

    /**
     * 创建索引映射
     */
    @Test
    public void testCreateIndexMapping() throws IOException {
        String processInstanceMappings = "{\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"definitionId\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"name\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"starter\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"deptName\": {\n" +
                "      \"type\": \"keyword\"\n" +
                "    },\n" +
                "    \"createTime\": {\n" +
                "      \"type\": \"long\"\n" +
                "    },\n" +
                "    \"content\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"analyzer\": \"ik_max_word\",\n" +
                "      \"search_analyzer\": \"ik_smart\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonpMapper jsonpMapper = esClient._jsonpMapper();
        try (StringReader sr = new StringReader(processInstanceMappings);
             JsonParser parser = jsonpMapper.jsonProvider().createParser(sr)) {
            esClient.indices().create(r -> r.index("process_instance")
                    .mappings(TypeMapping._DESERIALIZER.deserialize(parser, jsonpMapper))
            );
        }

    }

    /**
     * 插入一条流程实例ES索引
     */
    @Test
    public void testInsertIndex() throws IOException {
        // 模拟定时扫描
        // 扫描到其中一条申请
        ProcessInstance processInstance = processInstanceMapper.selectById("1690218632980283393");
        // 取出申请表
        FormInstance formInstance = formInstanceMapper.selectById(processInstance.formInstanceId);
        String variables = formInstance.variables;
        Map<String, Object> variablesMap = JsonUtil.readValue(variables, Map.class);
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.id = processInstance.id;
        index.name = processInstance.name;
        index.definitionId = processInstance.definitionId;
        index.starter = processInstance.starter;
        index.deptName = "金融科技部";
        index.createTime = processInstance.createTime;
        index.variables = variablesMap;
        if (!CollectionUtils.isEmpty(variablesMap)) {
            StringBuilder content = new StringBuilder();
            variablesMap.entrySet().forEach(entry -> {
                if (entry.getValue() != null) {
                    content.append(entry.getValue()).append(";");
                }
            });
            index.content = content.toString();
        }
        esClient.index(i -> i.index("process_instance").id(index.id)
                .document(index));
    }

    @Test
    public void testSearch() throws IOException {
        SearchResponse<ProcessInstanceIndex> response = esClient.search(s -> s
                        .index("process_instance")
                        .query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("deptName").value("金融科技部")))
                                        .must(m -> m.match(ma -> ma.field("content").query("公司贷款")))
                                        .filter(f -> f.range(r -> r.field("createTime").gte(JsonData.of(1680440901000L)).lte(JsonData.of(1680440902000L))))
                                )
                        )
                , ProcessInstanceIndex.class);
        response.hits().hits().forEach(hit -> {
            ProcessInstanceIndex source = hit.source();
            assert source != null;
            System.out.println(source);
        });
    }

    @Test
    public void testDelete() throws IOException {
        DeleteResponse response = esClient.delete(d -> d.index("process_instance")
                .id("1642514302277922817"));
        System.out.println(response.result());
    }

    @Test
    public void testUpdate() {
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.status = "SUCCESS";
        try {
            esClient.update(u -> u.index("process_instance").id("1694686632008433665")
                            .doc(index)
                    , ProcessInstanceIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
