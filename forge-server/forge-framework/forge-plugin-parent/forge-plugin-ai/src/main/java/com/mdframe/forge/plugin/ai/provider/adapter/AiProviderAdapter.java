package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import org.springframework.ai.chat.model.ChatModel;

/**
 * AI 供应商模型适配器。
 */
public interface AiProviderAdapter {

    /**
     * 返回稳定适配器代码。
     *
     * @return 适配器代码
     */
    String adapterCode();

    /**
     * 校验供应商配置和运行参数。
     *
     * @param provider 供应商配置
     * @param options 通用运行参数
     */
    void validate(AiProvider provider, AiModelRuntimeOptions options);

    /**
     * 创建供应商 ChatModel。
     *
     * @param provider 供应商配置
     * @param options 通用运行参数
     * @return ChatModel
     */
    ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options);
}
