package com.github.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.github.SpringApplicationTests;
import com.github.core.JsonUtil;
import com.github.iassign.dto.ProcessInstanceIndexDTO;
import com.github.iassign.entity.ProcessInstanceIndex;
import com.github.iassign.entity.FormInstance;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.mapper.FormInstanceMapper;
import com.github.iassign.mapper.ProcessDefinitionMapper;
import com.github.iassign.mapper.ProcessInstanceMapper;
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
    public void testUpdateIndexMapping() throws IOException {
        // 索引不存在的话，创建一个索引，并把content字段设置为ik分词器
        if (!esClient.indices().exists(e -> e.index("process_instance")).value()) {
            CreateIndexResponse createIndexResponse = esClient.indices().create(r -> r.index("process_instance")
                    .mappings(m -> m.properties("content", f -> f.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))));
        }

    }

    /**
     * 插入一条流程实例ES索引
     */
    @Test
    public void testInsertIndex() throws IOException {
        // 模拟定时扫描
        // 扫描到其中一条申请
        ProcessInstance processInstance = processInstanceMapper.selectOne(null);
        // 取出申请表
        FormInstance formInstance = formInstanceMapper.selectById(processInstance.formInstanceId);
        String variables = formInstance.variables;
        Map<String, Object> variablesMap = JsonUtil.readValue(variables, Map.class);
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.id = processInstance.id;
        index.name = processInstance.name;
        index.definitionId = processInstance.definitionId;
        index.starter = processInstance.starter;
        index.starterName = processInstance.starterName;
        index.deptName = "科技部";
        index.deptId = "1";
        index.createTime = processInstance.createTime;
        index.status = processInstance.status.name();
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
        SearchResponse<ProcessInstanceIndexDTO> response = esClient.search(s -> s
                        .index("process_instance")
                        .query(q -> q.bool(b -> b
                                        .must(m -> m.match(ma -> ma.field("content").query("测试margin")))
                                )
                        ).highlight(h -> h.preTags("<em>").postTags("</em>")
                                .fields("content",f->f.boundaryChars(";")).encoder(HighlighterEncoder.Html))
                , ProcessInstanceIndexDTO.class);
        response.hits().hits().forEach(h->{
            System.out.println(h.source().getContent());
            System.out.println(h.highlight());
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
