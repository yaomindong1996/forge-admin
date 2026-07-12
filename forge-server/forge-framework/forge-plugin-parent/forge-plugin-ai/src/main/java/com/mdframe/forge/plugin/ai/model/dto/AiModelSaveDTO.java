package com.mdframe.forge.plugin.ai.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiModelSaveDTO {
    private Long id;
    private Long providerId;
    private String modelType;
    private String modelId;
    private String modelName;
    private String description;
    private Integer maxTokens;
    private Integer contextWindow;
    private Long inputPricePerMillionCent;
    private Long outputPricePerMillionCent;
    private String icon;
    private String isDefault;
    private String status;
    private Integer sortOrder;
    private String remark;
    private List<String> capabilityCodes;
}
