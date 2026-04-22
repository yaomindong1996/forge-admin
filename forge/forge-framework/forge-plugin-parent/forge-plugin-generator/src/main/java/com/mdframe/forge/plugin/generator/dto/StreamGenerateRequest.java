package com.mdframe.forge.plugin.generator.dto;

import lombok.Data;

@Data
public class StreamGenerateRequest {

    private String sessionId;
    private String configKey;
    private String tableName;
    private String description;
    private Long providerId;
    private Long modelId;
    private Double temperature;
    private Integer maxTokens;
    private String existingSearchSchema;
    private String existingColumnsSchema;
    private String existingEditSchema;
    private String existingApiConfig;
}