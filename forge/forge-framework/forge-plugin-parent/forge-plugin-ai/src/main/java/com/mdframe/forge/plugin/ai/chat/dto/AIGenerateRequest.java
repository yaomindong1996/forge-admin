package com.mdframe.forge.plugin.ai.chat.dto;

import lombok.Data;

/**
 * AI 生成请求
 */
@Data
public class AIGenerateRequest {
    private String prompt;
    private String style;
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String componentCatalog;
    private String projectName;
    private String canvasContext;
    private Long providerId;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
}
