package com.github.iassign.controller;

import com.github.core.Result;
import com.github.iassign.dto.FormDTO;
import com.github.iassign.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/forms")
public class FormController  {
    @Autowired
    private FormService formService;

    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(formService.pageQuery(params));
    }

    @GetMapping("findById")
    public Result findById(@RequestParam String id) {
        return Result.success(formService.findDefinitionById(id));
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
