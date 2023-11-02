package com.github.iassign.service;

import cn.hutool.core.io.FileUtil;
import com.github.core.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
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
     * 文件夹不存在的时候创建文件夹
     */
    @PostConstruct
    public void init() {
        File file = new File(uploadPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

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
     * 文件上传，专门给ant-design的上传组件使用，关心文件名，但是又不能重复保存相同md5的文件
     *
     * @param file
     * @return
     * @throws IOException
     */
    public Map<String, String> uploadAttachment(MultipartFile file) throws IOException {
        String md5;
        try (InputStream in = file.getInputStream()) {
            md5 = DigestUtils.md5DigestAsHex(in);
        }
        Path filePath = Paths.get(uploadPath + "/" + md5);
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(filePath)) {
            StreamUtils.copy(in, out);
        }
        Map<String, String> result = new HashMap<>();
        result.put("uid", md5);
        result.put("name", file.getOriginalFilename());
        result.put("status", "done");
        result.put("response", "ok");
        result.put("url", serverUrl + "/download/" + md5 + "/" + URLEncoder.encode(file.getOriginalFilename(), "utf-8"));
        return result;
    }

    /**
     * 文件上传，一般是用于可预览的、不关心文件名的场景
     *
     * @param file
     * @return
     * @throws IOException
     */
    public String upload(MultipartFile file) throws IOException {
        String md5;
        try (InputStream in = file.getInputStream()) {
            md5 = DigestUtils.md5DigestAsHex(in);
        }
        int index = file.getOriginalFilename().lastIndexOf(".");
        if (index == -1) {
            throw new ApiException(500, "文件扩展名异常");
        }
        String hashFileName = md5 + file.getOriginalFilename().substring(index);
        Path filePath = Paths.get(uploadPath + "/" + hashFileName);
        try (InputStream in = file.getInputStream();
             OutputStream out = Files.newOutputStream(filePath)) {
            StreamUtils.copy(in, out);
        }
        return serverUrl + "/" + hashFileName;
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

    public File getFile(String fileRelativePath) {
        return new File(uploadPath + "/" + fileRelativePath);
    }
}
