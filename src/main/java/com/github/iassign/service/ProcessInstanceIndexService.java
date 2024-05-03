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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.ExistsIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.github.authorization.UserDetails;
import com.github.core.ApiException;
import com.github.core.DateUtil;
import com.github.core.JsonUtil;
import com.github.core.PageResult;
import com.github.iassign.dto.ProcessInstanceIndexDTO;
import com.github.iassign.dto.Tuple;
import com.github.iassign.entity.*;
import com.github.iassign.enums.TaskOperation;
import com.github.iassign.vo.ProcessInstanceIndexVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProcessInstanceIndexService {
    private final ElasticsearchClient esClient;
    // 索引模板
    private static final String TEMPLATE_NAME = "template_i_assign";
    // es 索引，按照日期模式创建索引
    private static final String INDEX_NAME = "process_instance";
    // 因为索引是按照日期模式去创建的，所以全部查询的时候，采用别名查询
    private static final String ALIAS_NAME = "i_assign_inst";
    private final UploadService uploadService;
    private final ProcessTaskService processTaskService;
    private final FormService formService;
    private final ProcessDefinitionService processDefinitionService;
    private final Redisson redisson;

    public ProcessInstanceIndexService(ElasticsearchClient esClient, UploadService uploadService,
                                       ProcessTaskService processTaskService, FormService formService,
                                       ProcessDefinitionService processDefinitionService, Redisson redisson) {
        this.esClient = esClient;
        this.uploadService = uploadService;
        this.processTaskService = processTaskService;
        this.processDefinitionService = processDefinitionService;
        this.formService = formService;
        this.redisson = redisson;
    }

    /**
     * （全新安装和部署本系统时才有意义）
     * 主要职责：系统启动时，去检查es索引，如果未对es索引进行设置，就做一些初始设置
     *
     * @throws Exception
     */
    @PostConstruct
    public void init() throws Exception {
        RLock lock = redisson.getLock("iassign:lock:process_index_create");
        try {
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (isLock) {
                // 判断索引模板是否存在
                ExistsIndexTemplateRequest idxTplReq = ExistsIndexTemplateRequest.of(f -> f.name(TEMPLATE_NAME));
                boolean exists = esClient.indices().existsIndexTemplate(idxTplReq).value();
                if (!exists) {
                    try (InputStream in = this.getClass().getResourceAsStream("/es_idx_template.json")) {
                        // 创建es索引模板
                        PutIndexTemplateRequest putIdxTplReq = PutIndexTemplateRequest.of(f -> f.name(TEMPLATE_NAME).withJson(in));
                        PutIndexTemplateResponse response = esClient.indices().putIndexTemplate(putIdxTplReq);
                        log.info("create index pattern:{}", response.acknowledged());
                    }
                } else {
                    log.info("index template is already exists");
                }
            }
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
        index.createTime = DateUtil.formatCn(processInstance.createTime);
        index.variables = contextVariables;
        index.status = processInstance.status.name();
        Pattern pattern = Pattern.compile("<([a-z]+)[\\S\\s]*?>(?<content>[\\S\\s]+)</\\1>");
        if (!CollectionUtils.isEmpty(contextVariables)) {
            StringBuilder content = new StringBuilder();
            contextVariables.forEach((key, value) -> {
                if (value != null) {
                    if (value instanceof String) {
                        String str = (String) value;
                        Matcher matcher = pattern.matcher(str);
                        // remove html tag
                        while (matcher.find()) {
                            String group = matcher.group(1);
                            str = str.replaceAll("<" + group + "[\\S\\s]*?>", "").replaceAll("</" + group + ">", ";");
                            matcher = pattern.matcher(str);
                        }
                        value = str;
                    } else if (value instanceof ArrayList) {
                        ArrayList list = (ArrayList) value;
                        if (!list.isEmpty()) {
                            List tmpList = new ArrayList();
                            list.forEach(item -> {
                                if (item instanceof LinkedHashMap) {
                                    LinkedHashMap<?, ?> map = (LinkedHashMap<?, ?>) item;
                                    if ("ok".equals(map.get("response"))) {
                                        tmpList.add(map.get("name") + "");
                                    }
                                } else {
                                    tmpList.add(item);
                                }
                            });
                            value = tmpList;
                        }
                    }
                    content.append(value).append(";");
                }
            });
            index.content = content.toString();
        }
        // 按照年份去存储索引
        String year = index.createTime.split("-")[0];
        esClient.index(i -> i.index(INDEX_NAME + "-" + year).id(index.id)
                .document(index));
    }

    /**
     * 更新申请表中的内容
     *
     * @param instance
     * @param contextVariables
     * @throws IOException
     */
    public void update(ProcessInstance instance, Map<String, Object> contextVariables) throws IOException {
        String year = DateUtil.format(instance.createTime, "yyyy");
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.variables = contextVariables;
        if (!CollectionUtils.isEmpty(contextVariables)) {
            StringBuilder content = new StringBuilder();
            contextVariables.forEach((key, value) -> {
                if (value != null) {
                    content.append(value).append(";");
                }
            });
            index.content = content.toString();
        }
        esClient.update(u -> u.index(INDEX_NAME + "-" + year)
                .id(instance.id).doc(index), ProcessInstanceIndex.class);
    }


    /**
     * 更新流程实例的状态
     *
     * @param instance
     */
    public void updateStatus(ProcessInstance instance) {
        ProcessInstanceIndex index = new ProcessInstanceIndex();
        index.status = instance.status.name();
        String year = DateUtil.format(instance.createTime, "yyyy");
        try {
            esClient.update(u -> u.index(INDEX_NAME + "-" + year)
                    .id(instance.id).doc(index), ProcessInstanceIndex.class);
        } catch (IOException e) {
            log.error("ES索引更新失败", e);
            throw new ApiException(500, "ES索引更新失败");
        }
    }

    public void delete(ProcessInstance instance) {
        String year = DateUtil.format(instance.createTime, "yyyy");
        DeleteResponse response;
        try {
            log.error("流程启动时异常，删除ES中的文档:{}", instance.id);
            response = esClient.delete(d -> d.index(INDEX_NAME + "-" + year)
                    .id(instance.id));
            log.info("流程启动时异常，删除ES中的文档:{},结果:{}", instance.id, response.result());
        } catch (IOException e) {
            log.error("ES删除文档失败，需要手动删除文档:" + instance.id, e);
        }
    }

    /**
     * 分页搜索
     *
     * @param lastId           上一次查询时，最后一条数据的id
     * @param lastId           上一次查询时，最后一条数据的评分
     * @param size             最大返回记录数
     * @param index            查询条件
     * @param highlight        是否高亮查询
     * @param excludeVariables 是否排除variables字段，默认是排除的(毕竟是动态字段而且不生成索引，只存储)
     */
    public PageResult<ProcessInstanceIndexDTO> pageQuery(String lastId, String score, Integer size,
                                                         ProcessInstanceIndexVO index, Boolean highlight,
                                                         Boolean excludeVariables) throws IOException {
        // 布尔多条件搜索
        Function<BoolQuery.Builder, ObjectBuilder<BoolQuery>> boolFn = (b) -> {
            // 指定查询流程定义
            if (StringUtils.hasText(index.definitionId)) {
                b.must(m -> m.term(t -> t.field("definitionId").value(index.definitionId)));
            }
            // 指定状态
            if (StringUtils.hasText(index.status)) {
                b.must(m -> m.term(t -> t.field("status").value(index.status)));
            }
            // 指定申请单号
            if (StringUtils.hasText(index.id)) {
                b.must(m -> m.term(t -> t.field("id").value(index.id)));
            }
            // 指定部门
            if (StringUtils.hasText(index.deptName)) {
                b.must(m -> m.term(t -> t.field("deptName").value(index.deptName)));
            }
            // 指定申请人
            if (StringUtils.hasText(index.starter)) {
                b.must(m -> m.term(t -> t.field("starter").value(index.starter)));
            }
            // 内容
            if (StringUtils.hasText(index.content)) {
                b.must(m -> m.match(ma -> ma.field("content").query(index.content)));
            }
            if (index.createTimeGe != null || index.createTimeLe != null) {
                Function<RangeQuery.Builder, ObjectBuilder<RangeQuery>> rangeFn = r -> {
                    RangeQuery.Builder createTimeQuery = r.field("createTime");
                    if (index.createTimeGe != null) {
                        createTimeQuery.from(DateUtil.formatCn(index.createTimeGe));
                    }
                    if (index.createTimeLe != null) {
                        createTimeQuery.to(DateUtil.formatCn(index.createTimeLe));
                    }
                    return createTimeQuery;
                };
                b.filter(f -> f.range(rangeFn));
            }
            // 构造动态查询 ，前提条件是es_idx_template的dynamic改为true，并且索引要根据流程定义id存储，一旦设置，不要修改
            // dynamicSearch();
            return b;
        };

        // 这里采用search after的方式能避免深度分页的问题，批量导出时数据大小最好限制在1万条以内
        int from = 0;
        // 判断搜索条件是否跨年了，未跨的话就直接指定年份去查询，加快查询速率。否则就用别名查询
        final String[] searchIndex = new String[]{ALIAS_NAME};
        String format = "yyyy";
        String createYearGe = DateUtil.format(index.createTimeGe, format);
        String createYearLe = DateUtil.format(index.createTimeLe, format);
        if (createYearGe.equals(createYearLe)) {
            searchIndex[0] = INDEX_NAME + "-" + createYearGe;
        }
        Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> searchFn = (s) -> {
            SearchRequest.Builder search = s.index(searchIndex[0])
                    // 分页
                    .from(from).size(size)
                    // 根据评分和id倒叙排序
                    .sort(sort -> sort.field(f -> f.field("_score").order(SortOrder.Desc)))
                    .sort(sort -> sort.field(f -> f.field("id").order(SortOrder.Desc)))
                    // 查询条件
                    .query(q -> q.bool(boolFn));
            if (!Boolean.FALSE.equals(excludeVariables)) {
                search.source(f -> f.filter(e -> e.excludes("variables")));
            }
            if (StringUtils.hasText(score)) {
                search.searchAfter(f -> f.doubleValue(Double.parseDouble(score)));
            }
            if (StringUtils.hasText(lastId)) {
                search.searchAfter(f -> f.stringValue(lastId));
            }
            if (highlight && StringUtils.hasText(index.content)) {
                // 高亮关键字
                search.highlight(h -> h.fields("content", f -> f.preTags("<span>").postTags("</span>")).encoder(HighlighterEncoder.Html));
            }
            return search;
        };

        SearchResponse<ProcessInstanceIndexDTO> result;
        try {
            result = esClient.search(searchFn, ProcessInstanceIndexDTO.class);
        } catch (ElasticsearchException ee) {
            int status = ee.response().status();
            if (status == 404) {
                return PageResult.empty();
            }
            throw ee;
        }

        HitsMetadata<ProcessInstanceIndexDTO> hits = result.hits();
        List<Hit<ProcessInstanceIndexDTO>> list = hits.hits();

        // 构造返回数据（系统采用es的search after分页方式，前端必须采用滚动分页，所以这里pageResult的页码和总数直接忽略即可）
        PageResult<ProcessInstanceIndexDTO> pageResult = new PageResult<>();
        pageResult.list = new ArrayList<>();
        list.forEach(i -> {
            ProcessInstanceIndexDTO dto = i.source();
            dto.score = String.valueOf(i.score());
            if (highlight && StringUtils.hasText(index.content)) {
                dto.highlight = i.highlight().get("content");
                dto.isHighlight = Boolean.TRUE;
            }
            pageResult.list.add(dto);
        });
        return pageResult;
    }

    /**
     * 生成excel，并返回excel的下载地址，
     * 导出的流程实例数最多仅限1万条，此外还会导出相关联的审批历史
     * 代码看起来简陋，但是为了应对大数据量导出，必须使用高效率的代码
     *
     * @param index
     * @return
     */
    public String generateExcel(ProcessInstanceIndexVO index) throws IOException {
        PageResult<ProcessInstanceIndexDTO> result = pageQuery(null, null, 10000, index, false, false);
        int length = result.list.size();
        File tmpFile = new File("/tmp/" + UUID.randomUUID() + ".xlsx");
        List<Tuple<String, String>> labels = new ArrayList<>();
        // 整理标签顺序，这个要动态扩展到excel的标题列中
        if (length > 0) {
            // 跟进流程定义ID取出关联的申请表
            String definitionId = index.definitionId;
            ProcessDefinition definition = processDefinitionService.selectById(definitionId);
            if (StringUtils.hasText(definition.formId)) {
                labels = formService.findLabels(definition.formId);
            }
        }
        int dynamicColLength = labels.size();
        try (Workbook workbook = new SXSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmpFile)) {
            // 创建第一页，保存流程实例和申请人提交的表单信息
            Sheet sheet = workbook.createSheet("申请记录");
            Row titileRow = sheet.createRow(0);
            titileRow.createCell(0).setCellValue("申请单号");
            titileRow.createCell(1).setCellValue("流程名称");
            titileRow.createCell(2).setCellValue("状态");
            titileRow.createCell(3).setCellValue("申请人ID");
            titileRow.createCell(4).setCellValue("申请人姓名");
            titileRow.createCell(5).setCellValue("申请部门");
            titileRow.createCell(6).setCellValue("申请日期");

            // 动态扩展标题列
            for (int i = 0; i < dynamicColLength; i++) {
                titileRow.createCell(i + 7).setCellValue(labels.get(i).v);
            }

            // 创建第二页，此页记录着审批历史
            Sheet auditSheet = workbook.createSheet("审批历史");
            Row auditTitleRow = auditSheet.createRow(0);
            auditTitleRow.createCell(0).setCellValue("申请单号");
            auditTitleRow.createCell(1).setCellValue("任务编号");
            auditTitleRow.createCell(2).setCellValue("审批环节");
            auditTitleRow.createCell(3).setCellValue("审批时间");
            auditTitleRow.createCell(4).setCellValue("审批人ID");
            auditTitleRow.createCell(5).setCellValue("审批人姓名");
            auditTitleRow.createCell(6).setCellValue("审批意见");
            auditTitleRow.createCell(7).setCellValue("指派");
            auditTitleRow.createCell(8).setCellValue("审批状态");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            AtomicInteger opinionRowIndex = new AtomicInteger(1); // 审批历史行数
            for (int i = 0; i < length; i++) {
                // 流程实例，单独一个sheet
                ProcessInstanceIndex instanceIndex = result.list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(instanceIndex.id);
                row.createCell(1).setCellValue(instanceIndex.name);
                row.createCell(2).setCellValue(instanceIndex.status);
                row.createCell(3).setCellValue(instanceIndex.starter);
                row.createCell(4).setCellValue(instanceIndex.starterName);
                row.createCell(5).setCellValue(instanceIndex.deptName);
                row.createCell(6).setCellValue(instanceIndex.createTime);

                // 动态填充数据列
                for (int j = 0; j < dynamicColLength; j++) {
                    String field = labels.get(j).k;
                    Object value = instanceIndex.variables.get(field);
                    Cell cell = row.createCell(j + 7);
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof List) {
                        String val = ((List<Object>) value).stream().filter(Objects::nonNull).map(Object::toString)
                                .collect(Collectors.joining(","));
                        cell.setCellValue(val);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                    instanceIndex.variables.remove(field);
                }
                // 剩余的没有label的变量，整合到一列
                if (instanceIndex.variables.size() > 0) {
                    Cell cell = row.createCell(dynamicColLength + 7);
                    cell.setCellValue(JsonUtil.toJson(instanceIndex.variables));
                }

                // 审批历史，另外一个sheet
                List<ProcessTask> processTasks = processTaskService.auditListAfter(instanceIndex.id, null);
                processTasks.forEach(task -> {
                    Row auditRow = auditSheet.createRow(opinionRowIndex.getAndIncrement());
                    auditRow.createCell(0).setCellValue(task.instanceId);
                    auditRow.createCell(1).setCellValue(task.id);
                    auditRow.createCell(2).setCellValue(task.name);
                    auditRow.createCell(3).setCellValue(sdf.format(task.createTime));
                    // 审批意见
                    if (task.opinions != null) {
                        // 第一条审批意见紧跟着审批任务节点
                        Iterator<ProcessOpinion> opinionIt = task.opinions.iterator();
                        ProcessOpinion firstOpinion = opinionIt.next();
                        auditRow.createCell(3).setCellValue(sdf.format(firstOpinion.createTime));
                        auditRow.createCell(4).setCellValue(firstOpinion.userId);
                        auditRow.createCell(5).setCellValue(firstOpinion.username);
                        auditRow.createCell(6).setCellValue(firstOpinion.remark);
                        String assignDetails = "";
                        if (firstOpinion.operation == TaskOperation.ASSIGN) {
                            assignDetails = "[" + firstOpinion.userId + "]" + firstOpinion.username + " 指派 [" + firstOpinion.assignId + "]"
                                    + firstOpinion.assignName;
                        }
                        auditRow.createCell(7).setCellValue(assignDetails);
                        auditRow.createCell(8).setCellValue(firstOpinion.operation.name());
                        // 其他审批意见各自独占一行
                        while (opinionIt.hasNext()) {
                            ProcessOpinion opinion = opinionIt.next();
                            Row opinionRow = auditSheet.createRow(opinionRowIndex.getAndIncrement());
                            opinionRow.createCell(3).setCellValue(sdf.format(opinion.createTime));
                            opinionRow.createCell(4).setCellValue(opinion.userId);
                            opinionRow.createCell(5).setCellValue(opinion.username);
                            opinionRow.createCell(6).setCellValue(opinion.remark);
                            assignDetails = "";
                            if (opinion.operation == TaskOperation.ASSIGN) {
                                assignDetails = "[" + opinion.userId + "]" + opinion.username + " 指派 [" + opinion.assignId + "]"
                                        + opinion.assignName;
                            }
                            opinionRow.createCell(7).setCellValue(assignDetails);
                            opinionRow.createCell(8).setCellValue(opinion.operation.name());
                        }
                    }
                });
            }
            workbook.write(fos);
        }
        String url = uploadService.saveToTmp(tmpFile);
        tmpFile.delete();
        return url;
    }



    /*private void dynamicSearch() {
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
    }*/
}
