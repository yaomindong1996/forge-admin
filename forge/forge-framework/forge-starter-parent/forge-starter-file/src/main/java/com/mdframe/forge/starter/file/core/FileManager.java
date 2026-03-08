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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件管理器
 * 统一文件上传、下载、删除等操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileManager {
    
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
     * 上传文件（使用默认存储策略）
     */
    public FileMetadata upload(MultipartFile file, String businessType, String businessId) {
        if (configProvider == null) {
            throw new RuntimeException("未配置StorageConfigProvider");
        }
        
        StorageConfig config = configProvider.getDefaultConfig();
        return upload(file, businessType, businessId, config.getStorageType());
    }
    
    /**
     * 上传文件（指定存储策略）
     */
    public FileMetadata upload(MultipartFile file, String businessType, String businessId, String storageType) {
        // 验证文件
        validateFile(file, storageType);
        
        // 秒传检查
        String md5 = FileUtil.calculateMd5(file);
        if (metadataPersistence != null) {
            FileMetadata existing = metadataPersistence.getByMd5(md5);
            if (existing != null) {
                log.info("文件秒传: md5={}", md5);
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
     * 验证文件
     */
    private void validateFile(MultipartFile file, String storageType) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        if (configProvider == null) {
            return;
        }
        
        StorageConfig config = configProvider.getConfigByType(storageType);
        if (config == null) {
            return;
        }
        
        // 验证文件大小
        if (config.getMaxFileSize() != null) {
            long maxSize = config.getMaxFileSize() * 1024L * 1024L;
            if (file.getSize() > maxSize) {
                throw new RuntimeException("文件大小超过限制: " + config.getMaxFileSize() + "MB");
            }
        }
        
        // 验证文件类型
        List<String> allowedTypes = config.getAllowedTypeList();
        if (!allowedTypes.isEmpty()) {
            String extension = FileUtil.getExtension(file.getOriginalFilename());
            if (!allowedTypes.contains(extension.toLowerCase())) {
                throw new RuntimeException("不支持的文件类型: " + extension);
            }
        }
    }
}
