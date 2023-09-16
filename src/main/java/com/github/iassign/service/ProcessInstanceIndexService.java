package com.github.iassign.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.util.ObjectBuilder;
import com.github.authorization.UserDetails;
import com.github.core.JsonUtil;
import com.github.core.PageResult;
import com.github.iassign.entity.ProcessInstance;
import com.github.iassign.entity.ProcessInstanceIndex;
import com.github.iassign.entity.ProcessTask;
import com.github.iassign.vo.ProcessInstanceIndexVO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


@Service
public class ProcessInstanceIndexService {
    private final ElasticsearchClient esClient;
    private final String INDEX_NAME = "process_instance";
    private final UploadService uploadService;
    private final ProcessTaskService processTaskService;

    public ProcessInstanceIndexService(RestClientBuilder restClientBuilder, UploadService uploadService,
                                       ProcessTaskService processTaskService) {
        RestClient restClient = restClientBuilder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        this.esClient = new ElasticsearchClient(transport);
        this.uploadService = uploadService;
        this.processTaskService = processTaskService;
    }

    // 插入索引
    public void save(ProcessInstance processInstance, UserDetails userDetails, Map<String, Object> contextVariables) throws IOException {
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.id = processInstance.id;
        index.name = processInstance.name;
        index.definitionId = processInstance.definitionId;
        index.starter = processInstance.starter;
        index.starterName = userDetails.username;
        index.deptName = userDetails.deptName;
        index.deptId = userDetails.deptId;
        index.createTime = processInstance.createTime;
        index.variables = contextVariables;
        index.status = processInstance.status.name();
        if (!CollectionUtils.isEmpty(contextVariables)) {
            StringBuilder content = new StringBuilder();
            contextVariables.forEach((key, value) -> {
                if (value != null) {
                    content.append(value).append(";");
                }
            });
            index.content = content.toString();
        }
        esClient.index(i -> i.index(INDEX_NAME).id(index.id)
                .document(index));
    }

    /**
     * 分页搜索
     */
    public PageResult<ProcessInstanceIndex> pageQuery(Integer page, Integer size, ProcessInstanceIndexVO index) throws IOException {
        // 布尔多条件搜索
        Function<BoolQuery.Builder, ObjectBuilder<BoolQuery>> boolFn = (b) -> {
            // 指定查询流程定义
            if (StringUtils.hasText(index.definitionId)) {
                b.must(m -> m.term(t -> t.field("definitionId.keyword").value(index.definitionId)));
            }
            // 指定状态
            if (StringUtils.hasText(index.status)) {
                b.must(m -> m.term(t -> t.field("status.keyword").value(index.status)));
            }
            // 指定申请单号
            if (StringUtils.hasText(index.id)) {
                b.must(m -> m.term(t -> t.field("id.keyword").value(index.id)));
            }
            // 指定部门
            if (StringUtils.hasText(index.deptName)) {
                b.must(m -> m.term(t -> t.field("deptName.keyword").value(index.deptName)));
            }
            // 指定申请人
            if (StringUtils.hasText(index.starter)) {
                b.must(m -> m.term(t -> t.field("starter.keyword").value(index.starter)));
            }
            // 内容
            if (StringUtils.hasText(index.content)) {
                b.must(m -> m.match(ma -> ma.field("content").query(index.content)));
            }
            if (index.createTimeGe != null || index.createTimeLe != null) {
                Function<RangeQuery.Builder, ObjectBuilder<RangeQuery>> rangeFn = r -> {
                    RangeQuery.Builder createTimeQuery = r.field("createTime");
                    if (index.createTimeGe != null) {
                        createTimeQuery.from(JsonData.of(index.createTimeGe, esClient._jsonpMapper()).toJson().toString());
                    }
                    if (index.createTimeLe != null) {
                        createTimeQuery.to(JsonData.of(index.createTimeLe, esClient._jsonpMapper()).toJson().toString());
                    }
                    return createTimeQuery;
                };
                b.filter(f -> f.range(rangeFn));
            }
            // 构造动态查询
            if (index.variables != null && !index.variables.isEmpty()) {
                index.variables.forEach((key, value) -> {
                    // 空的参数不参与查询
                    if (value == null || !StringUtils.hasText(value.toString())) {
                        return;
                    }
                    String[] strArr = key.split("_");
                    String field = strArr[0];
                    if (strArr.length == 2) {
                        switch (strArr[1]) {
                            // 区间查询
                            case "ge":
                                Function<RangeQuery.Builder, ObjectBuilder<RangeQuery>> rangeFnGe = r -> {
                                    RangeQuery.Builder createTimeQuery = r.field(field);
                                    createTimeQuery.gte(JsonData.of(value));
                                    Object valueLe = index.variables.get(field + "_le");
                                    if (valueLe != null) {
                                        createTimeQuery.lte(JsonData.of(valueLe));
                                    }
                                    return createTimeQuery;
                                };
                                b.filter(f -> f.range(rangeFnGe));
                                break;
                            case "le":
                                Function<RangeQuery.Builder, ObjectBuilder<RangeQuery>> rangeFnLe = r -> {
                                    RangeQuery.Builder createTimeQuery = r.field(field);
                                    createTimeQuery.lte(JsonData.of(value));
                                    Object valueGe = index.variables.get(field + "_ge");
                                    if (valueGe != null) {
                                        createTimeQuery.lte(JsonData.of(valueGe));
                                    }
                                    return createTimeQuery;
                                };
                                b.filter(f -> f.range(rangeFnLe));
                                break;
                            // 模糊查询
                            case "like":
                                b.must(m -> m.match(ma -> ma.field("variables." + field).query(value.toString())));
                                break;
                            // 等于
                            case "eq":
                            default:
                                if (value instanceof String) {
                                    b.must(m -> m.term(t -> t.field(strArr + ".keyword").value((String) value)));
                                } else if (value instanceof Long) {
                                    b.must(m -> m.term(t -> t.field(strArr + ".keyword").value((Long) value)));
                                } else if (value instanceof Integer) {
                                    b.must(m -> m.term(t -> t.field(strArr + ".keyword").value(Long.valueOf((Integer) value))));
                                } else if (value instanceof Double) {
                                    b.must(m -> m.term(t -> t.field(strArr + ".keyword").value((Double) value)));
                                } else if (value instanceof Float) {
                                    b.must(m -> m.term(t -> t.field(strArr + ".keyword").value(Double.valueOf((Float) value))));
                                }
                                break;
                        }
                    }
                });
            }
            return b;
        };

        // 根据页码计算游标起始位置
        int from = (page - 1) * size;

        SearchResponse<ProcessInstanceIndex> result;
        // 固定查询某个流程定义
        try {
            result = esClient.search(s -> s.index(INDEX_NAME)
                            // 分页
                            .from(from).size(size)
                            // 根据id倒叙排序
                            .sort(sort -> sort.field(f -> f.field("id.keyword").order(SortOrder.Desc)))
                            // 查询条件
                            .query(q -> q.bool(boolFn))
                    , ProcessInstanceIndex.class);
        } catch (ElasticsearchException ee) {
            int status = ee.response().status();
            if (status == 404) {
                return PageResult.empty();
            }
            throw ee;
        }

        HitsMetadata<ProcessInstanceIndex> hits = result.hits();
        long total = hits.total().value();
        List<Hit<ProcessInstanceIndex>> list = hits.hits();

        // 构造分页数据
        PageResult<ProcessInstanceIndex> pageResult = new PageResult<>();
        pageResult.list = new ArrayList<>();
        list.forEach(i -> pageResult.list.add(i.source()));
        pageResult.page = page;
        pageResult.size = size;
        pageResult.total = total;
        int pages = Long.valueOf(total / size).intValue();
        pageResult.totalPages = total % size == 0 ? pages : (pages + 1);
        return pageResult;
    }

    /**
     * 生成excel，并返回excel的下载地址，
     * 导出的流程实例数最多仅限1万条，此外还会导出相关联的审批历史
     *
     * @param index
     * @return
     */
    public String generateExcel(ProcessInstanceIndexVO index) throws IOException {
        PageResult<ProcessInstanceIndex> result = pageQuery(1, 10000, index);
        int length = result.list.size();
        File tmpFile = new File("/tmp/" + UUID.randomUUID() + ".xlsx");
        try (Workbook workbook = new SXSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmpFile)) {
            Sheet sheet = workbook.createSheet("申请记录");
            Row titileRow = sheet.createRow(0);
            titileRow.createCell(0).setCellValue("申请单号");
            titileRow.createCell(1).setCellValue("流程名称");
            titileRow.createCell(2).setCellValue("状态");
            titileRow.createCell(3).setCellValue("申请人ID");
            titileRow.createCell(4).setCellValue("申请人姓名");
            titileRow.createCell(5).setCellValue("申请部门");
            titileRow.createCell(6).setCellValue("申请日期");
            titileRow.createCell(7).setCellValue("申请内容");

            Sheet auditSheet = workbook.createSheet("审批历史");
            Row auditTitleRow = auditSheet.createRow(0);
            auditTitleRow.createCell(0).setCellValue("申请单号");
            auditTitleRow.createCell(1).setCellValue("任务编号");
            auditTitleRow.createCell(2).setCellValue("任务环节");
            auditTitleRow.createCell(3).setCellValue("创建时间");
            auditTitleRow.createCell(4).setCellValue("受理人ID");
            auditTitleRow.createCell(5).setCellValue("受理人姓名");
            auditTitleRow.createCell(6).setCellValue("受理意见");
            auditTitleRow.createCell(7).setCellValue("指派人ID");
            auditTitleRow.createCell(8).setCellValue("指派人姓名");
            auditTitleRow.createCell(9).setCellValue("指派人意见");
            auditTitleRow.createCell(10).setCellValue("审批状态");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            AtomicInteger j = new AtomicInteger(1); // 审批历史行数
            for (int i = 0; i < length; i++) {
                ProcessInstanceIndex instanceIndex = result.list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(instanceIndex.id);
                row.createCell(1).setCellValue(instanceIndex.name);
                row.createCell(2).setCellValue(instanceIndex.status);
                row.createCell(3).setCellValue(instanceIndex.starter);
                row.createCell(4).setCellValue(instanceIndex.starterName);
                row.createCell(5).setCellValue(instanceIndex.deptName);
                row.createCell(6).setCellValue(sdf.format(instanceIndex.createTime));
                row.createCell(7).setCellValue(JsonUtil.toJson(instanceIndex.variables));
                // 查找审批历史
                List<ProcessTask> processTasks = processTaskService.auditListAfter(instanceIndex.id, "0");
                processTasks.forEach(task -> {
                    Row auditRow = auditSheet.createRow(j.getAndIncrement());
                    auditRow.createCell(0).setCellValue(task.instanceId);
                    auditRow.createCell(1).setCellValue(task.id);
                    auditRow.createCell(2).setCellValue(task.name);
                    auditRow.createCell(3).setCellValue(sdf.format(task.createTime));
                    auditRow.createCell(4).setCellValue(task.handlerId);
                    auditRow.createCell(5).setCellValue(task.handlerName);
                    auditRow.createCell(6).setCellValue(task.remark);
                    auditRow.createCell(7).setCellValue(task.assignId);
                    auditRow.createCell(8).setCellValue(task.assignName);
                    auditRow.createCell(9).setCellValue(task.assignRemark);
                    auditRow.createCell(10).setCellValue(task.status.name());
                });
            }
            workbook.write(fos);
        }
        String url = uploadService.saveToTmp(tmpFile);
        tmpFile.delete();
        return url;
    }

    /**
     * 更新流程实例的状态
     *
     * @param instance
     */
    public void updateStatus(ProcessInstance instance) {
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.status = instance.status.name();
        try {
            esClient.update(u -> u.index(INDEX_NAME).id(instance.id).doc(index), ProcessInstanceIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
