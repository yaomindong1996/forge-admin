package com.mdframe.forge.plugin.job.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 最终失败告警所需的安全上下文，不包含任务参数或完整执行结果。
 */
@Data
public class JobFailureAlarmContextVO {

    private Long executionId;

    private Long jobConfigId;

    private String jobName;

    private String jobGroup;

    private LocalDateTime failureTime;

    private String exceptionSummary;

    private Integer alarmEnabled;

    private String alarmChannels;

    private String alarmRecipientUserIds;

    private String alarmEmail;
}
