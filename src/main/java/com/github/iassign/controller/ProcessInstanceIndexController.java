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

import com.github.core.DateUtil;
import com.github.core.Result;
import com.github.iassign.service.ProcessInstanceIndexService;
import com.github.iassign.vo.ProcessInstanceIndexVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/process-instance-index")
public class ProcessInstanceIndexController {
    @Autowired
    private ProcessInstanceIndexService processInstanceIndexService;

    /**
     * 用post查询，因为参数可能很多
     *
     * @param lastId search after，即从哪条数据id之后开始查询，大数据量时分页友好
     * @param score  search after，评分
     * @param index
     * @return
     */
    @PostMapping("query")
    public Result pageQuery(@RequestParam(required = false) String lastId,
                            @RequestParam(required = false) String score,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestBody ProcessInstanceIndexVO index) throws IOException {
        if (!StringUtils.hasText(index.definitionId)) {
            return Result.error(422, "缺少definitionId参数");
        }
        if (index.createTimeGe == null || index.createTimeLe == null) {
            return Result.error("请选择申请日期区间");
        }
        if (DateUtil.toLocalDateTime(index.createTimeGe).plus(6, ChronoUnit.MONTHS).plus(1, ChronoUnit.DAYS)
                .isBefore(DateUtil.toLocalDateTime(index.createTimeLe))) {
            return Result.error("选择的日期范围不能相差6个月以上");
        }
        return Result.success(processInstanceIndexService.pageQuery(lastId, score, size, index,
                StringUtils.hasText(index.content), true));
    }

    /**
     * 导出
     *
     * @param index
     * @return 返回文件下载路径
     */
    @PostMapping("download")
    public Result download(@RequestBody ProcessInstanceIndexVO index) throws IOException {
        if (!StringUtils.hasText(index.definitionId)) {
            return Result.error(422, "缺少definitionId参数");
        }
        if (index.createTimeGe == null || index.createTimeLe == null) {
            return Result.error("请选择申请日期区间");
        }
        if (DateUtil.toLocalDateTime(index.createTimeGe).plus(6, ChronoUnit.MONTHS).plus(1, ChronoUnit.DAYS)
                .isBefore(DateUtil.toLocalDateTime(index.createTimeLe))) {
            return Result.error("选择的日期范围不能相差6个月以上");
        }
        return Result.success(processInstanceIndexService.generateExcel(index));
    }

}
