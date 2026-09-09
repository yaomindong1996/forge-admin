package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

/**
 * 流程图连线执行状态。状态无法从历史级别推断时由上层标记能力不可用。
 */
@Data
public class ProcessSequenceFlowInfo {

    private String flowId;

    private String sourceRef;

    private String targetRef;

    /** 取值见 FlowDiagramStatus，已执行连线为 COMPLETED。 */
    private String status;
}
