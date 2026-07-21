package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobOpenApiSummaryVO {

    private Long id;
    private String jobName;
    private String jobGroup;
    private String description;
    private String scheduleType;
    private String timezone;
    private Integer status;
    private String syncStatus;
    private Integer lastExecutionStatus;
    private LocalDateTime lastExecutionTime;
}
