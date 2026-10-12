package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.system.entity.SysFileStorageConfig;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 文件存储配置Service
 */
public interface ISysFileStorageConfigService extends IService<SysFileStorageConfig> {
    
    /**
     * 分页查询
     */
    Page<SysFileStorageConfig> page(PageQuery query, SysFileStorageConfig condition);
    
    /**
     * 设置默认配置
     */
    void setDefault(Long id);
    
    /**
     * 启用/禁用配置
     */
    void updateEnabled(Long id, Boolean enabled);
    
    /**
     * 测试连接
     */
    boolean testConnection(Long id);

    /**
     * 创建存储桶
     */
    boolean createBucket(Long id);

    /**
     * 查询默认配置
     */
    SysFileStorageConfig getDefaultConfig();

    /**
     * 查询启用的存储配置选项
     */
    List<SysFileStorageConfig> listEnabledOptions();
}
