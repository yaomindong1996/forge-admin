package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 业务应用保存参数。
 */
@Data
public class BusinessApplicationDTO {

    private Long id;

    private String applicationCode;

    private String applicationName;

    private String suiteCode;

    private String icon;

    private String description;

    private Integer status;

    private String options;
}
