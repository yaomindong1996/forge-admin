package com.mdframe.forge.plugin.ai.chat.service;

import com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest;
import com.mdframe.forge.plugin.ai.client.AiClient;
import com.mdframe.forge.plugin.ai.client.dto.AiClientRequest;
import com.mdframe.forge.plugin.ai.client.dto.AiClientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String DASHBOARD_AGENT_CODE = "dashboard_generator";

    private final AiClient aiClient;

    public String generateDashboard(AIGenerateRequest request) {
        AiClientRequest clientRequest = new AiClientRequest();
        clientRequest.setAgentCode(DASHBOARD_AGENT_CODE);
        clientRequest.setMessage(buildDashboardPrompt(request));
        clientRequest.setProviderId(request.getProviderId());
        clientRequest.setModelName(request.getModelName());
        clientRequest.setTemperature(request.getTemperature());
        clientRequest.setMaxTokens(request.getMaxTokens());
        clientRequest.setContextVars(buildDashboardContextVars(request));

        AiClientResponse response = aiClient.call(clientRequest);
        if (response.isFallback()) {
            throw new RuntimeException("AI 生成失败: " + response.getFallbackReason());
        }
        return response.getContent();
    }

    public Flux<String> generateDashboardStream(AIGenerateRequest request) {
        AiClientRequest clientRequest = new AiClientRequest();
        clientRequest.setAgentCode(DASHBOARD_AGENT_CODE);
        clientRequest.setMessage(buildDashboardPrompt(request));
        clientRequest.setUserInput(request.getPrompt());
        clientRequest.setProviderId(request.getProviderId());
        clientRequest.setModelName(request.getModelName());
        clientRequest.setTemperature(request.getTemperature());
        clientRequest.setMaxTokens(request.getMaxTokens());
        clientRequest.setContextVars(buildDashboardContextVars(request));
        clientRequest.setSessionId(request.getSessionId());

        return aiClient.stream(clientRequest);
    }

    public Flux<String> chatStream(String content, String agentCode, String sessionId, Long userId,
                                   Long providerId, String modelName, Double temperature, Integer maxTokens,
                                   String projectName, String canvasContext) {
        String effectiveAgentCode = StringUtils.hasText(agentCode) ? agentCode : DASHBOARD_AGENT_CODE;
        String userPrompt = buildChatUserPrompt(content, projectName, canvasContext);

        AiClientRequest clientRequest = new AiClientRequest();
        clientRequest.setAgentCode(effectiveAgentCode);
        clientRequest.setMessage(userPrompt);
        clientRequest.setMessage(content);
        clientRequest.setProviderId(providerId);
        clientRequest.setModelName(modelName);
        clientRequest.setTemperature(temperature);
        clientRequest.setMaxTokens(maxTokens);
        clientRequest.setSessionId(sessionId);

        return aiClient.stream(clientRequest);
    }

    private String buildDashboardPrompt(AIGenerateRequest request) {
        StringBuilder userPrompt = new StringBuilder(request.getPrompt());
        if (StringUtils.hasText(request.getProjectName())) {
            userPrompt.append("\n\n当前项目：").append(request.getProjectName());
        }
        if (StringUtils.hasText(request.getCanvasContext())) {
            userPrompt.append("\n\n当前画布已有内容（请在此基础上增量修改，尽量保持现有布局和组件语义一致）：\n")
                    .append(request.getCanvasContext());
        }
        if (request.getCanvasWidth() != null && request.getCanvasHeight() != null) {
            userPrompt.append("\n\n画布尺寸：")
                    .append(request.getCanvasWidth())
                    .append("x")
                    .append(request.getCanvasHeight());
        }
        if (StringUtils.hasText(request.getStyle())) {
            userPrompt.append("\n风格要求：")
                    .append("light".equalsIgnoreCase(request.getStyle()) ? "浅色主题" : "深色主题");
        }
        return userPrompt.toString();
    }

    private Map<String, String> buildDashboardContextVars(AIGenerateRequest request) {
        int canvasWidth = request.getCanvasWidth() != null ? request.getCanvasWidth() : 1920;
        int canvasHeight = request.getCanvasHeight() != null ? request.getCanvasHeight() : 1080;
        boolean isLight = "light".equalsIgnoreCase(request.getStyle());

        Map<String, String> vars = new HashMap<>();
        vars.put("prompt", safeText(request.getPrompt()));
        vars.put("canvasWidth", String.valueOf(canvasWidth));
        vars.put("canvasHeight", String.valueOf(canvasHeight));
        vars.put("style", StringUtils.hasText(request.getStyle()) ? request.getStyle() : "dark");
        vars.put("styleLabel", isLight ? "浅色主题" : "深色主题");
        vars.put("backgroundColor", isLight ? "#f5f7fa" : "#1e1e2e");
        vars.put("backgroundSuggestion", isLight ? "使用浅色背景（如 #f5f7fa、#ffffff）" : "使用深色背景（如 #1e1e2e、#0f172a）");
        vars.put("textColorSuggestion", isLight ? "文字使用深色，强调信息层次" : "文字使用白色或浅色，保证对比度");
        vars.put("componentCatalog", safeText(request.getComponentCatalog()));
        vars.put("projectName", safeText(request.getProjectName()));
        vars.put("canvasContext", safeText(request.getCanvasContext()));
        return vars;
    }

    private String buildChatUserPrompt(String content, String projectName, String canvasContext) {
        StringBuilder prompt = new StringBuilder(content);
        if (StringUtils.hasText(projectName)) {
            prompt.append("\n\n当前项目：").append(projectName);
        }
        if (StringUtils.hasText(canvasContext)) {
            prompt.append("\n\n当前画布已有内容（请基于现有内容进行分析、修改或补充，而不是完全忽略已有内容）：\n")
                    .append(canvasContext);
        }
        return prompt.toString();
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
