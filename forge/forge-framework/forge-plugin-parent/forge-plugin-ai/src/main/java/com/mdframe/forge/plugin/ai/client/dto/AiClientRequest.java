package com.mdframe.forge.plugin.ai.client.dto;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Map;

@Data
public class AiClientRequest {

    private String agentCode;
    private String message;
    private String userInput;
    private Long providerId;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Map<String, String> contextVars;
    private String sessionId;

    public String getUserInputOrMessage() {
        return StringUtils.hasText(userInput) ? userInput : message;
    }
}
