package com.mdframe.forge.starter.file.core;

import com.mdframe.forge.starter.file.model.FileMetadata;
import com.mdframe.forge.starter.file.model.StorageConfig;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import com.mdframe.forge.starter.file.spi.StorageConfigProvider;
import com.mdframe.forge.starter.file.storage.FileStorage;
import com.mdframe.forge.starter.file.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件管理器
 * 统一文件上传、下载、删除等操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileManager {

    private static final long DEFAULT_MAX_FILE_SIZE_MB = 100L;

    public static final String DEFAULT_ALLOWED_TYPES =
            "jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3";

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            "jsp", "jspx", "php", "asp", "aspx", "html", "htm",
            "js", "mjs", "ts", "vue", "sh", "bash", "bat", "cmd",
            "ps1", "exe", "dll", "so", "dylib", "jar", "war", "ear",
            "sql", "md", "svg"
    );

    private static final Set<String> DANGEROUS_MIME_TYPES = Set.of(
            "text/html", "application/javascript", "text/javascript",
            "application/x-javascript", "image/svg+xml",
            "application/x-sh", "application/x-msdownload",
            "application/x-msdos-program", "application/x-php"
    );

    private static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("doc", Set.of("application/msword", "application/octet-stream")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream")),
            Map.entry("xls", Set.of("application/vnd.ms-excel", "application/octet-stream")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "application/octet-stream")),
            Map.entry("txt", Set.of("text/plain", "application/octet-stream")),
            Map.entry("csv", Set.of("text/csv", "application/vnd.ms-excel", "text/plain", "application/octet-stream")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")),
            Map.entry("rar", Set.of("application/vnd.rar", "application/x-rar-compressed", "application/octet-stream")),
            Map.entry("mp4", Set.of("video/mp4", "application/octet-stream")),
            Map.entry("mp3", Set.of("audio/mpeg", "audio/mp3", "application/octet-stream"))
    );
    
    private final Map<String, FileStorage> storageMap = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private StorageConfigProvider configProvider;
    
    @Autowired(required = false)
    private FileMetadataPersistence metadataPersistence;
    
    /**
     * 注册存储策略
     */
    public void registerStorage(FileStorage storage) {
        storageMap.put(storage.getStorageType(), storage);
        log.info("注册文件存储策略: {}", storage.getStorageType());
    }
    
    /**
     * 获取存储策略
     */
    public FileStorage getStorage(String storageType) {
        return storageMap.get(storageType);
    }

    /**
     * 获取文件元数据
     */
    public FileMetadata getMetadata(String fileId) {
        if (metadataPersistence == null) {
            return null;
        }
        return metadataPersistence.getById(fileId);
    }
    
    /**
     * 上传文件（使用默认存储策略）
     */
    public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
        return upload(file, businessType, businessId, null, null);
    }

    /**
     * 上传文件（指定存储策略 + 可见范围）
     */
    public FileMetadata upload(MultipartFile file, String businessType, String businessId,
                               String storageType, Boolean isPrivate) {
        if (storageType == null) {
            if (configProvider == null) {
                throw new RuntimeException("未配置StorageConfigProvider");
            }
            StorageConfig config = configProvider.getDefaultConfig();
            if (config == null) {
                throw new RuntimeException("未找到默认存储配置");
            }
            storageType = config.getStorageType();
        }
        return doUpload(file, businessType, businessId, storageType, isPrivate);
    }

    /**
     * 上传文件（指定存储策略）
     */
    public FileMetadata upload(MultipartFile file, String businessType, String businessId, String storageType) {
        return doUpload(file, businessType, businessId, storageType, null);
    }

    /**
     * 上传文件流（使用默认存储策略）。
     */
    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId) {
        return upload(inputStream, fileName, contentType, businessType, businessId, null, null);
    }

    /**
     * 上传文件流（指定存储策略 + 可见范围）。
     */
    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId,
                               String storageType, Boolean isPrivate) {
        return upload(inputStream, fileName, contentType, businessType, businessId, storageType, isPrivate, null);
    }

    /**
     * 上传文件流（指定存储策略 + 可见范围 + 已知文件大小）。
     */
    public FileMetadata upload(InputStream inputStream, String fileName, String contentType,
                               String businessType, String businessId,
                               String storageType, Boolean isPrivate, Long fileSize) {
        if (inputStream == null) {
            throw new RuntimeException("文件流不能为空");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }
        if (storageType == null) {
            if (configProvider == null) {
                throw new RuntimeException("未配置StorageConfigProvider");
            }
            StorageConfig config = configProvider.getDefaultConfig();
            if (config == null) {
                throw new RuntimeException("未找到默认存储配置");
            }
            storageType = config.getStorageType();
        }
        validateFileName(fileName, storageType, fileSize);

        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }

        FileMetadata metadata = storage.upload(inputStream, fileName, contentType, businessType, businessId, fileSize);
        if (metadata.getFileSize() == null && fileSize != null) {
            metadata.setFileSize(fileSize);
        }
        if (isPrivate != null) {
            metadata.setIsPrivate(isPrivate);
        }
        if (metadataPersistence != null) {
            metadataPersistence.save(metadata);
        }
        return metadata;
    }

    private FileMetadata doUpload(MultipartFile file, String businessType, String businessId,
                                  String storageType, Boolean isPrivate) {
        // 验证文件
        validateFile(file, storageType);
        
        // 秒传检查：仅当 businessType 和 businessId 也一致时才复用
        String md5 = FileUtil.calculateMd5(file);
        if (metadataPersistence != null) {
            FileMetadata existing = metadataPersistence.getByMd5(md5);
            if (existing != null
                    && java.util.Objects.equals(existing.getBusinessType(), businessType)
                    && java.util.Objects.equals(existing.getBusinessId(), businessId)) {
                log.info("文件秒传: md5={}, businessType={}", md5, businessType);
                return existing;
            }
        }
        
        // 获取存储策略并上传
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        
        FileMetadata metadata = storage.upload(file, businessType, businessId);
        metadata.setMd5(md5);
        if (isPrivate != null) {
            metadata.setIsPrivate(isPrivate);
        }

        // 持久化元数据
        if (metadataPersistence != null) {
            metadataPersistence.save(metadata);
        }
        
        return metadata;
    }
    
    /**
     * 下载文件
     */
    public void download(String fileId, HttpServletResponse response) {
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }
        
        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        FileStorage storage = getStorage(metadata.getStorageType());
        if (storage == null) {
            throw new RuntimeException("存储策略不存在: " + metadata.getStorageType());
        }
        
        try (InputStream inputStream = storage.download(fileId);
             OutputStream outputStream = response.getOutputStream()) {
            
            response.setContentType(metadata.getMimeType());
            response.setHeader("Content-Disposition",
                "attachment;filename=" + java.net.URLEncoder.encode(metadata.getOriginalName(), StandardCharsets.UTF_8));
            
            inputStream.transferTo(outputStream);
            
            // 更新下载次数
            metadataPersistence.incrementDownloadCount(fileId);
            
        } catch (Exception e) {
            log.error("文件下载失败: {}", fileId, e);
            throw new RuntimeException("文件下载失败", e);
        }
    }
    
    /**
     * 获取文件访问URL
     */
    public String getAccessUrl(String fileId, Integer expires) {
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }
        
        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }
        
        FileStorage storage = getStorage(metadata.getStorageType());
        if (storage == null) {
            throw new RuntimeException("存储策略不存在: " + metadata.getStorageType());
        }
        
        return storage.getAccessUrl(fileId, expires);
    }
    
    /**
     * 获取文件内容的Base64编码
     */
    public String getFileContentBase64(String fileId) {
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }
        
        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            return null;
        }
        
        FileStorage storage = getStorage(metadata.getStorageType());
        if (storage == null) {
            return null;
        }
        
        try (InputStream inputStream = storage.download(fileId)) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("获取文件Base64失败: {}", fileId, e);
            return null;
        }
    }
    
    /**
     * 获取文件元数据
     */
    public FileMetadata getFileMetadata(String fileId) {
        if (metadataPersistence == null) {
            return null;
        }
        return metadataPersistence.getById(fileId);
    }
    
    /**
     * 删除文件
     */
    public boolean delete(String fileId) {
        if (metadataPersistence == null) {
            throw new RuntimeException("未配置FileMetadataPersistence");
        }
        
        FileMetadata metadata = metadataPersistence.getById(fileId);
        if (metadata == null) {
            return false;
        }
        
        FileStorage storage = getStorage(metadata.getStorageType());
        if (storage != null) {
            storage.delete(fileId);
        }
        
        metadataPersistence.delete(fileId);
        return true;
    }
    
    /**
     * 分片上传初始化
     */
    public String initMultipartUpload(String fileName, String businessType, String businessId, String storageType) {
        validateFileName(fileName, storageType, null);
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.initMultipartUpload(fileName, businessType, businessId);
    }
    
    /**
     * 上传分片
     */
    public String uploadPart(String uploadId, int partNumber, InputStream inputStream, String storageType) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.uploadPart(uploadId, partNumber, inputStream);
    }
    
    /**
     * 完成分片上传
     */
    public FileMetadata completeMultipartUpload(String uploadId, List<String> partETags, String storageType) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        
        FileMetadata metadata = storage.completeMultipartUpload(uploadId, partETags);
        
        if (metadataPersistence != null) {
            metadataPersistence.save(metadata);
        }
        
        return metadata;
    }

    /**
     * 测试存储连接
     */
    public boolean testConnection(String storageType) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.testConnection();
    }

    /**
     * 创建存储桶
     */
    public boolean createBucket(String storageType, String bucketName) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.createBucket(bucketName);
    }

    /**
     * 删除存储桶
     */
    public boolean deleteBucket(String storageType, String bucketName) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.deleteBucket(bucketName);
    }

    /**
     * 检查存储桶是否存在
     */
    public boolean bucketExists(String storageType, String bucketName) {
        FileStorage storage = getStorage(storageType);
        if (storage == null) {
            throw new RuntimeException("不支持的存储类型: " + storageType);
        }
        return storage.bucketExists(bucketName);
    }

    /**
     * 重新加载已启用的存储配置
     */
    public void refreshConfiguredStorages() {
        if (configProvider == null) {
            log.warn("未配置StorageConfigProvider，跳过存储策略刷新");
            return;
        }

        configProvider.getAllEnabledConfigs().forEach(config -> {
            FileStorage storage = getStorage(config.getStorageType());
            if (storage != null) {
                try {
                    storage.init(config);
                    log.info("刷新文件存储策略: {} - {}", config.getStorageType(), config.getConfigName());
                } catch (Exception e) {
                    log.warn("刷新文件存储策略失败: {} - {}", config.getStorageType(), config.getConfigName(), e);
                }
            }
        });
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file, String storageType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        validateFilePolicy(file.getOriginalFilename(), storageType, file.getSize(), file.getContentType());
    }

    private void validateFileName(String fileName, String storageType) {
        validateFileName(fileName, storageType, null);
    }

    private void validateFileName(String fileName, String storageType, Long fileSize) {
        validateFilePolicy(fileName, storageType, fileSize, null);
    }

    private void validateFilePolicy(String fileName, String storageType, Long fileSize, String contentType) {
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }

        StorageConfig config = resolveValidationConfig(storageType);
        long maxFileSizeMb = config != null && config.getMaxFileSize() != null && config.getMaxFileSize() > 0
                ? config.getMaxFileSize()
                : DEFAULT_MAX_FILE_SIZE_MB;
        if (fileSize != null && fileSize >= 0) {
            long maxSize = maxFileSizeMb * 1024L * 1024L;
            if (fileSize > maxSize) {
                throw new RuntimeException("文件大小超过限制: " + maxFileSizeMb + "MB");
            }
        }

        String extension = normalizeExtension(FileUtil.getExtension(fileName));
        if (extension.isBlank()) {
            throw new RuntimeException("文件必须包含扩展名");
        }
        if (DANGEROUS_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("不支持上传高风险文件类型: " + extension);
        }

        Set<String> allowedTypes = resolveAllowedTypes(config);
        if (!allowedTypes.contains(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }

        validateMimeType(extension, contentType);
    }

    private StorageConfig resolveValidationConfig(String storageType) {
        if (configProvider == null) {
            return null;
        }
        if (storageType != null && !storageType.isBlank()) {
            return configProvider.getConfigByType(storageType);
        }
        return configProvider.getDefaultConfig();
    }

    private Set<String> resolveAllowedTypes(StorageConfig config) {
        if (config == null || config.getAllowedTypes() == null || config.getAllowedTypes().isBlank()) {
            throw new RuntimeException("文件存储配置未设置允许的文件类型");
        }
        Set<String> configuredTypes = config.getAllowedTypeList().stream()
                .map(this::normalizeExtension)
                .filter(type -> !type.isBlank())
                .filter(type -> !DANGEROUS_EXTENSIONS.contains(type))
                .collect(java.util.stream.Collectors.toSet());
        if (configuredTypes.isEmpty()) {
            throw new RuntimeException("文件存储配置未设置有效的允许文件类型");
        }
        return configuredTypes;
    }

    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private void validateMimeType(String extension, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String normalizedContentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (DANGEROUS_MIME_TYPES.contains(normalizedContentType)) {
            throw new RuntimeException("不支持上传高风险文件内容类型: " + normalizedContentType);
        }
        Set<String> expectedTypes = EXTENSION_MIME_TYPES.get(extension);
        if (expectedTypes == null || expectedTypes.isEmpty() || "application/octet-stream".equals(normalizedContentType)) {
            return;
        }
        if (!expectedTypes.contains(normalizedContentType)) {
            throw new RuntimeException("文件扩展名与内容类型不匹配: " + extension + " / " + normalizedContentType);
        }
    }

}
