package com.mdframe.forge.plugin.job.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务新增和修改参数。
 */
@Data
public class JobConfigSaveRequest {

    private Long id;

    private String jobName;

    private String jobGroup;

    private String description;

    private String executorBean;

    private String executorMethod;

    private String executorHandler;

    private String executorService;

    private String scheduleType;

    private String cronExpression;

    private LocalDateTime fireOnceTime;

    private String timezone;

    private String jobParam;

    private Integer status;

    private String executeMode;

    private String invokeMode;

    private String flowModelKey;

    private Integer flowModelVersion;

    private String concurrentPolicy;

    private String misfirePolicy;

    private Integer idempotentFlag;

    private Integer retryCount;

    private Integer alarmEnabled;

    private String alarmChannels;

    private String alarmRecipientUserIds;

    private String alarmEmail;

    private Integer version;
}
