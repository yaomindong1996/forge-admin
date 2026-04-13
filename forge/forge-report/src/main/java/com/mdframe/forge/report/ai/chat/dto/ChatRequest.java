package com.mdframe.forge.report.ai.chat.dto;

import lombok.Data;

/**
 * AI 聊天请求
 */
@Data
public class ChatRequest {
    private String content;
    private String agentCode;
    private String sessionId;
}
