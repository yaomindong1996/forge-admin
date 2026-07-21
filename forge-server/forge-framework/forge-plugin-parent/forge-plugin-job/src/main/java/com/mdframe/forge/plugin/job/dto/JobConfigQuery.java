package com.mdframe.forge.plugin.job.dto;

import lombok.Data;

/**
 * 定时任务分页查询参数。
 */
@Data
public class JobConfigQuery {

    private String jobName;

    private String jobGroup;

    private String executeMode;

    private String invokeMode;

    private String scheduleType;

    private Integer status;

    private String syncStatus;
}
