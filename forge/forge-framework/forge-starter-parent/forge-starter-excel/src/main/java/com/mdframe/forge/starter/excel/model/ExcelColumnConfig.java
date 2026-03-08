package com.mdframe.forge.starter.excel.model;

import lombok.Data;

/**
 * Excel列配置（从数据库读取）
 */
@Data
public class ExcelColumnConfig {

    /**
     * 配置键（如：user_export）
     */
    private String configKey;

    /**
     * 字段名
     */
    private String fieldName;

    /**
     * 列名（表头）
     */
    private String columnName;

    /**
     * 列宽
     */
    private Integer width;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * 是否导出
     */
    private Boolean export;

    /**
     * 日期格式
     */
    private String dateFormat;

    /**
     * 数字格式
     */
    private String numberFormat;

    /**
     * 字典类型
     */
    private String dictType;
}
