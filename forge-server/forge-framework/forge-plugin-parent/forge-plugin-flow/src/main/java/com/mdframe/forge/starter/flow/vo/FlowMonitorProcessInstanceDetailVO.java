package com.mdframe.forge.starter.flow.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理员流程实例详情。 */
@Data
public class FlowMonitorProcessInstanceDetailVO {

    private String id;
    private String processName;
    private String processDefKey;
    private String processDefName;
    private String initiatorName;
    private String initiatorId;
    private String status;
    private LocalDateTime startTime;
    private String businessKey;
    private LocalDateTime endTime;
    private String deptName;
}
