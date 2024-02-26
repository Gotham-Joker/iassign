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

package com.github.iassign.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.UserDetails;
import com.github.core.JsonUtil;
import com.github.core.Result;
import com.github.iassign.core.util.PlaceHolderUtils;
import com.github.iassign.dto.FormDTO;
import com.github.iassign.entity.FormDefinition;
import com.github.iassign.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/forms")
public class FormController {
    @Autowired
    private FormService formService;
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(formService.pageQuery(params));
    }

    @GetMapping("{id:\\d+}")
    public Result findById(@PathVariable String id) {
        return Result.success(formService.findDefinitionById(id));
    }

    /**
     * 查找表单，并且进行上下文变量替换
     *
     * @return
     */
    @GetMapping("def/context")
    public Result findByIdWithContext(@RequestParam String id) {
        FormDefinition formDefinition = formService.findDefinitionById(id);
        String json = JsonUtil.toJson(formDefinition);
        UserDetails details = AuthenticationContext.details();
        Map<String, Object> context = new HashMap<>();
        context.put("USER_ID", details.id);
        context.put("USERNAME", details.username);
        context.put("DEPT_ID", details.deptId);
        context.put("DEPT_CODE", details.deptCode);
        json = PlaceHolderUtils.replace(json, context);
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

    /**
     * 导出表单
     */
    @GetMapping("out")
    public ResponseEntity<byte[]> exportForm(@RequestParam String id) {
        return Result.octetStream(id+"_f.json", JsonUtil.toJson(formService.selectById(id)).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 导入表单
     */
    @PostMapping("in")
    public Result importForm(@RequestParam MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()){
            FormDefinition definition = objectMapper.readValue(in, FormDefinition.class);
            formService.save(definition);
        }
        return Result.success();
    }
}
