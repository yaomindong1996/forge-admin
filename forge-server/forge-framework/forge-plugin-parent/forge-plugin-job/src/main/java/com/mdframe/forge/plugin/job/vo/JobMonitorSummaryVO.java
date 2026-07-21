package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务近 24 小时轻量监控摘要。
 */
@Data
public class JobMonitorSummaryVO {

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    private Long totalCount;

    private Long successCount;

    private Long failedCount;

    private Long runningCount;

    private Long skippedCount;

    private Long acceptedCount;

    private BigDecimal successRate;

    private BigDecimal failureRate;

    private Integer consecutiveFailureTaskCount;

    private List<JobFailureTaskVO> failureTasks;
}
