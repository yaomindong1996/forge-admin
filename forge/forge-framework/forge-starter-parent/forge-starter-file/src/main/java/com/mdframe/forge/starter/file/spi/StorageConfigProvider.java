package com.mdframe.forge.starter.file.spi;

import com.mdframe.forge.starter.file.model.StorageConfig;

import java.util.List;

/**
 * 存储配置提供者SPI
 * 由业务模块实现，从数据库读取存储配置
 */
public interface StorageConfigProvider {
    
    /**
     * 获取默认存储配置
     */
    StorageConfig getDefaultConfig();
    
    /**
     * 根据存储类型获取配置
     */
    StorageConfig getConfigByType(String storageType);
    
    /**
     * 获取所有启用的存储配置
     */
    List<StorageConfig> getAllEnabledConfigs();
    
    /**
     * 刷新配置缓存
     */
    void refreshConfig();
}
