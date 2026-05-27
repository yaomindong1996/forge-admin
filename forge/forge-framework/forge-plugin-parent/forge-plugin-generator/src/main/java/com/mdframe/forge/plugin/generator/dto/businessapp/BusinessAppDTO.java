package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 应用入口保存参数。
 */
@Data
public class BusinessAppDTO {

    private Long id;

    private String appCode;

    private String appName;

    private String appType;

    private String suiteCode;

    private String objectCode;

    private String entryMode;

    private String entryUrl;

    private String configKey;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;
}
