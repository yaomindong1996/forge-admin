package com.mdframe.forge.starter.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.file")
public class FileStorageProperties {
    
    /**
     * 是否启用通用文件API
     */
    private Boolean enableGenericApi = true;
    
    /**
     * 默认存储类型
     */
    private String defaultStorageType = "local";
}
