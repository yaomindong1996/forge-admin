package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 供应商适配器注册表。
 */
@Component
public class AiProviderAdapterRegistry {

    private final Map<String, AiProviderAdapter> adapters;

    public AiProviderAdapterRegistry(List<AiProviderAdapter> adapterList) {
        Map<String, AiProviderAdapter> registered = new LinkedHashMap<>();
        for (AiProviderAdapter adapter : adapterList) {
            String code = AiProviderAdapterCode.require(adapter.adapterCode()).getCode();
            AiProviderAdapter previous = registered.putIfAbsent(code, adapter);
            if (previous != null) {
                throw new IllegalStateException("AI供应商连接协议重复注册: " + code);
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    /**
     * 获取已注册适配器。
     *
     * @param adapterCode 适配器代码
     * @return 适配器
     */
    public AiProviderAdapter getRequired(String adapterCode) {
        String code = AiProviderAdapterCode.require(adapterCode).getCode();
        AiProviderAdapter adapter = adapters.get(code);
        if (adapter == null) {
            throw new BusinessException("AI供应商连接协议未注册: " + code);
        }
        return adapter;
    }

    /**
     * 按固定的选择、校验、创建顺序构建模型。
     *
     * @param provider 供应商配置
     * @param options 通用运行参数
     * @return ChatModel
     */
    public ChatModel createChatModel(AiProvider provider, AiModelRuntimeOptions options) {
        if (provider == null) {
            throw new BusinessException("AI供应商配置不能为空");
        }
        if (options == null) {
            throw new BusinessException("AI模型运行参数不能为空");
        }
        AiProviderAdapter adapter = getRequired(provider.getAdapterCode());
        adapter.validate(provider, options);
        return adapter.createChatModel(provider, options);
    }
}
