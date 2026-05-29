package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务字段设计参数。
 */
@Data
public class BusinessFieldDTO {

    private String fieldName;

    private String fieldCode;

    private String columnName;

    /** 业务字段类型：TEXT/MONEY/DATE/DICT/REFERENCE 等。 */
    private String fieldType;

    private String dataType;

    private Integer length;

    private Integer precision;

    private Boolean required;

    private Object defaultValue;

    private Boolean searchable;

    private Boolean listVisible;

    private Boolean formVisible;

    private Boolean importable;

    private Boolean exportable;

    private String componentType;

    private String queryType;

    private String dictType;

    private String sensitiveType;

    private String encryptAlgorithm;

    private Boolean sortable;

    private Boolean systemField;

    private Boolean readonly;

    private String fieldStatus;

    private String referenceObjectCode;

    private String referenceDisplayField;

    private String placeholder;

    private String remark;

    private Integer sortOrder;

    private Map<String, Object> basicProps = new LinkedHashMap<>();

    private Map<String, Object> advancedProps = new LinkedHashMap<>();
}
