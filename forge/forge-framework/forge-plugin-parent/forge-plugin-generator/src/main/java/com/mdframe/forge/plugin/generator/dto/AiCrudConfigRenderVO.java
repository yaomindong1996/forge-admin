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
}
