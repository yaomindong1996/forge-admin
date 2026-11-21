package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderTemplateVO;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderTypeCountVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderTemplateCatalogTest {

    @Test
    void templatesShouldContain19OpenAiCompatibleProvidersWithoutDashScopeNative() {
        List<AiProviderTemplateVO> templates = AiProviderTemplateCatalog.templates();
        assertEquals(19, templates.size());
        assertTrue(templates.stream().allMatch(t ->
                AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode().equals(t.adapterCode())));
        assertFalse(templates.stream().anyMatch(t -> "alibaba_native".equals(t.templateKey())));
    }

    @Test
    void defaultBaseUrlShouldResolveCanonicalAndAliasProviderTypes() {
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode",
                AiProviderTemplateCatalog.defaultBaseUrl("alibaba"));
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode",
                AiProviderTemplateCatalog.defaultBaseUrl("dashscope"));
        assertEquals("https://api.moonshot.cn/v1",
                AiProviderTemplateCatalog.defaultBaseUrl("kimi"));
        assertNull(AiProviderTemplateCatalog.defaultBaseUrl("azure"));
        assertNull(AiProviderTemplateCatalog.defaultBaseUrl("custom"));
    }

    @Test
    void withCountsShouldAggregateCanonicalAndAliasCounts() {
        AiProviderTypeCountVO alibaba = new AiProviderTypeCountVO();
        alibaba.setProviderType("alibaba");
        alibaba.setConfiguredCount(2L);
        AiProviderTypeCountVO dashscope = new AiProviderTypeCountVO();
        dashscope.setProviderType("dashscope");
        dashscope.setConfiguredCount(1L);

        List<AiProviderTemplateVO> templates = AiProviderTemplateCatalog.withCounts(List.of(alibaba, dashscope));
        AiProviderTemplateVO alibabaTemplate = templates.stream()
                .filter(t -> "alibaba".equals(t.templateKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(3L, alibabaTemplate.configuredCount());
    }
}
