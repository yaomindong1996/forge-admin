package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class SchemaGenerateResult {

    private String configKey;
    private String tableName;
    private String tableComment;
    private String searchSchema;
    private String columnsSchema;
    private String editSchema;
    private String apiConfig;
}
