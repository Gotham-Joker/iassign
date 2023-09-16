package com.github.iassign.controller;

import com.github.iassign.service.UploadService;
import com.github.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@RestController
@RequestMapping("/upload")
public class UploadController {
    @Autowired
    private UploadService uploadService;

    @PostMapping
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(uploadService.upload(file));
    }
}
