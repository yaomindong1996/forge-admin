package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * HTTP 请求内置工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HttpTool implements AgentTool {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final Set<String> METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.POST.name());
    private final SecureOutboundClient outboundClient;

    @Override
    public String getKey() {
        return "http_request";
    }

    @Override
    public String getDescription() {
        return "发送HTTP请求获取外部API数据。支持GET/POST方法。";
    }

    @Override
    public String getParametersSchema() {
        return """
        {
          "type": "object",
          "properties": {
            "url": {
              "type": "string",
              "description": "请求URL"
            },
            "method": {
              "type": "string",
              "enum": ["GET", "POST"],
              "description": "HTTP方法（默认GET）"
            },
            "headers": {
              "type": "object",
              "description": "请求头"
            },
            "body": {
              "type": "string",
              "description": "请求体（POST时使用）"
            }
          },
          "required": ["url"]
        }
        """;
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ToolContext context) {
        try {
            String url = (String) args.get("url");
            if (url == null || url.isBlank()) {
                return ToolResult.error("URL不能为空");
            }
            String method = args.getOrDefault("method", HttpMethod.GET.name()).toString()
                    .toUpperCase(java.util.Locale.ROOT);
            if (!METHODS.contains(method)) {
                return ToolResult.error("HTTP 工具仅支持 GET/POST");
            }
            String body = HttpMethod.POST.name().equals(method) ? args.getOrDefault("body", "").toString() : "";
            // 使用系统已维护的外部连接器场景与目标白名单，不能创建一个校验器不认识的场景。
            var request = OutboundRequest.builder().scene(OutboundScenes.EXTERNAL_CONNECTOR).url(url).method(method)
                    .headers(headers(args.get("headers"))).contentType("application/json")
                    .body(body.getBytes(StandardCharsets.UTF_8)).callTimeout(TIMEOUT).build();
            // 主机/内网、DNS 重绑定、重定向、敏感头与响应体上限由框架统一拒绝；不转发登录请求头。
            var response = outboundClient.execute(request);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                return ToolResult.error("HTTP 请求未成功");
            }
            return ToolResult.data(response.bodyAsUtf8());
        } catch (RuntimeException ex) {
            log.warn("[AgentHttp] 请求被拒绝或失败: exceptionType={}", ex.getClass().getSimpleName());
            return ToolResult.error("HTTP 请求未通过安全策略或调用失败");
        }
    }

    private Map<String, String> headers(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("请求头必须为对象");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String text)) {
                throw new IllegalArgumentException("请求头必须为字符串");
            }
            result.put(key, text);
        }
        return result;
    }
}
