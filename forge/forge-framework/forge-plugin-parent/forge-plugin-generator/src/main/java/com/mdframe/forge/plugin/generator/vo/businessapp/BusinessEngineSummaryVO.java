package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 引擎运行状态汇总 VO
 */
@Data
public class BusinessEngineSummaryVO {

    /**
     * 引擎类型：APPROVAL / REPORT / MESSAGE / PERMISSION / TRIGGER
     */
    private String engineType;

    /**
     * 引擎名称
     */
    private String engineName;

    /**
     * 引擎图标
     */
    private String engineIcon;

    /**
     * 总接入数
     */
    private Integer totalCount;

    /**
     * 可运行数
     */
    private Integer runnableCount;

    /**
     * 待配置数
     */
    private Integer pendingCount;

    /**
     * 异常数
     */
    private Integer errorCount;

    /**
     * 整体状态：RUNNABLE / PARTIAL / MISSING
     */
    private String status;

    /**
     * 状态文案
     */
    private String statusLabel;

    /**
     * 详细说明
     */
    private String message;
}
