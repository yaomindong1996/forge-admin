package com.mdframe.forge.starter.excel.model;

import lombok.Data;

/**
 * 通用导出配置（数据库表模型）
 * 定义导出的数据源、字段映射等元数据
 */
@Data
public class ExcelExportMetadata {

    /**
     * 配置键（唯一标识，如：user_list_export）
     */
    private String configKey;

    /**
     * 导出名称
     */
    private String exportName;

    /**
     * Sheet名称
     */
    private String sheetName;

    /**
     * 文件名模板（支持占位符：{date}、{time}）
     */
    private String fileNameTemplate;

    /**
     * 数据源Bean名称（如：userService）
     */
    private String dataSourceBean;

    /**
     * 数据查询方法名（如：list、page）
     */
    private String queryMethod;

    /**
     * 是否自动翻译字典
     */
    private Boolean autoTrans;

    /**
     * 是否分页查询
     */
    private Boolean pageable;

    /**
     * 最大导出条数（防止大数据量）
     */
    private Integer maxRows;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向（ASC/DESC）
     */
    private String sortOrder;

    /**
     * 状态（1-启用，0-禁用）
     */
    private Integer status;
}
