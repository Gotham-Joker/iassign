package com.github.iassign.controller;

import com.github.iassign.dto.ProcessStartDTO;
import com.github.iassign.service.ProcessService;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
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
        dto.instanceId = null;
        return Result.success(processService.startInstance(dto).id);
    }

    /**
     * 退回申请人后，申请人修改，然后重新启动流程实例
     */
    @PutMapping
    public Result restartInstance(@Validated @RequestBody ProcessStartDTO dto) {
        if (!StringUtils.hasText(dto.instanceId)) {
            return Result.error(422, "缺少instanceId参数");
        }
        return processService.restartInstance(dto);
    }

    /**
     * 取消流程实例（撤回）
     *
     * @param id
     * @return
     */
    @PutMapping("cancel")
    public Result cancelInstance(@RequestParam String id) {
        processService.cancelInstance(id);
        return Result.success();
    }

}
