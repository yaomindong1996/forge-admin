package com.mdframe.forge.starter.excel.spi;

import com.mdframe.forge.starter.excel.model.ExcelColumnConfig;

import java.util.List;

/**
 * Excel配置提供者SPI
 * 由业务模块实现，从数据库读取Excel导出配置
 */
public interface ExcelConfigProvider {

    /**
     * 根据配置键获取列配置
     *
     * @param configKey 配置键
     * @return 列配置列表
     */
    List<ExcelColumnConfig> getColumnConfigs(String configKey);
}
