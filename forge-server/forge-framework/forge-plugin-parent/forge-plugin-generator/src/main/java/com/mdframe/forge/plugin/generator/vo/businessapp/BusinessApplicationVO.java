package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务应用视图。
 */
@Data
public class BusinessApplicationVO {

    private Long id;

    private String applicationCode;

    private String applicationName;

    private String suiteCode;

    private String suiteName;

    private String icon;

    private String description;

    private Integer status;

    private String designStatus;

    private Integer lastPublishVersion;

    private LocalDateTime lastPublishTime;

    private String options;

    private Long objectCount;

    private Long entryCount;

    private Long activeEntryCount;

    private Long flowCount;

    private Long extensionCount;

    private Long problemCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
