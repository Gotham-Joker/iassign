package com.github.iassign.controller;

import com.github.core.Result;
import com.github.iassign.service.ProcessInstanceIndexService;
import com.github.iassign.vo.ProcessInstanceIndexVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/process-instance-index")
public class ProcessInstanceIndexController {
    @Autowired
    private ProcessInstanceIndexService processInstanceIndexService;

    /**
     * 用post查询，因为参数可能很多
     *
     * @param index
     * @return
     */
    @PostMapping("query")
    public Result pageQuery(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "10") Integer size,
                            @RequestParam(defaultValue = "true") Boolean highlight,
                            @RequestBody ProcessInstanceIndexVO index) throws IOException {
        /*if (!StringUtils.hasText(index.definitionId)) {
            return Result.error(422, "缺少definitionId参数");
        }*/
        if (index.createTimeGe == null || index.createTimeLe == null) {
            return Result.error("请选择申请日期区间");
        }
        if (LocalDateTime.ofInstant(index.createTimeGe.toInstant(), ZoneId.of("UTC+8")).plus(1, ChronoUnit.MONTHS)
                .plus(1, ChronoUnit.DAYS).isBefore(LocalDateTime.ofInstant(index.createTimeLe.toInstant(), ZoneId.of("UTC+8")))) {
            return Result.error("选择的日期范围不能相差超过1个月");
        }
        return Result.success(processInstanceIndexService.pageQuery(page, size, index, highlight));
    }

    /**
     * 导出
     *
     * @param index
     * @return 返回文件下载路径
     */
    @PostMapping("download")
    public Result download(@RequestBody ProcessInstanceIndexVO index) throws IOException {
        if (index.createTimeGe == null || index.createTimeLe == null) {
            return Result.error("请选择申请日期区间");
        }
        if (LocalDateTime.ofInstant(index.createTimeGe.toInstant(), ZoneId.of("UTC+8")).plus(1, ChronoUnit.MONTHS)
                .plus(1, ChronoUnit.DAYS).isBefore(LocalDateTime.ofInstant(index.createTimeLe.toInstant(), ZoneId.of("UTC+8")))) {
            return Result.error("选择的日期范围不能相差超过1个月,且excel单次导出记录不允许超过1万条");
        }
        return Result.success(processInstanceIndexService.generateExcel(index));
    }
}
