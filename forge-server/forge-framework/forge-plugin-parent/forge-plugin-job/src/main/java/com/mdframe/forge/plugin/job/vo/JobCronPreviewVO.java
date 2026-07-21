package com.mdframe.forge.plugin.job.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron校验与执行时间预览。
 */
@Data
@Builder
public class JobCronPreviewVO {

    private String cronExpression;

    private String timezone;

    private String description;

    private List<LocalDateTime> nextFireTimes;
}
