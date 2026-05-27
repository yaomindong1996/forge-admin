package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用入口视图。
 */
@Data
public class BusinessAppVO {

    private Long id;

    private String appCode;

    private String appName;

    private String appType;

    private String suiteCode;

    private String suiteName;

    private String objectCode;

    private String objectName;

    private String entryMode;

    private String entryUrl;

    private String configKey;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;

    private Long bindingCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
