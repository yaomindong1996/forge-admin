package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务单据流程运行态视图。
 */
@Data
public class BusinessFlowRuntimeVO {

    private Long linkId;

    private String objectCode;

    private Long recordId;

    private String businessKey;

    private String flowModelKey;

    private String processInstanceId;

    private String flowStatus;

    private String result;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String message;
}
