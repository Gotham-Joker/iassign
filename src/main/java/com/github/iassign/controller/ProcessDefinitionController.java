package com.github.iassign.controller;

import com.github.iassign.dto.ProcessDeployDTO;
import com.github.iassign.entity.ProcessDefinition;
import com.github.iassign.entity.ProcessDefinitionRu;
import com.github.iassign.service.ProcessDefinitionRuService;
import com.github.iassign.service.ProcessDefinitionService;
import com.github.authorization.AuthenticationContext;
import com.github.base.BaseController;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
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

    @Override
    @PostMapping
    public Result save(@Validated @RequestBody ProcessDefinition entity) {
        entity.creator = AuthenticationContext.current().getId();
        entity.createTime = new Date();
        entity.status = false;
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
        entity.updateTime = new Date();
        ProcessDefinitionRu ru = processDefinitionRuService.saveIfAbsent(entity);
        entity.ruId = ru.id;
        processDefinitionService.updateById(entity);
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
     * 查询已部署的部门
     *
     * @return
     */
    @GetMapping("detail/permission")
    public Result permission(@RequestParam String id) {
        return Result.success(processDefinitionService.findPermission(id));
    }
}
