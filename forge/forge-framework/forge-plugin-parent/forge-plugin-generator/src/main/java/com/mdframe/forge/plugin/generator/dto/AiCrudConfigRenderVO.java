package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class AiCrudConfigRenderVO {

    private String configKey;
    private String tableName;
    private String tableComment;
    private Object searchSchema;
    private Object columnsSchema;
    private Object editSchema;
    private Object apiConfig;
    private Object options;
    private String rowKey;
    private String modalType;
    private String modalWidth;
    private Integer editGridCols;
    private Integer searchGridCols;
    private Object dictConfig;
    private Object desensitizeConfig;
    private Object encryptConfig;
    private Object transConfig;
    /** 页面模板类型 */
    private String layoutType;
    /** 模板的默认配置（从 ai_page_template.default_config 合并而来） */
    private Object templateDefaultConfig;
}
