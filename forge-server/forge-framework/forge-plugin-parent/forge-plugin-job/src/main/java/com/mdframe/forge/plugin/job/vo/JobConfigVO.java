package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时任务管理视图。
 */
@Data
public class JobConfigVO {

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

    private String flowDeploymentId;

    private String flowProcessDefinitionId;

    private String concurrentPolicy;

    private String misfirePolicy;

    private Integer idempotentFlag;

    private String executionSummary;

    private String scheduleSummary;

    private LocalDateTime nextFireTime;

    private Integer lastExecutionStatus;

    private LocalDateTime lastExecutionTime;

    private Integer retryCount;

    private Integer consecutiveFailures;

    private Integer alarmEnabled;

    private String alarmChannels;

    private String alarmRecipientUserIds;

    private String alarmEmail;

    private String syncStatus;

    private String syncError;

    private LocalDateTime syncTime;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
