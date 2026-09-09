package com.mdframe.forge.starter.flow.service.support;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import java.util.Collections;
import java.util.Map;

/** 受限的流程表达式求值器，禁止类型、Bean 和构造器访问。 */
public final class FlowSafeExpressionEvaluator {

    public static final int MAX_EXPRESSION_LENGTH = 2000;
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private FlowSafeExpressionEvaluator() {
    }

    public static Object evaluate(String expression, Map<String, Object> variables) {
        String normalized = normalize(expression);
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withInstanceMethods()
                .withAssignmentDisabled()
                .build();
        Map<String, Object> safeVariables = variables == null ? Collections.emptyMap() : variables;
        safeVariables.forEach(context::setVariable);
        return PARSER.parseExpression(normalized).getValue(context);
    }

    public static Boolean evaluateBoolean(String expression, Map<String, Object> variables) {
        Object value = evaluate(expression, variables);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private static String normalize(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("流程表达式不能为空");
        }
        String value = expression.trim();
        if (value.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("流程表达式长度超过限制");
        }
        if ((value.startsWith("${") || value.startsWith("#{")) && value.endsWith("}")) {
            value = value.substring(2, value.length() - 1).trim();
        }
        if (value.isEmpty() || value.matches("(?s).*\\bT\\s*\\(.*") || value.contains("@")
                || value.matches("(?s).*\\bnew\\s+.*")
                || value.matches("(?s).*\\b(getClass|forName|getRuntime|exec|invoke)\\s*\\(.*")
                || value.contains("#root") || value.contains("#this")) {
            throw new IllegalArgumentException("流程表达式包含不允许的语法");
        }
        return value;
    }
}
