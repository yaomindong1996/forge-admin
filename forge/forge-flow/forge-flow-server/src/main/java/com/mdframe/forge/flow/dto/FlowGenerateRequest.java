package com.mdframe.forge.flow.dto;

import lombok.Data;

@Data
public class FlowGenerateRequest {

    private String sessionId;
    private String description;
    private String modelId;
    private String flowModelId;
    private String modelKey;
    private String modelName;
    private String category;
    private String flowType;
    private String formType;
    private String currentBpmnXml;
    private String currentFormJson;
    private Long providerId;
    private String aiModelName;
    private Double temperature;
    private Integer maxTokens;
}
