package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderAdapterRegistryTest {

    private AiSecretCrypto secretCrypto() {
        AiSecretCrypto crypto = mock(AiSecretCrypto.class);
        // decrypt 默认返回输入（测试用 provider 的 apiKey 是明文）
        when(crypto.decrypt(any())).thenAnswer(inv -> inv.getArgument(0));
        return crypto;
    }

    @Test
    void constructorShouldRejectDuplicateAdapterCodes() {
        AiProviderAdapter first = new RecordingAdapter("openai_compatible", new ArrayList<>(), null);
        AiProviderAdapter duplicate = new RecordingAdapter("openai_compatible", new ArrayList<>(), null);

        assertThrows(IllegalStateException.class,
                () -> new AiProviderAdapterRegistry(List.of(first, duplicate), secretCrypto()));
    }

    @Test
    void getRequiredShouldResolveKnownAdapterAndRejectUnknownCode() {
        AiProviderAdapter adapter = new RecordingAdapter("dashscope_native", new ArrayList<>(), null);
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter), secretCrypto());

        assertSame(adapter, registry.getRequired("dashscope_native"));
        assertThrows(BusinessException.class, () -> registry.getRequired("unknown"));
    }

    @Test
    void createChatModelShouldValidateBeforeCreating() {
        List<String> events = new ArrayList<>();
        ChatModel expected = prompt -> null;
        AiProviderAdapter adapter = new RecordingAdapter("openai_compatible", events, expected);
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter), secretCrypto());
        AiProvider provider = provider("openai_compatible");
        AiModelRuntimeOptions options = new AiModelRuntimeOptions("gpt-4o-mini", 0.2D, 128);

        ChatModel actual = registry.createChatModel(provider, options);

        assertSame(expected, actual);
        assertEquals(List.of("validate", "create"), events);
    }

    @Test
    void createChatModelShouldNotCreateWhenValidationFails() {
        List<String> events = new ArrayList<>();
        AiProviderAdapter adapter = new RecordingAdapter("openai_compatible", events, null) {
            @Override
            public void validate(AiProvider provider, AiModelRuntimeOptions options) {
                events.add("validate");
                throw new BusinessException("invalid");
            }
        };
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter), secretCrypto());

        assertThrows(BusinessException.class,
                () -> registry.createChatModel(provider("openai_compatible"),
                        new AiModelRuntimeOptions("gpt-4o-mini", null, null)));
        assertEquals(List.of("validate"), events);
    }

    private AiProvider provider(String adapterCode) {
        AiProvider provider = new AiProvider();
        provider.setAdapterCode(adapterCode);
        return provider;
    }

    @Test
    void createEmbeddingModelDelegatesToTheRegisteredProtocol() {
        List<String> events = new ArrayList<>();
        var adapter = new RecordingAdapter("dashscope_native", events, null);
        var registry = new AiProviderAdapterRegistry(List.of(adapter), secretCrypto());
        assertSame(adapter.embedding, registry.createEmbeddingModel(provider("dashscope_native"), "t1-embedding"));
        assertEquals(List.of("create_embedding"), events);
    }

    private static class RecordingAdapter implements AiProviderAdapter {

        private final String code;
        private final List<String> events;
        private final ChatModel model;
        private final EmbeddingModel embedding = mock(EmbeddingModel.class);

        private RecordingAdapter(String code, List<String> events, ChatModel model) {
            this.code = code;
            this.events = events;
            this.model = model;
        }

        @Override
        public String adapterCode() {
            return code;
        }

        @Override
        public void validate(AiProvider provider, AiModelRuntimeOptions options) {
            events.add("validate");
        }

        @Override
        public ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options) {
            events.add("create");
            return model;
        }

        @Override
        public EmbeddingModel createEmbeddingModel(AiProvider provider, String model) {
            events.add("create_embedding");
            return embedding;
        }
    }
}
