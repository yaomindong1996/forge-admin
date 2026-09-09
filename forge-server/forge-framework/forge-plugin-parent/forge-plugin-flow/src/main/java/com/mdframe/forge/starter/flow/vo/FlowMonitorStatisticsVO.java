package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

/**
 * 流程监控首页统计结果。
 *
 * <p>统计不可用时数值字段保持 {@code null}，由 {@link #degraded} 和
 * {@link #errorCode} 区分“暂无数据”和“后端降级”，避免前端把故障显示成 0。</p>
 */
@Data
public class FlowMonitorStatisticsVO {

    private Long runningInstances;

    private Long pendingTasks;

    private Long todayCompleted;

    private Long timeoutTasks;

    private Boolean degraded;

    private String errorCode;
}
