package com.github.iassign.controller;

import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.service.ProcessService;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process")
public class ProcessController {
    @Autowired
    private ProcessService processService;

    /**
     * 启动流程实例
     */
    @PostMapping
    public Result startInstance(@Validated @RequestBody ProcessStartDTO dto) throws Exception {
        return Result.success(processService.startInstance(dto).id);
    }

    @PutMapping("cancel")
    public Result cancelInstance(@RequestParam String id) {
        processService.cancelInstance(id);
        return Result.success();
    }
}
