package com.mdframe.forge.plugin.ai.provider.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从供应商 SDK 异常中提取可安全记录的最小诊断信息。
 *
 * <p>该类型不保留原始异常消息、响应正文或请求信息，避免日志泄露供应商凭据和敏感内容。</p>
 */
public record AiProviderFailureDiagnostics(Integer httpStatus, String errorCode) {

    private static final int MAX_MESSAGE_LENGTH = 16_384;
    private static final int MAX_CAUSE_DEPTH = 8;
    private static final String UNKNOWN_ERROR_CODE = "unknown";
    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("^\\s*(\\d{3})\\s*-");
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(
            "\\\"code\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern ERROR_TYPE_PATTERN = Pattern.compile(
            "\\\"type\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern SAFE_ERROR_CODE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public AiProviderFailureDiagnostics {
        errorCode = sanitizeErrorCode(errorCode);
    }

    /**
     * 提取 HTTP 状态码及供应商错误码，不保留原始消息。
     *
     * @param failure SDK 异常
     * @return 安全诊断字段
     */
    public static AiProviderFailureDiagnostics from(Throwable failure) {
        Integer httpStatus = null;
        String errorCode = null;
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth++ < MAX_CAUSE_DEPTH) {
            String message = limit(current.getMessage());
            if (httpStatus == null) {
                httpStatus = extractHttpStatus(message);
            }
            if (errorCode == null) {
                errorCode = extractErrorCode(message);
            }
            current = current.getCause();
        }
        return new AiProviderFailureDiagnostics(httpStatus, errorCode);
    }

    private static Integer extractHttpStatus(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        int value = Integer.parseInt(matcher.group(1));
        return value >= 100 && value <= 599 ? value : null;
    }

    private static String extractErrorCode(String message) {
        if (message == null) {
            return null;
        }
        String code = matchFirstGroup(ERROR_CODE_PATTERN, message);
        return code != null ? code : matchFirstGroup(ERROR_TYPE_PATTERN, message);
    }

    private static String matchFirstGroup(Pattern pattern, String message) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String sanitizeErrorCode(String value) {
        if (value == null || !SAFE_ERROR_CODE_PATTERN.matcher(value).matches()) {
            return UNKNOWN_ERROR_CODE;
        }
        return value;
    }

    private static String limit(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
