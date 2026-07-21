package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobOpenApiExecutionVO {

    private Long id;
    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String triggerType;
    private Integer status;
    private LocalDateTime triggerTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private Integer retryCount;
}
