package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 业务字段、字段编码与数据库列的映射视图。
 */
@Data
public class BusinessObjectTableFieldMappingVO {

    private String businessName;

    private String fieldCode;

    private String columnName;

    private String dataType;

    private Integer length;

    private Integer precision;

    private Boolean required;

    private Object defaultValue;

    private String componentType;

    private Boolean systemField;

    private Boolean readonly;

    private Boolean configuredIndex;

    private String databaseType;

    private Boolean databaseNullable;

    private Object databaseDefaultValue;

    private Boolean databaseIndexed;

    /** IN_SYNC/MISSING_DATABASE_COLUMN/TYPE_MISMATCH/UNMAPPED_DATABASE_COLUMN。 */
    private String syncStatus;
}
