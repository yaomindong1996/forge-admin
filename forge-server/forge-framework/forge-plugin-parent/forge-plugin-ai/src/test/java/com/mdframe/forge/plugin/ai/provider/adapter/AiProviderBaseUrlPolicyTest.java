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
        assertEquals("http://proxy.example.com/openai/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible",
                        "http://proxy.example.com/openai/v1/"));
    }

    @Test
    void blankBaseUrlShouldFallBackToProviderTypeDefault() {
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "alibaba", null));
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "dashscope", " "));
        assertEquals("https://api.openai.com/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "openai", null));
        assertEquals("https://open.bigmodel.cn/api/paas/v4",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "zhipu", null));
        assertEquals("https://api.moonshot.cn/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "moonshot", null));
        assertEquals("https://api.deepseek.com/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "deepseek", null));
        assertEquals("http://localhost:11434/v1",
                AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "ollama", null));
    }

    @Test
    void blankBaseUrlWithoutDefaultShouldFail() {
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "azure", null));
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "custom", null));
        assertThrows(BusinessException.class,
                () -> AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", "unknown", null));
    }

    private String normalize(String baseUrl) {
        return AiProviderBaseUrlPolicy.normalizeAndValidate("openai_compatible", baseUrl);
    }
}
