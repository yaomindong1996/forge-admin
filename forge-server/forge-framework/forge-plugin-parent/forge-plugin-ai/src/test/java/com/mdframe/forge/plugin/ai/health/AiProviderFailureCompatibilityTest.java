package com.mdframe.forge.plugin.ai.health;

import com.mdframe.forge.plugin.ai.provider.support.AiProviderRequestException;
import com.openai.errors.OpenAIServiceException;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderFailureCompatibilityTest {

    private final AiModelFailureClassifier classifier = new AiModelFailureClassifier();

    @Test
    void recognizesTheOfficialSdkRateLimitAndAuthenticationStatuses() {
        var error = mock(OpenAIServiceException.class);
        when(error.code()).thenReturn(Optional.empty());
        when(error.type()).thenReturn(Optional.empty());
        when(error.statusCode()).thenReturn(429);
        assertEquals(AiModelFailureCategory.RATE_LIMIT, classifier.classify(error));
        when(error.statusCode()).thenReturn(401);
        assertEquals(AiModelFailureCategory.AUTHENTICATION, classifier.classify(error));
        when(error.statusCode()).thenReturn(503);
        assertEquals(AiModelFailureCategory.PROVIDER_5XX, classifier.classify(error));
    }

    @Test
    void nativeHttpErrorsMustNotBeMistakenForLocalValidation() {
        assertEquals(AiModelFailureCategory.PROVIDER_5XX,
                classifier.classify(new AiProviderRequestException("调用失败", 503, "http_error")));
        assertEquals(AiModelFailureCategory.AUTHENTICATION,
                classifier.classify(new AiProviderRequestException("调用失败", 401, "http_error")));
    }

    @Test
    void nativeSanitizedTransportErrorsRetainTheirHealthCategory() {
        assertEquals(AiModelFailureCategory.TIMEOUT,
                classifier.classify(new AiProviderRequestException("超时", null, "timeout")));
        assertEquals(AiModelFailureCategory.NETWORK,
                classifier.classify(new AiProviderRequestException("连接失败", null, "connection_error")));
        assertEquals(AiModelFailureCategory.TIMEOUT, classifier.classify(new HttpTimeoutException("fixture")));
    }
}
