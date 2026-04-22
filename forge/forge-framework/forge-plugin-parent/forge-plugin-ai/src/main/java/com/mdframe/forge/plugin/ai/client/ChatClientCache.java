package com.mdframe.forge.plugin.ai.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatClientCache {

    private static final int MAX_CACHE_SIZE = 50;
    private static final int CACHE_TTL_MINUTES = 30;

    private final Cache<String, ChatClient> cache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterWrite(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .removalListener((key, value, cause) ->
                    log.info("[ChatClientCache] 缓存移除, key={}, cause={}", key, cause))
            .build();

    public ChatClient getOrCreate(Long providerId, String modelName, AiProvider provider,
                                  OpenAiChatOptions options, String sessionId,
                                  org.springframework.ai.chat.memory.ChatMemory chatMemory) {
        String cacheKey = providerId + ":" + modelName;
        return cache.get(cacheKey, k -> buildChatClient(provider, options, sessionId, chatMemory));
    }

    public void evict(Long providerId, String modelName) {
        String cacheKey = providerId + ":" + modelName;
        cache.invalidate(cacheKey);
        log.info("[ChatClientCache] 主动清除缓存, key={}", cacheKey);
    }

    public void evictByProvider(Long providerId) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(providerId + ":"));
        log.info("[ChatClientCache] 按供应商清除缓存, providerId={}", providerId);
    }

    private ChatClient buildChatClient(AiProvider provider, OpenAiChatOptions options,
                                       String sessionId,
                                       org.springframework.ai.chat.memory.ChatMemory chatMemory) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        if (sessionId != null && chatMemory != null) {
            builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build());
        }
        log.info("[ChatClientCache] 新建 ChatClient, provider={}, model={}",
                provider.getProviderName(), options.getModel());
        return builder.build();
    }
}
