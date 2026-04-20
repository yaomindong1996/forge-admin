package com.mdframe.forge.plugin.ai.client.dto;

import lombok.Data;

@Data
public class AiClientResponse {

    private String content;
    private boolean fallback;
    private String fallbackReason;
    private String sessionId;

    public static AiClientResponse success(String content, String sessionId) {
        AiClientResponse response = new AiClientResponse();
        response.setContent(content);
        response.setFallback(false);
        response.setSessionId(sessionId);
        return response;
    }

    public static AiClientResponse fallback(String content, AiFallbackReason reason, String sessionId) {
        AiClientResponse response = new AiClientResponse();
        response.setContent(content);
        response.setFallback(true);
        response.setFallbackReason(reason.name());
        response.setSessionId(sessionId);
        return response;
    }
}
