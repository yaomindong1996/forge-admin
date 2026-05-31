package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务字段视图。
 */
@Data
public class BusinessFieldVO {

    private String fieldName;

    private String fieldCode;

    private String columnName;

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

    private Integer sortOrder;

    private Integer width;

    private String remark;

    private String referenceStatus;

    private Boolean canDelete;

    private List<String> referencedBy = new ArrayList<>();

    private String templateCode;

    private String templateName;

    private Map<String, Object> fieldBinding = new LinkedHashMap<>();

    private Map<String, Object> basicProps = new LinkedHashMap<>();

    private Map<String, Object> advancedProps = new LinkedHashMap<>();
}
