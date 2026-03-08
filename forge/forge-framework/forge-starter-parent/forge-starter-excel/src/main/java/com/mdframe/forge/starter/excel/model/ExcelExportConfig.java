package com.mdframe.forge.starter.excel.model;

import lombok.Builder;
import lombok.Data;

/**
 * Excel导出配置
 */
@Data
@Builder
public class ExcelExportConfig {

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 是否自动翻译字典
     */
    @Builder.Default
    private Boolean autoTrans = true;

    /**
     * 是否过滤null
     */
    @Builder.Default
    private Boolean filterNull = false;

    /**
     * 是否从数据库读取配置
     */
    @Builder.Default
    private Boolean useDbConfig = false;

    /**
     * 数据库配置标识（如：user_export）
     */
    private String configKey;
}
