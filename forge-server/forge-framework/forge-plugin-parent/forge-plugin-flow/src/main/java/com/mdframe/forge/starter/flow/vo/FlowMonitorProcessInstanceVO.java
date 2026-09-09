package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员流程监控列表中的流程实例。 */
@Data
public class FlowMonitorProcessInstanceVO {

    private String id;
    private String processName;
    private String processDefKey;
    private String processDefName;
    private String initiatorName;
    private String initiatorId;
    private String status;
    private LocalDateTime startTime;
    private String businessKey;
    private String duration;
    private String currentNode;
    private String currentAssignee;
}
