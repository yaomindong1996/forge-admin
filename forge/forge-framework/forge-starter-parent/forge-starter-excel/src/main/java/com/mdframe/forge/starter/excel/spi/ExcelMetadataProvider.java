package com.mdframe.forge.starter.excel.spi;

import com.mdframe.forge.starter.excel.model.ExcelExportMetadata;

/**
 * Excel元数据提供者SPI
 * 由业务模块实现，从数据库读取导出配置
 */
public interface ExcelMetadataProvider {

    /**
     * 根据配置键获取导出元数据
     *
     * @param configKey 配置键
     * @return 导出元数据
     */
    ExcelExportMetadata getMetadata(String configKey);
}
