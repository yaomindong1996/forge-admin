package com.mdframe.forge.plugin.ai.health;

import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

@Component
public class AiModelFailureClassifier {

    private static final int MAX_CAUSE_DEPTH = 16;
    private static final Set<String> CONTENT_POLICY_CODES = Set.of(
            "content_filter",
            "safety",
            "content_policy_violation"
    );

    public AiModelFailureCategory classify(Throwable failure) {
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth++ < MAX_CAUSE_DEPTH) {
            if (current instanceof CancellationException) return AiModelFailureCategory.CANCELLED;
            if (current instanceof SocketTimeoutException || current instanceof TimeoutException) return AiModelFailureCategory.TIMEOUT;
            if (current instanceof IOException) return AiModelFailureCategory.NETWORK;
            if (current instanceof BusinessException) return AiModelFailureCategory.VALIDATION;
            current = current.getCause();
        }
        AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(failure);
        Integer status = diagnostics.httpStatus();
        if (status != null && status == 429) return AiModelFailureCategory.RATE_LIMIT;
        if (status != null && status >= 500) return AiModelFailureCategory.PROVIDER_5XX;
        if (status != null && (status == 401 || status == 403)) return AiModelFailureCategory.AUTHENTICATION;
        String code = diagnostics.errorCode();
        if (code != null && CONTENT_POLICY_CODES.contains(code.toLowerCase(Locale.ROOT))) return AiModelFailureCategory.CONTENT_POLICY;
        if ("model_not_found".equalsIgnoreCase(code) || "invalid_model".equalsIgnoreCase(code)) return AiModelFailureCategory.MODEL_UNAVAILABLE;
        return AiModelFailureCategory.UNKNOWN;
    }
}
