package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务指标 VO。
 */
@Data
public class BusinessStatsMetricVO {

    private String metricCode;

    private String metricName;

    /** COUNT/SUM/GROUP/TREND */
    private String metricType;

    private String field;

    private Object value;

    private String unit;

    private List<Map<String, Object>> items = new ArrayList<>();
}
