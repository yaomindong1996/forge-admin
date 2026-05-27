package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务套件查询参数。
 */
@Data
public class BusinessSuiteQueryDTO {

    private String keyword;

    private String suiteCode;

    private Integer status;
}
