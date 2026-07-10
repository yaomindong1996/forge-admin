package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderBaseUrlPolicyTest {

    @Test
    void normalizeShouldRejectUnsafeUris() {
        assertThrows(BusinessException.class, () -> normalize("ftp://example.com"));
        assertThrows(BusinessException.class, () -> normalize("https://example.com/path?key=value"));
        assertThrows(BusinessException.class, () -> normalize("https://example.com/path#fragment"));
        assertThrows(BusinessException.class, () -> normalize("https://user@example.com/path"));
    }

    @Test
    void nativeShouldUseOfficialRootAndRejectCompatiblePath() {
        assertEquals("https://dashscope.aliyuncs.com",
                AiProviderBaseUrlPolicy.normalizeAndValidate("dashscope_native", null));
        assertEquals("https://dashscope.aliyuncs.com",
                AiProviderBaseUrlPolicy.normalizeAndValidate("dashscope_native",
                        "https://dashscope.aliyuncs.com/"));
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("dashscope_native",
                        "https://dashscope.aliyuncs.com/compatible-mode"));
    }

    @Test
    void compatibleShouldRequireCompatibleModeOnOfficialDashScopeHost() {
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible",
                        "https://dashscope.aliyuncs.com/compatible-mode/"));
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible",
                        "https://dashscope.aliyuncs.com"));
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible",
                        "https://dashscope.aliyuncs.com/compatible-mode/v1"));
    }

    @Test
    void customProxyShouldOnlyApplyGenericValidation() {
        assertEquals("https://proxy.example.com/dashscope",
                AiProviderBaseUrlPolicy.normalizeAndValidate("dashscope_native",
                        "https://proxy.example.com/dashscope/"));
        assertEquals("http://proxy.example.com/openai/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible",
                        "http://proxy.example.com/openai/v1/"));
    }

    private String normalize(String baseUrl) {
        return AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", baseUrl);
    }
}
