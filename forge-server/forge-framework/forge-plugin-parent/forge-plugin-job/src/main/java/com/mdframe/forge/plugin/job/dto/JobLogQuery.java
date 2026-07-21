package com.mdframe.forge.plugin.job.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 定时任务日志分页查询参数。
 */
@Data
public class JobLogQuery {

    private Long jobConfigId;

    private String jobName;

    private String jobGroup;

    private Integer status;

    private String triggerType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
