package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 访问入口保存参数。
 */
@Data
public class BusinessAppDTO {

    private Long id;

    private String appCode;

    private String appName;

    private String appType;

    private Long applicationId;

    private String suiteCode;

    private String objectCode;

    private String entryMode;

    private String entryUrl;

    private String configKey;

    private String runtimeOpenMode;

    /** DYNAMIC_RENDER/CODE_DOWNLOAD，仅 RUNTIME 入口生效。 */
    private String appMode;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;
}
