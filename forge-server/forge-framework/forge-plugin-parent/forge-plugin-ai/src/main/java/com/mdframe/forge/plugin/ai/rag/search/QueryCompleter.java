package com.mdframe.forge.plugin.ai.rag.search;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询补全器。
 * 使用默认 Chat 模型扩展用户查询，使其更适合检索。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryCompleter {

    private final AiProviderService providerService;
    private final AiProviderAdapterRegistry providerAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;

    /**
     * 扩展查询文本。
     * 使用 LLM 将简短查询扩展为更详细的检索查询。
     *
     * @param query 原始查询
     * @return 扩展后的查询
     */
    public String expand(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }

        try {
            AiProvider provider = providerService.requireEnabledDefaultProvider();
            String apiKey = aiSecretCrypto.resolvePlainSecret(provider.getApiKey());

            AiModelRuntimeOptions options = new AiModelRuntimeOptions(
                    provider.getDefaultModel(), 0.3, 256);
            ChatModel chatModel = providerAdapterRegistry.createChatModel(provider, options);

            String systemPrompt = """
                    你是一个查询扩展专家。将用户的简短查询扩展为更适合文档检索的详细查询。
                    保持原始查询的核心意图，添加相关的同义词、上下文和细节。
                    只输出扩展后的查询文本，不要添加任何解释。
                    """;

            Prompt prompt = new Prompt(List.of(
                    new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                    new UserMessage(query)));

            ChatResponse response = chatModel.call(prompt);
            String expanded = response.getResult().getOutput().getText();

            if (expanded != null && !expanded.isBlank()) {
                return expanded.trim();
            }
            return query;
        } catch (Exception e) {
            log.warn("[QueryCompleter] 查询扩展失败，使用原始查询", e);
            return query;
        }
    }
}
