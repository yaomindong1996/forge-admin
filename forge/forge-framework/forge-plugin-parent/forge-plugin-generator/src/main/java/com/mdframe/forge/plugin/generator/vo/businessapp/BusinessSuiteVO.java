package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务套件视图。
 */
@Data
public class BusinessSuiteVO {

    private Long id;

    private String suiteCode;

    private String suiteName;

    private String icon;

    private String description;

    private Integer status;

    private Integer sortOrder;

    private String options;

    private Long objectCount;

    private Long appCount;

    private Long enabledAppCount;

    private LocalDateTime latestUpdateTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
