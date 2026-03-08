package com.mdframe.forge.starter.file.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地文件系统存储实现
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {
    
    private static final String STORAGE_TYPE = "local";
    private static final String MULTIPART_DIR = "multipart";
    
    private StorageConfig config;
    private String basePath;
    
    @Autowired(required = false)
    private FileMetadataPersistence metadataPersistence;
    
    /**
     * 分片上传临时信息存储
     */
    private final Map<String, MultipartUploadContext> multipartUploads = new ConcurrentHashMap<>();
    
    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }
    
    @Override
    public void init(StorageConfig config) {
        this.config = config;
        this.basePath = config.getBasePath();
        
        if (basePath == null || basePath.isEmpty()) {
            this.basePath = System.getProperty("user.home") + File.separator + "file-storage";
        }
        
        // 确保基础路径存在
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                log.info("创建本地存储目录: {}", basePath);
            }
        }
        
        log.info("本地文件存储初始化完成, 基础路径: {}", basePath);
    }
    
    @Override
    public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
        try {
            return upload(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                businessType,
                businessId
            );
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId) {
        try {
            // 生成存储路径和文件名
            String storageName = generateStorageName(fileName);
            String relativePath = generateRelativePath(businessType);
            String fullPath = basePath + relativePath;
            
            // 确保目录存在
            File dir = new File(fullPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new RuntimeException("创建目录失败: " + fullPath);
                }
                log.debug("创建文件存储目录: {}", fullPath);
            }
            
            // 保存文件
            File targetFile = new File(fullPath + File.separator + storageName);
            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // 构建元数据
            return FileMetadata.builder()
                    .fileId(IdUtil.fastSimpleUUID())
                    .originalName(fileName)
                    .storageName(storageName)
                    .filePath(relativePath + File.separator + storageName)
                    .fileSize(targetFile.length())
                    .mimeType(contentType)
                    .extension(getExtension(fileName))
                    .storageType(STORAGE_TYPE)
                    .bucket(null)
                    .businessType(businessType)
                    .businessId(businessId)
                    .uploadTime(LocalDateTime.now())
                    .isPrivate(false)
                    .downloadCount(0)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }
    
    @Override
    public String initMultipartUpload(String fileName, String businessType, String businessId) {
        String uploadId = IdUtil.fastSimpleUUID();
        String relativePath = generateRelativePath(businessType);
        
        MultipartUploadContext context = new MultipartUploadContext();
        context.setUploadId(uploadId);
        context.setFileName(fileName);
        context.setBusinessType(businessType);
        context.setBusinessId(businessId);
        context.setRelativePath(relativePath);
        context.setTempDir(basePath + File.separator + MULTIPART_DIR + File.separator + uploadId);
        
        // 创建临时目录
        File tempDir = new File(context.getTempDir());
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (!created && !tempDir.exists()) {
                throw new RuntimeException("创建临时目录失败: " + context.getTempDir());
            }
            log.debug("创建分片上传临时目录: {}", context.getTempDir());
        }
        
        multipartUploads.put(uploadId, context);
        log.info("初始化分片上传: uploadId={}, fileName={}", uploadId, fileName);
        
        return uploadId;
    }
    
    @Override
    public String uploadPart(String uploadId, int partNumber, InputStream inputStream) {
        MultipartUploadContext context = multipartUploads.get(uploadId);
        if (context == null) {
            throw new RuntimeException("无效的上传ID: " + uploadId);
        }
        
        try {
            // 保存分片文件
            String partFileName = "part_" + partNumber;
            File partFile = new File(context.getTempDir() + File.separator + partFileName);
            Files.copy(inputStream, partFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            context.getParts().put(partNumber, partFileName);
            
            log.debug("上传分片成功: uploadId={}, partNumber={}", uploadId, partNumber);
            return partFileName;
        } catch (IOException e) {
            throw new RuntimeException("分片上传失败", e);
        }
    }
    
    @Override
    public FileMetadata completeMultipartUpload(String uploadId, List<String> partETags) {
        MultipartUploadContext context = multipartUploads.get(uploadId);
        if (context == null) {
            throw new RuntimeException("无效的上传ID: " + uploadId);
        }
        
        try {
            // 生成最终文件路径
            String storageName = generateStorageName(context.getFileName());
            String fullPath = basePath + File.separator + context.getRelativePath();
            File dir = new File(fullPath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new RuntimeException("创建目录失败: " + fullPath);
                }
                log.debug("创建文件存储目录: {}", fullPath);
            }
            
            File targetFile = new File(fullPath + File.separator + storageName);
            // 确保父目录存在
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 合并分片
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                for (int i = 1; i <= context.getParts().size(); i++) {
                    String partFileName = context.getParts().get(i);
                    File partFile = new File(context.getTempDir() + File.separator + partFileName);
                    
                    try (FileInputStream fis = new FileInputStream(partFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            
            // 删除临时文件
            FileUtil.del(context.getTempDir());
            multipartUploads.remove(uploadId);
            
            log.info("完成分片上传: uploadId={}, fileName={}, size={}",
                    uploadId, storageName, targetFile.length());
            
            // 构建元数据
            return FileMetadata.builder()
                    .fileId(IdUtil.fastSimpleUUID())
                    .originalName(context.getFileName())
                    .storageName(storageName)
                    .filePath(context.getRelativePath() + File.separator + storageName)
                    .fileSize(targetFile.length())
                    .extension(getExtension(context.getFileName()))
                    .storageType(STORAGE_TYPE)
                    .businessType(context.getBusinessType())
                    .businessId(context.getBusinessId())
                    .uploadTime(LocalDateTime.now())
                    .isPrivate(false)
                    .downloadCount(0)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("合并分片失败", e);
        }
    }
    
    @Override
    public InputStream download(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        try {
            String fullPath = basePath + File.separator + metadata.getFilePath();
            File file = new File(fullPath);
            
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + fullPath);
            }
            
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("文件读取失败", e);
        }
    }
    
    @Override
    public String getAccessUrl(String fileId, Integer expires) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return null;
        }
        
        // 本地存储返回相对路径，需要配合文件下载接口使用
        String domain = config.getDomain();
        if (domain != null && !domain.isEmpty()) {
            return domain + "/api/file/download/" + fileId;
        }
        
        return "/api/file/download/" + fileId;
    }
    
    @Override
    public boolean delete(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return false;
        }
        
        try {
            String fullPath = basePath + File.separator + metadata.getFilePath();
            File file = new File(fullPath);
            
            if (file.exists()) {
                boolean deleted = file.delete();
                log.info("删除文件: {}, 结果: {}", fullPath, deleted);
                return deleted;
            }
            
            return false;
        } catch (Exception e) {
            log.error("删除文件失败: {}", fileId, e);
            return false;
        }
    }
    
    @Override
    public boolean exists(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            return false;
        }
        
        String fullPath = basePath + File.separator + metadata.getFilePath();
        File file = new File(fullPath);
        return file.exists();
    }
    
    /**
     * 生成存储文件名
     */
    private String generateStorageName(String originalFileName) {
        String extension = getExtension(originalFileName);
        String uuid = IdUtil.fastSimpleUUID();
        return extension.isEmpty() ? uuid : uuid + "." + extension;
    }
    
    /**
     * 生成相对路径（按日期和业务类型分组）
     */
    private String generateRelativePath(String businessType) {
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return (businessType != null ? businessType : "common") + File.separator + date;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        
        return fileName.substring(lastDot + 1);
    }
    
    /**
     * 获取文件元数据
     */
    private FileMetadata getFileMetadata(String fileId) {
        if (metadataPersistence == null) {
            log.warn("FileMetadataPersistence未配置，无法获取文件元数据");
            return null;
        }
        return metadataPersistence.getById(fileId);
    }
    
    /**
     * 分片上传上下文
     */
    private static class MultipartUploadContext {
        private String uploadId;
        private String fileName;
        private String businessType;
        private String businessId;
        private String relativePath;
        private String tempDir;
        private final Map<Integer, String> parts = new ConcurrentHashMap<>();
        
        public String getUploadId() {
            return uploadId;
        }
        
        public void setUploadId(String uploadId) {
            this.uploadId = uploadId;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public String getBusinessType() {
            return businessType;
        }
        
        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }
        
        public String getBusinessId() {
            return businessId;
        }
        
        public void setBusinessId(String businessId) {
            this.businessId = businessId;
        }
        
        public String getRelativePath() {
            return relativePath;
        }
        
        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }
        
        public String getTempDir() {
            return tempDir;
        }
        
        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }
        
        public Map<Integer, String> getParts() {
            return parts;
        }
    }
}
