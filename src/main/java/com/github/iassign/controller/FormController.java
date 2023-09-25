package com.github.iassign.controller;

import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.JsonUtil;
import com.github.core.Result;
import com.github.iassign.dto.FormDTO;
import com.github.iassign.entity.FormDefinition;
import com.github.iassign.service.FormService;
import com.github.iassign.util.PlaceHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/forms")
public class FormController {
    @Autowired
    private FormService formService;

    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(formService.pageQuery(params));
    }

    @GetMapping("{id:\\d+}")
    public Result findById(@PathVariable String id) {
        return Result.success(formService.findDefinitionById(id));
    }

    @GetMapping("def/context")
    public Result findByIdWithContext(@RequestParam String id) {
        FormDefinition formDefinition = formService.findDefinitionById(id);
        String json = JsonUtil.toJson(formDefinition);
        UserDetails details = AuthenticationContext.details();
        Map<String, Object> context = new HashMap<>();
        context.put("USER_ID", details.id);
        context.put("USERNAME", details.username);
        context.put("DEPT_ID", details.deptId);
        json = PlaceHolderUtil.replace(json, context);
        FormDefinition definition = JsonUtil.readValue(json, FormDefinition.class);
        return Result.success(definition);
    }

    @GetMapping("instance")
    public Result findInstanceData(@RequestParam String id) {
        return Result.success(formService.findInstanceData(id));
    }

    @PostMapping
    public Result saveDefinition(@Validated @RequestBody FormDTO formDTO) {
        formService.saveDefinition(formDTO);
        return Result.success();
    }

    @PutMapping
    public Result updateDefinition(@Validated @RequestBody FormDTO formDTO) {
        formService.updateDefinition(formDTO);
        return Result.success();
    }

    @DeleteMapping
    public Result deleteDefinition(@RequestParam String id) {
        formService.deleteDefinition(id);
        return Result.success();
    }

}
