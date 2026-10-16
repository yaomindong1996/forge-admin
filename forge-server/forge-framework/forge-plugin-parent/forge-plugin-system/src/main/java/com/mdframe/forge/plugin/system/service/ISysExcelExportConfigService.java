package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import com.mdframe.forge.starter.core.domain.PageQuery;

/**
 * Excel导出配置Service
 */
public interface ISysExcelExportConfigService extends IService<SysExcelExportConfig> {
    
    /**
     * 分页查询
     */
    Page<SysExcelExportConfig> page(PageQuery query, SysExcelExportConfig condition);

    /**
     * 保存配置
     */
    void saveConfig(SysExcelExportConfig config);

    /**
     * 更新配置
     */
    void updateConfig(SysExcelExportConfig config);
    
    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 复制配置
     */
    SysExcelExportConfig copyConfig(Long id, String newConfigKey);
}
