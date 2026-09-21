package com.mdframe.forge.plugin.print.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.regex.Pattern;

/**
 * 不执行表达式，只拒绝明显不安全或超限的文本。
 */
final class PrintExpressionRules {

    private static final int MAX_LENGTH = 500;
    private static final Pattern CONTROL = Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]");

    private PrintExpressionRules() {
    }

    static void check(JsonNode value, String path, PrintProtocolRules r) {
        if (value == null || !value.isTextual() || value.textValue().isBlank() || value.textValue().length() > MAX_LENGTH) {
            r.issue(path, "INVALID_EXPRESSION", "表达式必须是不超过 500 字的文本");
            return;
        }
        String text = value.textValue();
        if (CONTROL.matcher(text).find()) {
            r.issue(path, "INVALID_EXPRESSION", "表达式含有非法控制字符");
            return;
        }
        String lower = text.toLowerCase();
        if (lower.contains("eval(") || lower.contains("function(") || lower.contains("__proto__") || lower.contains(".constructor") || lower.contains("javascript:")) {
            r.issue(path, "INVALID_EXPRESSION", "表达式含有不支持的内容");
        }
    }
}
