package com.mdframe.forge.plugin.job.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 定时任务日志脱敏和限长处理器。
 */
@Component
@RequiredArgsConstructor
public class JobLogSanitizer {

    public static final int MAX_JOB_PARAM_LENGTH = 2000;
    public static final int MAX_RESULT_LENGTH = 2000;
    public static final int MAX_EXCEPTION_LENGTH = 4000;
    public static final String TRUNCATED_SUFFIX = "...[已截断]";

    private static final String MASK = "****";
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "authorization", "token", "accesstoken", "refreshtoken", "idtoken",
            "password", "passwd", "secret", "clientsecret", "apikey", "credential",
            "cookie", "setcookie", "phone", "mobile", "telephone", "idcard",
            "identitynumber", "bankcard", "cardnumber");
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*)(?:bearer\\s+)?([^\\s,;]+)");
    private static final Pattern SECRET_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)((?:access[_-]?token|refresh[_-]?token|id[_-]?token|api[_-]?key|token|password|passwd|"
                    + "client[_-]?secret|secret|credential|cookie)\\s*[:=]\\s*[\\\"']?)([^\\s,\\\"';}\\]]+)");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(?i)(bearer\\s+)[a-z0-9._~+/=-]+");
    private static final Pattern MOBILE_PATTERN = Pattern.compile(
            "(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{6})\\d{8}(\\d{3}[0-9xX])(?!\\d)");

    private final ObjectMapper objectMapper;

    public String sanitizeJobParam(String value) {
        return sanitize(value, MAX_JOB_PARAM_LENGTH);
    }

    public String sanitizeResult(String value) {
        return sanitize(value, MAX_RESULT_LENGTH);
    }

    public String sanitizeException(String value) {
        return sanitize(value, MAX_EXCEPTION_LENGTH);
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = sanitizeJson(value);
        sanitized = AUTHORIZATION_PATTERN.matcher(sanitized).replaceAll("$1" + MASK);
        sanitized = SECRET_KEY_VALUE_PATTERN.matcher(sanitized).replaceAll("$1" + MASK);
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1" + MASK);
        sanitized = MOBILE_PATTERN.matcher(sanitized).replaceAll("$1****$2");
        sanitized = ID_CARD_PATTERN.matcher(sanitized).replaceAll("$1********$2");
        return truncate(sanitized, maxLength);
    }

    private String sanitizeJson(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null) {
                return value;
            }
            redactJson(root);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            return value;
        }
    }

    private void redactJson(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            for (Map.Entry<String, JsonNode> field : objectNode.properties()) {
                if (isSensitiveField(field.getKey())) {
                    objectNode.set(field.getKey(), TextNode.valueOf(MASK));
                } else {
                    redactJson(field.getValue());
                }
            }
            return;
        }
        if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::redactJson);
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELDS.contains(normalized);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        int contentLength = Math.max(0, maxLength - TRUNCATED_SUFFIX.length());
        return value.substring(0, contentLength) + TRUNCATED_SUFFIX;
    }
}
