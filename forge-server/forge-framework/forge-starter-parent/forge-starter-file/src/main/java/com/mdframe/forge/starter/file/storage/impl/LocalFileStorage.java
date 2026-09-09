package com.mdframe.forge.starter.file.storage.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 本地文件系统存储实现
 */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {
    
    private static final String STORAGE_TYPE = "local";
    private static final String MULTIPART_DIR = "multipart";
    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[A-Za-z0-9]{1,20}");
    
    private StorageConfig config;
    private String basePath;
    private Path baseDirectory;

    @Value("${forge.file.multipart.max-contexts:100}")
    private int maxMultipartContexts = 100;

    @Value("${forge.file.multipart.ttl-millis:1800000}")
    private long multipartContextTtlMillis = 1_800_000L;
    
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
        
        try {
            Path configuredBase = Paths.get(basePath).toAbsolutePath().normalize();
            Files.createDirectories(configuredBase);
            this.baseDirectory = configuredBase.toRealPath();
            this.basePath = baseDirectory.toString();
        } catch (IOException e) {
            throw new IllegalStateException("初始化本地存储目录失败", e);
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
            Path directory = resolveInsideBase(relativePath);
            createDirectories(directory);
            
            // 保存文件
            Path targetFile = resolveInsideBase(Paths.get(relativePath, storageName).toString());
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            // 构建元数据
            return FileMetadata.builder()
                    .fileId(IdUtil.fastSimpleUUID())
                    .originalName(fileName)
                    .storageName(storageName)
                    .filePath(relativePath + File.separator + storageName)
                    .fileSize(Files.size(targetFile))
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
        context.setTempDir(resolveInsideBase(Paths.get(MULTIPART_DIR, uploadId).toString()).toString());
        context.setLastActivityAt(System.currentTimeMillis());
        
        // 创建临时目录
        try {
            createDirectories(Paths.get(context.getTempDir()));
        } catch (IOException e) {
            throw new RuntimeException("创建分片上传临时目录失败", e);
        }
        
        synchronized (multipartUploads) {
            cleanupExpiredMultipartUploads();
            if (multipartUploads.size() >= Math.max(1, maxMultipartContexts)) {
                FileUtil.del(context.getTempDir());
                throw new IllegalStateException("未完成分片上传数量已达上限，请稍后重试");
            }
            multipartUploads.put(uploadId, context);
        }
        log.info("初始化分片上传: uploadId={}, fileName={}", uploadId, fileName);
        
        return uploadId;
    }
    
    @Override
    public String uploadPart(String uploadId, int partNumber, InputStream inputStream) {
        MultipartUploadContext context = multipartUploads.get(uploadId);
        if (context == null) {
            throw new RuntimeException("无效的上传ID: " + uploadId);
        }
        if (partNumber < 1 || partNumber > 10_000) {
            throw new IllegalArgumentException("分片编号必须在 1 到 10000 之间");
        }
        if (isExpired(context)) {
            removeMultipartUpload(uploadId, context);
            throw new RuntimeException("分片上传已过期，请重新初始化");
        }
        
        try {
            // 保存分片文件
            String partFileName = "part_" + partNumber;
            Path partFile = resolveInsideBase(baseDirectory.relativize(Paths.get(context.getTempDir()))
                    .resolve(partFileName).toString());
            Files.copy(inputStream, partFile, StandardCopyOption.REPLACE_EXISTING);
            
            context.getParts().put(partNumber, partFileName);
            context.setLastActivityAt(System.currentTimeMillis());
            
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
        if (isExpired(context)) {
            removeMultipartUpload(uploadId, context);
            throw new RuntimeException("分片上传已过期，请重新初始化");
        }
        List<Integer> partNumbers = new ArrayList<>(context.getParts().keySet());
        int expectedParts = partETags == null || partETags.isEmpty()
                ? partNumbers.stream().mapToInt(Integer::intValue).max().orElse(0)
                : partETags.size();
        if (expectedParts == 0 || expectedParts > 10_000 || partNumbers.size() != expectedParts
                || partNumbers.stream().anyMatch(partNumber -> partNumber < 1 || partNumber > expectedParts)
                || java.util.stream.IntStream.rangeClosed(1, expectedParts)
                .anyMatch(partNumber -> !context.getParts().containsKey(partNumber))) {
            throw new IllegalArgumentException("分片不完整，无法合并");
        }
        
        try {
            // 生成最终文件路径
            String storageName = generateStorageName(context.getFileName());
            Path directory = resolveInsideBase(context.getRelativePath());
            createDirectories(directory);
            
            Path targetFile = resolveInsideBase(Paths.get(context.getRelativePath(), storageName).toString());
            
            // 合并分片
            try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                for (int i = 1; i <= expectedParts; i++) {
                    String partFileName = context.getParts().get(i);
                    Path partFile = resolveInsideBase(baseDirectory.relativize(Paths.get(context.getTempDir()))
                            .resolve(partFileName).toString());
                    
                    try (FileInputStream fis = new FileInputStream(partFile.toFile())) {
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
                    uploadId, storageName, Files.size(targetFile));
            
            // 构建元数据
            return FileMetadata.builder()
                    .fileId(IdUtil.fastSimpleUUID())
                    .originalName(context.getFileName())
                    .storageName(storageName)
                    .filePath(context.getRelativePath() + File.separator + storageName)
                    .fileSize(Files.size(targetFile))
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

    /** 定期清理未完成且超过 TTL 的上传，避免内存和临时目录无限增长。 */
    @Scheduled(fixedDelayString = "${forge.file.multipart.cleanup-interval-millis:60000}")
    public void cleanupExpiredMultipartUploads() {
        long now = System.currentTimeMillis();
        multipartUploads.forEach((uploadId, context) -> {
            if (now - context.getLastActivityAt() > Math.max(1_000L, multipartContextTtlMillis)) {
                removeMultipartUpload(uploadId, context);
            }
        });
    }

    private boolean isExpired(MultipartUploadContext context) {
        return System.currentTimeMillis() - context.getLastActivityAt()
                > Math.max(1_000L, multipartContextTtlMillis);
    }

    private void removeMultipartUpload(String uploadId, MultipartUploadContext context) {
        if (multipartUploads.remove(uploadId, context)) {
            FileUtil.del(context.getTempDir());
            log.info("清理过期分片上传: uploadId={}", uploadId);
        }
    }
    
    @Override
    public InputStream download(String fileId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        Path file = resolveInsideBase(metadata.getFilePath());
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new RuntimeException("文件不存在: " + file);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
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
        
        Path file = resolveInsideBase(metadata.getFilePath());
        try {
            boolean deleted = Files.deleteIfExists(file);
            log.info("删除文件: {}, 结果: {}", file, deleted);
            return deleted;
        } catch (IOException e) {
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
        
        Path file = resolveInsideBase(metadata.getFilePath());
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public boolean testConnection() {
        return baseDirectory != null && Files.isDirectory(baseDirectory) && Files.isWritable(baseDirectory);
    }

    @Override
    public boolean createBucket(String bucketName) {
        if (bucketName == null || bucketName.isEmpty()) {
            return true;
        }
        requireSafeSegment(bucketName, "bucketName");
        Path directory = resolveInsideBase(bucketName);
        try {
            createDirectories(directory);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("创建存储桶失败", e);
        }
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        if (bucketName == null || bucketName.isEmpty()) {
            return false;
        }
        requireSafeSegment(bucketName, "bucketName");
        return FileUtil.del(resolveInsideBase(bucketName).toFile());
    }

    @Override
    public boolean bucketExists(String bucketName) {
        if (bucketName == null || bucketName.isEmpty()) {
            return Files.isDirectory(baseDirectory);
        }
        requireSafeSegment(bucketName, "bucketName");
        return Files.isDirectory(resolveInsideBase(bucketName), LinkOption.NOFOLLOW_LINKS);
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
        String safeBusinessType = businessType != null ? businessType : "common";
        requireSafeSegment(safeBusinessType, "businessType");
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return Paths.get(safeBusinessType, date.split("/")).toString();
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
        
        String extension = fileName.substring(lastDot + 1);
        return SAFE_EXTENSION.matcher(extension).matches() ? extension : "";
    }

    private void requireSafeSegment(String value, String fieldName) {
        if (value == null || !SAFE_PATH_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(fieldName + " 只能包含字母、数字、下划线和中划线");
        }
    }

    private Path resolveInsideBase(String relativePath) {
        if (baseDirectory == null) {
            throw new IllegalStateException("本地存储尚未初始化");
        }
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("文件相对路径不能为空");
        }
        Path relative = Paths.get(relativePath);
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("禁止使用绝对文件路径");
        }
        Path candidate = baseDirectory.resolve(relative).normalize();
        if (!candidate.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("文件路径超出本地存储目录");
        }
        rejectSymbolicLinks(candidate);
        return candidate;
    }

    private void rejectSymbolicLinks(Path candidate) {
        Path current = baseDirectory;
        for (Path segment : baseDirectory.relativize(candidate)) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("本地存储路径禁止包含符号链接");
            }
        }
    }

    private void createDirectories(Path directory) throws IOException {
        rejectSymbolicLinks(directory);
        Files.createDirectories(directory);
        rejectSymbolicLinks(directory);
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
        private long lastActivityAt;
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

        public long getLastActivityAt() {
            return lastActivityAt;
        }

        public void setLastActivityAt(long lastActivityAt) {
            this.lastActivityAt = lastActivityAt;
        }
        
        public Map<Integer, String> getParts() {
            return parts;
        }
    }
}
