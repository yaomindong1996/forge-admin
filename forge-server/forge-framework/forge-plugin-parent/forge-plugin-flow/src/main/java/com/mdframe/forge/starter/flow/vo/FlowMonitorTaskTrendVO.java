package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.util.List;

/** 流程任务近七日趋势。 */
@Data
public class FlowMonitorTaskTrendVO {
    private List<String> dates;
    private List<Long> created;
    private List<Long> completed;
    private boolean degraded;
    private String errorCode;
}
