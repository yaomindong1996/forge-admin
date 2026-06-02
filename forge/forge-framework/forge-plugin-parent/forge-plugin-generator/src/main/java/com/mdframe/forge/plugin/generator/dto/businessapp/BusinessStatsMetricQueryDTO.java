package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 业务指标查询 DTO。
 */
@Data
public class BusinessStatsMetricQueryDTO {

    /** 指标类型：OVERVIEW/GROUP/SUM/FLOW_RESULT */
    private List<String> metricTypes;

    /** 通用分组字段 */
    private String groupField;

    /** 状态分布字段 */
    private String statusField;

    /** 阶段分布字段 */
    private String stageField;

    /** 金额汇总字段，金额单位保持数据库分值 */
    private String amountField;

    /** 趋势粒度：day/week/month */
    private String period;

    /** 趋势回看天数 */
    private Integer days;

    /** 是否包含流程结果分布 */
    private Boolean includeFlowResult;
}
