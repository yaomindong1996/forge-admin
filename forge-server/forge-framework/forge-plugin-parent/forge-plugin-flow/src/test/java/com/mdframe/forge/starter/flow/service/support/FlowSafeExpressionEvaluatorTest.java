package com.mdframe.forge.starter.flow.service.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowSafeExpressionEvaluatorTest {

    @Test
    void shouldEvaluateVariablesAndTemplateWrappedConditions() {
        assertThat(FlowSafeExpressionEvaluator.evaluateBoolean("${#amount > 100}", Map.of("amount", 120)))
                .isTrue();
        assertThat(FlowSafeExpressionEvaluator.evaluate("#user.toUpperCase()", Map.of("user", "alice")))
                .isEqualTo("ALICE");
    }

    @Test
    void shouldRejectTypeBeanConstructorAndOversizedExpressions() {
        assertThatThrownBy(() -> FlowSafeExpressionEvaluator.evaluate(
                "T(java.lang.Runtime).getRuntime().exec('whoami')", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlowSafeExpressionEvaluator.evaluate(
                "#user.getClass().forName('java.lang.Runtime')", Map.of("user", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlowSafeExpressionEvaluator.evaluate("@systemProperties", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlowSafeExpressionEvaluator.evaluate("new java.lang.String('x')", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FlowSafeExpressionEvaluator.evaluate("x".repeat(2001), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
