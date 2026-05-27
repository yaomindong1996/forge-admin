package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务对象关系保存参数。
 */
@Data
public class BusinessObjectRelationDTO {

    private Long id;

    private String suiteCode;

    private String sourceObjectCode;

    private String targetObjectCode;

    private String relationType;

    private String relationName;

    private String sourceFieldCode;

    private String targetFieldCode;

    private String relationConfig;

    private String description;

    private Integer status;

    private Integer sortOrder;
}
