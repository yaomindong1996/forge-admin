package com.mdframe.forge.plugin.ai.provider.support;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiProviderFailureDiagnosticsTest {

    @Test
    void shouldExtractOnlySafeStatusAndErrorCode() {
        NonTransientAiException exception = new NonTransientAiException(
                "400 - {\"error\":{\"message\":\"secret sk-test-123\","
                        + "\"type\":\"invalid_request_error\",\"code\":\"invalid_request_error\"}}");

        AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(exception);

        assertEquals(400, diagnostics.httpStatus());
        assertEquals("invalid_request_error", diagnostics.errorCode());
        assertFalse(diagnostics.toString().contains("sk-test-123"));
    }

    @Test
    void shouldRejectUnsafeProviderErrorCode() {
        NonTransientAiException exception = new NonTransientAiException(
                "400 - {\"error\":{\"code\":\"unsafe key=sk-test-123\"}}");

        AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(exception);

        assertEquals(400, diagnostics.httpStatus());
        assertEquals("unknown", diagnostics.errorCode());
        assertFalse(diagnostics.toString().contains("sk-test-123"));
    }
}
