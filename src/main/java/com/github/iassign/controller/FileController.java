package com.github.iassign.controller;

import com.github.core.Result;
import com.github.iassign.service.UploadService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;

@Controller
public class FileController {
    @Autowired
    private UploadService uploadService;

    /**
     * 文件上传
     *
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("upload")
    @ResponseBody
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(uploadService.uploadAttachment(file));
    }

    /**
     * 文件下载
     *
     * @param md5
     */
    @GetMapping("download/{md5}/{fileName}")
    public void download(@PathVariable String md5, @PathVariable String fileName, HttpServletResponse response) throws IOException {
        File file = uploadService.getFile(md5);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
        OutputStream out = response.getOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            StreamUtils.copy(fis, out);
        }
    }
}
