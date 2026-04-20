package com.mdframe.forge.plugin.ai.chat.service;

import com.mdframe.forge.plugin.ai.chat.dto.AIGenerateRequest;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 提示词模板渲染工具
 */
public final class AiPromptTemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private static final String DEFAULT_DASHBOARD_SYSTEM_PROMPT = "你是一个数据可视化大屏设计专家，请根据用户需求生成合法的 JSON 大屏配置。";

    private AiPromptTemplateRenderer() {
    }

    public static String renderDashboardSystemPrompt(String template, AIGenerateRequest request) {
        String sourceTemplate = StringUtils.hasText(template) ? template : DEFAULT_DASHBOARD_SYSTEM_PROMPT;
        Map<String, String> variables = buildDashboardVariables(request);
        return render(sourceTemplate, variables);
    }

    private static Map<String, String> buildDashboardVariables(AIGenerateRequest request) {
        int canvasWidth = request.getCanvasWidth() != null ? request.getCanvasWidth() : 1920;
        int canvasHeight = request.getCanvasHeight() != null ? request.getCanvasHeight() : 1080;
        boolean isLight = "light".equalsIgnoreCase(request.getStyle());

        Map<String, String> variables = new HashMap<>();
        variables.put("prompt", safeText(request.getPrompt()));
        variables.put("canvasWidth", String.valueOf(canvasWidth));
        variables.put("canvasHeight", String.valueOf(canvasHeight));
        variables.put("style", StringUtils.hasText(request.getStyle()) ? request.getStyle() : "dark");
        variables.put("styleLabel", isLight ? "浅色主题" : "深色主题");
        variables.put("backgroundColor", isLight ? "#f5f7fa" : "#1e1e2e");
        variables.put("backgroundSuggestion", isLight ? "使用浅色背景（如 #f5f7fa、#ffffff）" : "使用深色背景（如 #1e1e2e、#0f172a）");
        variables.put("textColorSuggestion", isLight ? "文字使用深色，强调信息层次" : "文字使用白色或浅色，保证对比度");
        variables.put("componentCatalog", safeText(request.getComponentCatalog()));
        variables.put("projectName", safeText(request.getProjectName()));
        variables.put("canvasContext", safeText(request.getCanvasContext()));
        return variables;
    }

    private static String safeText(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    public static String render(String template, Map<String, String> variables) {
        if (!StringUtils.hasText(template) || variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
