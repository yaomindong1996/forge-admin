package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 流程监控每日新增和完成数量。
 */
@Data
public class FlowMonitorDailyStatVO {

    private LocalDate statDate;

    private Long createdCount;

    private Long completedCount;
}
