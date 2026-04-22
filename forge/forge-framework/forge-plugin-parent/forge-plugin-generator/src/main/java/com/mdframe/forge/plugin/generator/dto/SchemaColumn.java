package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class SchemaColumn {

    private String columnName;
    private String columnComment;
    private String columnType;
    private String javaType;
    private String javaField;
    private String htmlType;
    private String dictType;
    private Integer isRequired;
    private String validateRule;
}
