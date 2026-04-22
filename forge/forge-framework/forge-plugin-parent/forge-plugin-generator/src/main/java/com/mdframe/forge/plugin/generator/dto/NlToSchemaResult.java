package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

import java.util.List;

@Data
public class NlToSchemaResult {

    private String tableName;
    private String tableComment;
    private List<SchemaColumn> columns;
    private String rawResponse;
}
