package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务日志导出白名单视图。
 */
@Data
public class JobLogExportVO {

    private String jobName;

    private String jobGroup;

    private String executorHandler;

    private String triggerType;

    private Integer status;

    private LocalDateTime scheduledFireTime;

    private LocalDateTime triggerTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private Integer retryCount;
}
