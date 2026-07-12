package com.mdframe.forge.plugin.ai.routing;
import lombok.Data;

@Data
public class AiModelRouteCandidate {
    private Long targetId;
    private Long targetTenantId;
    private Long policyId;
    private Long modelPk;
    private Long modelTenantId;
    private Long providerPk;
    private Long providerTenantId;
    private String providerName;
    private String providerType;
    private String adapterCode;
    private String apiKey;
    private String baseUrl;
    private String providerStatus;
    private String modelId;
    private String modelName;
    private String modelType;
    private String modelStatus;
    private Integer maxTokens;
    private Integer contextWindow;
    private Long inputPricePerMillionCent;
    private Long outputPricePerMillionCent;
    private Integer priority;
    private String capabilityCodes;
}
