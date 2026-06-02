package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务单据运行态视图。
 */
@Data
public class BusinessDocumentRuntimeVO {

    private Boolean documentEnabled;

    private String documentStatus;

    private String documentStatusLabel;

    private String businessKey;

    private String flowStatus;

    private String processInstanceId;

    private List<String> availableActions = new ArrayList<>();

    private String nextAction;

    private String message;
}
