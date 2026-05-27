package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务对象关系视图。
 */
@Data
public class BusinessObjectRelationVO {

    private Long id;

    private String suiteCode;

    private String sourceObjectCode;

    private String sourceObjectName;

    private String targetObjectCode;

    private String targetObjectName;

    private String relationType;

    private String relationName;

    private String sourceFieldCode;

    private String targetFieldCode;

    private String relationConfig;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
