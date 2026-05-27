package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务对象查询参数。
 */
@Data
public class BusinessObjectQueryDTO {

    private String keyword;

    private String suiteCode;

    private String objectCode;

    private String objectType;

    private String modelCode;

    private Integer status;
}
