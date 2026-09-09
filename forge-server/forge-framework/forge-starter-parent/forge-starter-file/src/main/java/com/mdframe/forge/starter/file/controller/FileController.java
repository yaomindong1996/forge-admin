package com.mdframe.forge.starter.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * 通用文件管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "forge.file.enable-generic-api", havingValue = "true", matchIfMissing = true)
public class FileController {

    private static final String BUCKET_ADMIN_MESSAGE = "只有超级管理员可以管理存储桶";
    
    private final FileManager fileManager;
    
    /**
     * 上传文件
     */
    @ApiPermissionIgnore
    @PostMapping("/upload")
    public RespInfo<FileMetadata> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false, defaultValue = "common") String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "storageType", required = false) String storageType,
            @RequestParam(value = "isPrivate", required = false, defaultValue = "true") Boolean isPrivate) {

        if (!isPrivate && !StpUtil.hasPermission("*:*:*")) {
            return RespInfo.error("只有管理员才能上传公共素材");
        }

        FileMetadata metadata = fileManager.upload(file, businessType, businessId, storageType, isPrivate);

        return RespInfo.success(metadata);
    }
    
    /**
     * 下载文件
     */
    @ApiPermissionIgnore
    @GetMapping("/download/{fileId}")
    public void download(@PathVariable String fileId, HttpServletResponse response) {
        fileManager.download(fileId, response);
    }
    
    /**
     * 获取文件访问URL
     */
    @ApiPermissionIgnore
    @GetMapping("/url/{fileId}")
    public RespInfo<String> getAccessUrl(
            @PathVariable String fileId,
            @RequestParam(value = "expires", required = false, defaultValue = "3600") Integer expires,
            HttpServletResponse response) {
        
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Pragma", "no-cache");
        String url = fileManager.getAccessUrl(fileId, expires);
        return RespInfo.success(url);
    }
    
    /**
     * 删除文件
     */
    @ApiPermissionIgnore
    @DeleteMapping("/{fileId}")
    public RespInfo<Boolean> delete(@PathVariable String fileId) {
        boolean success = fileManager.delete(fileId);
        return RespInfo.success(success);
    }

    /**
     * 创建存储桶/本地目录
     */
    @PostMapping("/bucket")
    public RespInfo<Boolean> createBucket(
            @RequestParam("storageType") String storageType,
            @RequestParam("bucketName") String bucketName) {
        SessionHelper.assertAdmin(BUCKET_ADMIN_MESSAGE);
        return RespInfo.success(fileManager.createBucket(storageType, bucketName));
    }

    /**
     * 删除存储桶/本地目录
     */
    @DeleteMapping("/bucket")
    public RespInfo<Boolean> deleteBucket(
            @RequestParam("storageType") String storageType,
            @RequestParam("bucketName") String bucketName) {
        SessionHelper.assertAdmin(BUCKET_ADMIN_MESSAGE);
        return RespInfo.success(fileManager.deleteBucket(storageType, bucketName));
    }

    /**
     * 检查存储桶/本地目录是否存在
     */
    @GetMapping("/bucket/exists")
    public RespInfo<Boolean> bucketExists(
            @RequestParam("storageType") String storageType,
            @RequestParam("bucketName") String bucketName) {
        SessionHelper.assertAdmin(BUCKET_ADMIN_MESSAGE);
        return RespInfo.success(fileManager.bucketExists(storageType, bucketName));
    }
    
    /**
     * 分片上传 - 初始化
     */
    @ApiPermissionIgnore
    @PostMapping("/multipart/init")
    public RespInfo<String> initMultipartUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "businessType", required = false, defaultValue = "common") String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "storageType", required = false, defaultValue = "local") String storageType) {
        
        String uploadId = fileManager.initMultipartUpload(fileName, businessType, businessId, storageType);
        return RespInfo.success(uploadId);
    }
    
    /**
     * 分片上传 - 上传分片
     */
    @ApiPermissionIgnore
    @PostMapping("/multipart/upload")
    public RespInfo<String> uploadPart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") Integer partNumber,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "storageType", required = false, defaultValue = "local") String storageType) throws Exception {
        
        try (InputStream inputStream = file.getInputStream()) {
            String etag = fileManager.uploadPart(uploadId, partNumber, inputStream, storageType);
            return RespInfo.success(etag);
        }
    }
    
    /**
     * 分片上传 - 完成
     */
    @ApiPermissionIgnore
    @PostMapping("/multipart/complete")
    public RespInfo<FileMetadata> completeMultipartUpload(
            @RequestParam("uploadId") String uploadId,
            @RequestBody List<String> partETags,
            @RequestParam(value = "storageType", required = false, defaultValue = "local") String storageType) {
        
        FileMetadata metadata = fileManager.completeMultipartUpload(uploadId, partETags, storageType);
        return RespInfo.success(metadata);
    }
}
