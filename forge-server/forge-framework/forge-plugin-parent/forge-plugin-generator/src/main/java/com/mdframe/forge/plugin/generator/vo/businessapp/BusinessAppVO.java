package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问入口视图。
 */
@Data
public class BusinessAppVO {

    private Long id;

    private String appCode;

    private String appName;

    private String appType;

    private Long applicationId;

    private String suiteCode;

    private String suiteName;

    private String objectCode;

    private String objectName;

    private String entryMode;

    private String entryUrl;

    private String configKey;

    private String runtimeOpenMode;

    private String appMode;

    private Long menuResourceId;

    private Long adminMenuParentId;

    private Long adminMenuActualParentId;

    private Long suiteMenuResourceId;

    private String activeMenuKey;

    private Boolean adminMenuSyncEnabled;

    private Boolean suiteAsMenuParent;

    private Integer menuSort;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;

    private Long bindingCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
