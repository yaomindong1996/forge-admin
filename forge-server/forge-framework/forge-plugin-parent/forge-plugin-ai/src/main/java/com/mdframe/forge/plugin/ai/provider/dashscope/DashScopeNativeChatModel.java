package com.mdframe.forge.plugin.ai.provider.dashscope;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/** Spring AI 2 原生模型仅声明工具；执行循环由调用方的 Advisor 或 Forge ReactLoop 掌管。 */
public final class DashScopeNativeChatModel implements ChatModel {

    private final DashScopeNativeClient client;
    private final ChatOptions options;

    public DashScopeNativeChatModel(String baseUrl, String apiKey, ChatOptions options) {
        this.client = new DashScopeNativeClient(baseUrl, apiKey);
        this.options = options;
    }

    @Override
    public ChatOptions getOptions() {
        return options;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        var request = DashScopeChatRequestMapper.request(prompt, options, false);
        return new DashScopeChatResponseMapper().response(client.post(request.path(), request.body()),
                request.body().path("model").asText(), false);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        // 每个订阅单独维护工具分片状态，缓存 ChatModel 时不同会话不会串数据。
        return Flux.defer(() -> {
            var request = DashScopeChatRequestMapper.request(prompt, options, true);
            var mapper = new DashScopeChatResponseMapper();
            return client.stream(request.path(), request.body())
                    .map(json -> mapper.response(json, request.body().path("model").asText(), true))
                    .concatWith(Flux.defer(() -> {
                        mapper.assertComplete();
                        return Flux.empty();
                    }));
        });
    }
}
