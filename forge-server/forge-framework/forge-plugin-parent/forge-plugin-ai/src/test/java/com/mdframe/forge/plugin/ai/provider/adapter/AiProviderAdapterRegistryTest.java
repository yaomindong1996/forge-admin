package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderAdapterRegistryTest {

    @Test
    void constructorShouldRejectDuplicateAdapterCodes() {
        AiProviderAdapter first = new RecordingAdapter("openai_compatible", new ArrayList<>(), null);
        AiProviderAdapter duplicate = new RecordingAdapter("openai_compatible", new ArrayList<>(), null);

        assertThrows(IllegalStateException.class,
                () -> new AiProviderAdapterRegistry(List.of(first, duplicate)));
    }

    @Test
    void getRequiredShouldResolveKnownAdapterAndRejectUnknownCode() {
        AiProviderAdapter adapter = new RecordingAdapter("dashscope_native", new ArrayList<>(), null);
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter));

        assertSame(adapter, registry.getRequired("dashscope_native"));
        assertThrows(BusinessException.class, () -> registry.getRequired("unknown"));
    }

    @Test
    void createChatModelShouldValidateBeforeCreating() {
        List<String> events = new ArrayList<>();
        ChatModel expected = prompt -> null;
        AiProviderAdapter adapter = new RecordingAdapter("openai_compatible", events, expected);
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter));
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
        AiProviderAdapterRegistry registry = new AiProviderAdapterRegistry(List.of(adapter));

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

    private static class RecordingAdapter implements AiProviderAdapter {

        private final String code;
        private final List<String> events;
        private final ChatModel model;

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
    }
}
