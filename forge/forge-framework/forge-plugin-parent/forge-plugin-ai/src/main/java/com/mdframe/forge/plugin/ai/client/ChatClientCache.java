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

    public ChatClient getOrCreateBase(Long providerId, String modelName, AiProvider provider,
                                      OpenAiChatOptions options) {
        String cacheKey = buildCacheKey(providerId, modelName, options);
        return cache.get(cacheKey, k -> buildBaseChatClient(provider, options));
    }

    public ChatClient createSessionClient(ChatClient baseClient, String sessionId,
                                          org.springframework.ai.chat.memory.ChatMemory chatMemory) {
        if (baseClient == null) {
            return null;
        }
        if (sessionId == null || chatMemory == null) {
            return baseClient;
        }
        return baseClient.mutate()
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(sessionId)
                        .build())
                .build();
    }

    public void evict(Long providerId, String modelName) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(providerId + ":" + modelName + ":"));
        log.info("[ChatClientCache] 主动清除缓存, providerId={}, modelName={}", providerId, modelName);
    }

    public void evictByProvider(Long providerId) {
        cache.asMap().keySet().removeIf(key -> key.startsWith(providerId + ":"));
        log.info("[ChatClientCache] 按供应商清除缓存, providerId={}", providerId);
    }

    private String buildCacheKey(Long providerId, String modelName, OpenAiChatOptions options) {
        Double temperature = options.getTemperature();
        Integer maxTokens = options.getMaxTokens();
        return providerId + ":" + modelName + ":"
                + (temperature != null ? temperature : "null") + ":"
                + (maxTokens != null ? maxTokens : "null");
    }

    private ChatClient buildBaseChatClient(AiProvider provider, OpenAiChatOptions options) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(provider.getBaseUrl())
                .apiKey(provider.getApiKey())
                .build();
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        log.info("[ChatClientCache] 新建基础 ChatClient, provider={}, model={}, temperature={}, maxTokens={}",
                provider.getProviderName(), options.getModel(), options.getTemperature(), options.getMaxTokens());
        return ChatClient.builder(chatModel).build();
    }
}
