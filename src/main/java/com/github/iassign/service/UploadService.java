package com.github.iassign.service;

import cn.hutool.core.io.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class UploadService {
    @Value("${upload.path:/tmp}")
    private String uploadPath;
    @Value("${upload.server-url:http://localhost:8080}")
    private String serverUrl;

    /**
     * 每天清理一次tmp
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void clean() {
        File file = new File(uploadPath + "/tmp");
        if (file.exists()) {
            FileUtil.clean(file);
        }
    }

    /**
     * 文件上传
     *
     * @param file
     * @return
     * @throws IOException
     */
    public Map<String, String> upload(MultipartFile file) throws IOException {
        Path filePath = Paths.get(uploadPath + "/" + file.getOriginalFilename());
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(filePath)) {
            StreamUtils.copy(in, out);
        }
        Map<String, String> result = new HashMap<>();
        result.put("uid", filePath.getFileName().toString());
        result.put("name", file.getOriginalFilename());
        result.put("status", "done");
        result.put("response", "ok");
        result.put("url", serverUrl + "/" + file.getOriginalFilename());
        return result;
    }

    /**
     * 将文件保存到下载目录的子目录，如果子目录名是tmp里面的文件每天会被清理一次
     *
     * @param relativeDir 子目录名
     * @param file
     * @return
     * @throws IOException
     */
    public String save(String relativeDir, File file) throws IOException {
        File dir = new File(uploadPath + "/" + relativeDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Path filePath = Paths.get(dir.getPath() + "/" + file.getName());
        try (InputStream in = Files.newInputStream(file.toPath());
             OutputStream out = Files.newOutputStream(filePath)) {
            StreamUtils.copy(in, out);
        }
        return serverUrl + "/" + relativeDir + "/" + file.getName();
    }

    /**
     * 存到临时目录，里面的文件每天会被清理一次
     *
     * @param file
     * @return 返回文件下载路径
     * @throws IOException
     */
    public String saveToTmp(File file) throws IOException {
        return save("tmp", file);
    }

}
