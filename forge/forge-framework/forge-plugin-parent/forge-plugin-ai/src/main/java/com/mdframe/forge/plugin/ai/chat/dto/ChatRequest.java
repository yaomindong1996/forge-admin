package com.mdframe.forge.plugin.ai.chat.dto;

import lombok.Data;

/**
 * AI 聊天请求
 */
@Data
public class ChatRequest {
    private String content;
    private String agentCode;
    private String sessionId;
    private String projectName;
    private String canvasContext;
    private Long providerId;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
}
