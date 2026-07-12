package com.mdframe.forge.plugin.ai.health;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.assertEquals;
class AiModelFailureClassifierTest {
    private final AiModelFailureClassifier classifier=new AiModelFailureClassifier();
    @Test void shouldClassifyNetworkAndHttpFailures(){assertEquals(AiModelFailureCategory.TIMEOUT,classifier.classify(new SocketTimeoutException()));assertEquals(AiModelFailureCategory.NETWORK,classifier.classify(new IOException()));assertEquals(AiModelFailureCategory.RATE_LIMIT,classifier.classify(new IllegalStateException("429 - {\"error\":{\"code\":\"rate_limit\"}}")));assertEquals(AiModelFailureCategory.PROVIDER_5XX,classifier.classify(new IllegalStateException("503 - unavailable")));}
    @Test void localValidationShouldNotBecomeProviderFailure(){assertEquals(AiModelFailureCategory.VALIDATION,classifier.classify(new BusinessException("invalid")));}
    @Test void shouldClassifyWrappedTransportFailures(){assertEquals(AiModelFailureCategory.TIMEOUT,classifier.classify(new IllegalStateException(new SocketTimeoutException())));assertEquals(AiModelFailureCategory.NETWORK,classifier.classify(new IllegalStateException(new IOException())));}
    @Test void contentPolicyAndCancellationShouldNotPolluteHealth(){assertEquals(AiModelFailureCategory.CONTENT_POLICY,classifier.classify(new IllegalStateException("400 - {\"error\":{\"code\":\"content_filter\"}}")));assertEquals(AiModelFailureCategory.CONTENT_POLICY,classifier.classify(new IllegalStateException("400 - {\"error\":{\"type\":\"safety\"}}")));assertEquals(AiModelFailureCategory.CONTENT_POLICY,classifier.classify(new IllegalStateException("400 - {\"error\":{\"code\":\"content_policy_violation\"}}")));assertEquals(AiModelFailureCategory.CANCELLED,classifier.classify(new IllegalStateException(new CancellationException())));}
}
