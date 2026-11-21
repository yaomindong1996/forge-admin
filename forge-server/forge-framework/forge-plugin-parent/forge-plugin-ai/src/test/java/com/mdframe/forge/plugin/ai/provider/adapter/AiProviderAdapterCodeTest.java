package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderAdapterCodeTest {

    @Test
    void requireShouldResolveSupportedCodes() {
        assertEquals(AiProviderAdapterCode.OPENAI_COMPATIBLE,
                AiProviderAdapterCode.require("openai_compatible"));
    }

    @Test
    void requireShouldFailClosedForInvalidOrRetiredCodes() {
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require(null));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require(""));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require("   "));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require("dashscope_native"));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require("unknown"));
    }
}
