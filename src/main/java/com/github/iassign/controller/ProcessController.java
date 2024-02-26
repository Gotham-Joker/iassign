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
