package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务日志列表视图，不包含任务参数、结果和异常正文。
 */
@Data
public class JobLogVO {

    private Long id;

    private Long jobConfigId;

    private String jobName;

    private String jobGroup;

    private String executorHandler;

    private LocalDateTime triggerTime;

    private String triggerType;

    private LocalDateTime scheduledFireTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private Integer status;

    private Integer retryCount;
}
