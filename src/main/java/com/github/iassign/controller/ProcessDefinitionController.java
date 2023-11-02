package com.github.iassign.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.core.GlobalIdGenerator;
import com.github.core.JsonUtil;
import com.github.iassign.dto.ProcessDeployDTO;
import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.service.ProcessDefinitionRuService;
import com.github.iassign.service.ProcessDefinitionService;
import com.github.authorization.AuthenticationContext;
import com.github.base.BaseController;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/process-definition")
public class ProcessDefinitionController extends BaseController<ProcessDefinition> {
    @Autowired
    private ProcessDefinitionService processDefinitionService;
    @Autowired
    private ProcessDefinitionRuService processDefinitionRuService;
    @Autowired
    private GlobalIdGenerator generator;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @PostMapping
    public Result save(@Validated @RequestBody ProcessDefinition entity) {
        entity.creator = AuthenticationContext.current().getId();
        entity.createTime = new Date();
        entity.status = false;
        entity.id = generator.nextIdStr();
        ProcessDefinitionRu ru = processDefinitionRuService.saveIfAbsent(entity);
        entity.ruId = ru.id;
        processDefinitionService.save(entity);
        return Result.success(entity.id);
    }

    @Override
    @DeleteMapping
    public Result delete(@RequestParam Serializable id) {
        // 删除与之关联的流程图
        processDefinitionRuService.deleteByDefId(id);
        // 删除与之关联的部署关系
        processDefinitionService.deleteAuth(id);
        return super.delete(id);
    }

    /**
     * 查看用户有权限查看的流程定义清单
     * 查询参数暂时只支持流程名、流程描述
     *
     * @return
     */
    @GetMapping("list/current-user")
    public Result currentUserDefinitions(@RequestParam String keyword) {
        String deptId = AuthenticationContext.details().deptId;
        List<String> deptIds = Stream.of(deptId, "ALL").collect(Collectors.toList());
        Map<String, List<ProcessDefinition>> group = processDefinitionService.selectUsersDefinitions(keyword, deptIds).stream()
                .collect(Collectors.groupingBy(ProcessDefinition::getGroupName));
        return Result.success(group);
    }

    @PutMapping
    @Override
    public Result update(@Validated @RequestBody ProcessDefinition entity) {
        processDefinitionService.update(entity);
        return Result.success(entity.id);
    }

    /**
     * 部署流程定义 (部门 部署状态)
     *
     * @return
     */
    @PutMapping("deploy")
    public Result deploy(@RequestBody ProcessDeployDTO dto) {
        processDefinitionService.deploy(dto);
        return Result.success();
    }

    /**
     * 查看流程图
     *
     * @param id
     * @return
     */
    @GetMapping("dag")
    public Result dag(@RequestParam String id) {
        ProcessDefinition definition = processDefinitionService.selectById(id);
        if (definition == null) {
            return Result.error(404, "流程定义不存在");
        }
        ProcessDefinitionRu definitionRu = processDefinitionRuService.selectById(definition.ruId);
        if (definitionRu == null) {
            return Result.error(404, "流程图不存在");
        }
        return Result.success(definitionRu.dag);
    }

    /**
     * 查询流程详情，包含部署信息
     *
     * @return
     */
    @GetMapping("detail")
    public Result findDefinitionDetail(@RequestParam String id) {
        return Result.success(processDefinitionService.findDefinitionDetail(id));
    }

    /**
     * 导出流程
     */
    @GetMapping("out")
    public ResponseEntity<byte[]> exportDefinition(@RequestParam String id) {
        // 找到流程定义
        ProcessDefinition definition = processDefinitionService.selectById(id);
        // 取出流程图
        ProcessDefinitionRu ru = processDefinitionRuService.selectById(definition.ruId);
        Object[] objects = {definition, ru};
        return Result.octetStream(id + "_p.json", JsonUtil.toJson(objects).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 导入流程
     */
    @PostMapping("in")
    public Result importDefinition(@RequestParam MultipartFile file) throws IOException {
        ProcessDefinition definition;
        ProcessDefinitionRu ru;
        try (InputStream in = file.getInputStream()) {
            List<Map> list = objectMapper.readValue(in, new TypeReference<List<Map>>() {
            });
            definition = objectMapper.convertValue(list.get(0), ProcessDefinition.class);
            ru = objectMapper.convertValue(list.get(1), ProcessDefinitionRu.class);
        }
        processDefinitionService.importDefinition(definition, ru);
        return Result.success();
    }
}
