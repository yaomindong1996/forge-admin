package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务对象保存参数。
 */
@Data
public class BusinessObjectDTO {

    private Long id;

    private String suiteCode;

    private String objectCode;

    private String objectName;

    private String objectType;

    private Long modelId;

    private String modelCode;

    private String displayField;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;
}
