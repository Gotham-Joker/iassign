package com.github.iassign.controller;

import com.github.iassign.service.ProcessInstanceService;
import com.github.core.Result;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 流程实例查询
 */
@RestController
@RequestMapping("/api/process-instance")
public class ProcessInstanceController {
    @Autowired
    private ProcessInstanceService processInstanceService;

    /**
     * 分页查询流程实例
     *
     * @param params
     * @return
     */
    @GetMapping
    public Result pageQuery(@RequestParam Map<String, String> params) {
        return Result.success(processInstanceService.pageQuery(params));
    }


    /**
     * 流程实例详情
     */
    @GetMapping("detail")
    public Result findDetail(@RequestParam String id) {
        return processInstanceService.findDetail(id);
    }

    @GetMapping("log")
    public void downloadLog(@RequestParam String instanceId, HttpServletResponse response) throws IOException {

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + instanceId + ".log");
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        ServletOutputStream out = response.getOutputStream();
        File file = new File("./process/" + instanceId + ".log");
        if (!file.exists()) {
            out.write("日志文件不存在".getBytes(StandardCharsets.UTF_8));
            return;
        }
        // 查找日志，下载
        try (FileInputStream in = new FileInputStream(file)) {
            StreamUtils.copy(in, out);
        }
    }
}
