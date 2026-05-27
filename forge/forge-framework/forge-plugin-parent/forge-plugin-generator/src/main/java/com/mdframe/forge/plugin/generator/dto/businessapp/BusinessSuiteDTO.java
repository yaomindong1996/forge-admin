package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务套件保存参数。
 */
@Data
public class BusinessSuiteDTO {

    private Long id;

    private String suiteCode;

    private String suiteName;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;
}
