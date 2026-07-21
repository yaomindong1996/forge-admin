package com.mdframe.forge.plugin.job.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定时任务日志安全详情，仅补充经过脱敏和限长的摘要。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobLogDetailVO extends JobLogVO {

    private String fireInstanceId;

    private String processInstanceId;

    private String resultSummary;

    private String exceptionSummary;
}
