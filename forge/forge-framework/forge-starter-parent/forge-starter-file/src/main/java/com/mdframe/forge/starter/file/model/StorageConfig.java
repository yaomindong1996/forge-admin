package com.mdframe.forge.starter.file.model;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * 存储策略配置
 */
@Data
public class StorageConfig {
    
    /**
     * 配置ID
     */
    private Long id;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 存储类型
     */
    private String storageType;
    
    /**
     * 是否默认策略
     */
    private Boolean isDefault;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 访问端点
     */
    private String endpoint;
    
    /**
     * 访问密钥ID
     */
    private String accessKey;
    
    /**
     * 访问密钥Secret
     */
    private String secretKey;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 基础路径
     */
    private String basePath;
    
    /**
     * 访问域名
     */
    private String domain;
    
    /**
     * 是否使用HTTPS
     */
    private Boolean useHttps;
    
    /**
     * 最大文件大小（MB）
     */
    private Integer maxFileSize;
    
    /**
     * 允许的文件类型（逗号分隔）
     */
    private String allowedTypes;
    
    /**
     * 排序
     */
    private Integer orderNum;
    
    /**
     * 扩展配置（JSON格式）
     */
    private String extraConfig;
    
    /**
     * 获取允许的文件类型列表
     */
    public List<String> getAllowedTypeList() {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return List.of();
        }
        return List.of(allowedTypes.split(","));
    }
}
