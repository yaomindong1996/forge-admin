package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务套件汇总视图。
 */
@Data
public class BusinessSuiteSummaryVO {

    private Long id;

    private String suiteCode;

    private String suiteName;

    private String icon;

    private String description;

    private Integer status;

    private Long objectCount;

    private Long appCount;

    private Long enabledAppCount;

    private LocalDateTime latestUpdateTime;
}
