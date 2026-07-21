package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 连续失败任务摘要。
 */
@Data
public class JobFailureTaskVO {

    private Long jobConfigId;

    private String jobName;

    private String jobGroup;

    private Integer consecutiveFailures;

    private LocalDateTime lastFailureTime;
}
