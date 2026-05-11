package com.mdframe.forge.plugin.external.vo;

import lombok.Data;

@Data
public class ExternalApiLogSummary {

    private Long totalCount;

    private Long successCount;

    private Long failureCount;

    private Long debugCount;

    private Double successRate;

    private Double avgDurationMs;

    private Long maxDurationMs;
}
