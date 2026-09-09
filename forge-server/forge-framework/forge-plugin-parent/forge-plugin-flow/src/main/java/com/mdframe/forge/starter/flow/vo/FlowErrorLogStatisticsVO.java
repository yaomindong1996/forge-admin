package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/** 流程错误日志统计。 */
@Data
public class FlowErrorLogStatisticsVO {
    private long total;
    private long unresolved;
    private long retried;
    private long retryFailed;
    private boolean degraded;
    private String errorCode;
}
