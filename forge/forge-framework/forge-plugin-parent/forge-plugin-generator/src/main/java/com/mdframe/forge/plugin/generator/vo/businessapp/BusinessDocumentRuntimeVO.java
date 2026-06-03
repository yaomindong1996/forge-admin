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

    private List<RuntimeActionVO> runtimeActions = new ArrayList<>();

    private String nextAction;

    private String message;

    @Data
    public static class RuntimeActionVO {

        private String key;

        private String label;

        private String type;

        private String actionType;

        private Boolean visible;

        private Boolean disabled;

        private String disabledReason;

        private String objectCode;

        private Long recordId;
    }
}
