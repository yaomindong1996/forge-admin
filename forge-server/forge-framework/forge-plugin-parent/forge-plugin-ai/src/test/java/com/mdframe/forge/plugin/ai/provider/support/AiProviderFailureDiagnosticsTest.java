package com.mdframe.forge.plugin.ai.provider.support;

import org.junit.jupiter.api.Test;
import com.openai.errors.OpenAIServiceException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderFailureDiagnosticsTest {

    @Test
    void shouldExtractOnlySafeStatusAndErrorCode() {
        RuntimeException exception = new RuntimeException(
                "400 - {\"error\":{\"message\":\"secret sk-test-123\","
                        + "\"type\":\"invalid_request_error\",\"code\":\"invalid_request_error\"}}");

        AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(exception);

        assertEquals(400, diagnostics.httpStatus());
        assertEquals("invalid_request_error", diagnostics.errorCode());
        assertFalse(diagnostics.toString().contains("sk-test-123"));
    }

    @Test
    void shouldRejectUnsafeProviderErrorCode() {
        RuntimeException exception = new RuntimeException(
                "400 - {\"error\":{\"code\":\"unsafe key=sk-test-123\"}}");

        AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(exception);

        assertEquals(400, diagnostics.httpStatus());
        assertEquals("unknown", diagnostics.errorCode());
        assertFalse(diagnostics.toString().contains("sk-test-123"));
    }

    @Test
    void extractsStructuredOfficialSdkStatusInsteadOfParsingItsMessage() {
        var exception = mock(OpenAIServiceException.class);
        when(exception.statusCode()).thenReturn(429);
        when(exception.code()).thenReturn(Optional.of("rate_limit_exceeded"));
        var diagnostics = AiProviderFailureDiagnostics.from(new IllegalStateException(exception));
        assertEquals(429, diagnostics.httpStatus());
        assertEquals("rate_limit_exceeded", diagnostics.errorCode());
    }

    @Test
    void extractsSanitizedNativeStatusWithoutLeakingOriginalBodies() {
        var exception = new AiProviderRequestException("通义原生接口调用失败", 503, "unsafe key=sk-test-123");
        var diagnostics = AiProviderFailureDiagnostics.from(exception);
        assertEquals(503, diagnostics.httpStatus());
        assertEquals("unknown", diagnostics.errorCode());
        assertFalse(exception.toString().contains("sk-test-123"));
        assertFalse(diagnostics.toString().contains("sk-test-123"));
    }
}
