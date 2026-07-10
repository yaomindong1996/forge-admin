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
        assertEquals(AiProviderAdapterCode.DASHSCOPE_NATIVE,
                AiProviderAdapterCode.require("dashscope_native"));
    }

    @Test
    void requireShouldFailClosedForInvalidCodes() {
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require(null));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require(""));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require("   "));
        assertThrows(BusinessException.class, () -> AiProviderAdapterCode.require("unknown"));
    }
}
