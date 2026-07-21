package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个任务的运行概览。
 */
@Data
public class JobOverviewVO {

    private Long jobConfigId;

    private String jobName;

    private String jobGroup;

    private LocalDateTime nextFireTime;

    private Integer lastExecutionStatus;

    private LocalDateTime lastExecutionTime;

    private Integer consecutiveFailures;

    private List<JobLogVO> recentExecutions;
}
