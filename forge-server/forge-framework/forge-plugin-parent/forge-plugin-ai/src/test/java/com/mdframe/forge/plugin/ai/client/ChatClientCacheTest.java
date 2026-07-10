package com.mdframe.forge.plugin.ai.client;

import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatClientCacheTest {

    @Mock
    private AiProviderAdapterRegistry adapterRegistry;

    private ChatClientCache cache;

    @BeforeEach
    void setUp() {
        lenient().when(adapterRegistry.createChatModel(any(), any()))
                .thenAnswer(invocation -> mock(ChatModel.class));
        cache = new ChatClientCache(adapterRegistry);
    }

    @Test
    void sameConfigurationShouldReuseBaseClient() {
        AiProvider provider = provider(1L, 10L, "openai_compatible");
        AiModelRuntimeOptions options = options();

        ChatClient first = cache.getOrCreateBase(provider, options);
        ChatClient second = cache.getOrCreateBase(provider, options);

        assertSame(first, second);
        verify(adapterRegistry).createChatModel(provider, options);
    }

    @Test
    void differentTenantsShouldNotShareCache() {
        AiModelRuntimeOptions options = options();

        ChatClient first = cache.getOrCreateBase(provider(1L, 10L, "openai_compatible"), options);
        ChatClient second = cache.getOrCreateBase(provider(2L, 10L, "openai_compatible"), options);

        assertNotSame(first, second);
        verify(adapterRegistry, times(2)).createChatModel(any(), any());
    }

    @Test
    void differentAdaptersShouldNotShareCache() {
        AiModelRuntimeOptions options = options();

        ChatClient first = cache.getOrCreateBase(provider(1L, 10L, "openai_compatible"), options);
        ChatClient second = cache.getOrCreateBase(provider(1L, 10L, "dashscope_native"), options);

        assertNotSame(first, second);
        verify(adapterRegistry, times(2)).createChatModel(any(), any());
    }

    @Test
    void differentRuntimeOptionsShouldNotShareCache() {
        AiProvider provider = provider(1L, 10L, "openai_compatible");

        ChatClient first = cache.getOrCreateBase(provider, options());
        ChatClient second = cache.getOrCreateBase(
                provider, new AiModelRuntimeOptions("test-model", 0.6D, 512));

        assertNotSame(first, second);
        verify(adapterRegistry, times(2)).createChatModel(any(), any());
    }

    @Test
    void missingTenantIdShouldFailClosed() {
        AiProvider provider = provider(null, 10L, "openai_compatible");

        assertThrows(BusinessException.class, () -> cache.getOrCreateBase(provider, options()));
    }

    @Test
    void evictByProviderShouldRemoveAllModelVariants() {
        AiProvider provider = provider(1L, 10L, "openai_compatible");
        AiModelRuntimeOptions options = options();
        ChatClient first = cache.getOrCreateBase(provider, options);

        cache.evictByProvider(1L, 10L);
        ChatClient second = cache.getOrCreateBase(provider, options);

        assertNotSame(first, second);
        verify(adapterRegistry, times(2)).createChatModel(provider, options);
    }

    private AiProvider provider(Long tenantId, Long providerId, String adapterCode) {
        AiProvider provider = new AiProvider();
        provider.setTenantId(tenantId);
        provider.setId(providerId);
        provider.setProviderName("Test Provider");
        provider.setAdapterCode(adapterCode);
        return provider;
    }

    private AiModelRuntimeOptions options() {
        return new AiModelRuntimeOptions("test-model", 0.3D, 256);
    }
}
