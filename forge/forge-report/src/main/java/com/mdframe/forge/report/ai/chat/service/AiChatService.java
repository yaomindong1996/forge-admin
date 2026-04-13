package com.mdframe.forge.report.ai.chat.service;

import com.mdframe.forge.report.ai.agent.domain.AiAgent;
import com.mdframe.forge.report.ai.agent.service.AiAgentService;
import com.mdframe.forge.report.ai.provider.domain.AiProvider;
import com.mdframe.forge.report.ai.provider.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务（使用标准 OpenAI 兼容接口，支持多供应商扩展）
 * 
 * 支持的供应商：
 * - 阿里百炼：https://dashscope.aliyuncs.com/compatible-mode
 * - OpenAI：https://api.openai.com
 * - 智谱 AI：https://open.bigmodel.cn/api/paas/v4
 * - Moonshot：https://api.moonshot.cn/v1
 * - 其他兼容 OpenAI 接口的供应商
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiAgentService agentService;
    private final AiProviderService providerService;
    private volatile ChatClient chatClient;
    private volatile String lastProviderKey;

    /**
     * 获取或创建 ChatClient（根据默认供应商动态创建）
     * 使用标准 OpenAI 兼容接口，支持任意兼容 OpenAI API 的供应商
     */
    private ChatClient getChatClient() {
        AiProvider provider = providerService.getDefaultProvider();
        if (provider == null) {
            throw new RuntimeException("未配置默认 AI 供应商");
        }

        String providerKey = provider.getId() + "_" + provider.getApiKey();
        if (chatClient == null || !providerKey.equals(lastProviderKey)) {
            synchronized (this) {
                if (chatClient == null || !providerKey.equals(lastProviderKey)) {
                    // 创建标准 OpenAI API 客户端（兼容所有 OpenAI 接口供应商）
                    OpenAiApi openAiApi = OpenAiApi.builder()
                            .baseUrl(provider.getBaseUrl())
                            .apiKey(provider.getApiKey())
                            .build();

                    // 创建 ChatModel（使用 OpenAI 兼容模式）
                    ChatModel chatModel = OpenAiChatModel.builder()
                            .openAiApi(openAiApi)
                            .defaultOptions(OpenAiChatOptions.builder()
                                    .model("qwen-plus") // 默认模型，可从供应商配置中读取
                                    .temperature(0.7)
                                    .build())
                            .build();

                    // 创建 ChatClient
                    chatClient = ChatClient.builder(chatModel).build();
                    lastProviderKey = providerKey;
                    log.info("AI ChatClient 初始化成功, provider={}, baseUrl={}, 使用 OpenAI 兼容接口",
                            provider.getProviderName(), provider.getBaseUrl());
                }
            }
        }
        return chatClient;
    }

    /**
     * 大屏生成（非流式）
     */
    public String generateDashboard(String prompt) {
        AiAgent agent = agentService.getByCode("dashboard_generator");
        if (agent == null) {
            throw new RuntimeException("未找到大屏生成 Agent");
        }

        return getChatClient().prompt()
                .system(agent.getSystemPrompt())
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 流式对话（真正的 SSE 流式输出）
     */
    public Flux<String> chatStream(String content, String agentCode) {
        AiAgent agent = agentService.getByCode(agentCode);
        if (agent == null) {
            throw new RuntimeException("Agent 不存在: " + agentCode);
        }

        // 使用 Spring AI 的流式 API，返回真正的流式输出
        return getChatClient().prompt()
                .system(agent.getSystemPrompt())
                .user(content)
                .stream()
                .content();
    }
}
