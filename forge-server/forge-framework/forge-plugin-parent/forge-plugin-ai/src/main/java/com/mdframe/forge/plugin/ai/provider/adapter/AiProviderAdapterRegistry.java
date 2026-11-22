package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 供应商适配器注册表。
 */
@Component
@Slf4j
public class AiProviderAdapterRegistry {

    private final Map<String, AiProviderAdapter> adapters;
    private final AiSecretCrypto aiSecretCrypto;

    public AiProviderAdapterRegistry(List<AiProviderAdapter> adapterList, AiSecretCrypto aiSecretCrypto) {
        Map<String, AiProviderAdapter> registered = new LinkedHashMap<>();
        for (AiProviderAdapter adapter : adapterList) {
            String code = AiProviderAdapterCode.require(adapter.adapterCode()).getCode();
            AiProviderAdapter previous = registered.putIfAbsent(code, adapter);
            if (previous != null) {
                throw new IllegalStateException("AI供应商连接协议重复注册: " + code);
            }
        }
        this.adapters = Map.copyOf(registered);
        this.aiSecretCrypto = aiSecretCrypto;
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
     * 在构造前解密 apiKey（存储层为密文，使用时需明文）。
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
        // 解密 apiKey：存储层为密文，构造 ChatModel 需要明文
        log.info("[AiProviderAdapterRegistry] 构建ChatModel, providerId={}, providerName={}, adapterCode={}, baseUrl={}, model={}, temperature={}, maxTokens={}, apiKeyDecrypt={}",
                provider.getId(), provider.getProviderName(), provider.getAdapterCode(),
                provider.getBaseUrl(), options.model(), options.temperature(), options.maxTokens(),
                AiSecretCrypto.isEncrypted(provider.getApiKey()));
        AiProvider decryptedProvider = decryptProvider(provider);
        AiProviderAdapter adapter = getRequired(decryptedProvider.getAdapterCode());
        adapter.validate(decryptedProvider, options);
        return adapter.createChatModel(decryptedProvider, options);
    }

    /**
     * 按固定的选择、校验、创建顺序构建 Embedding 模型。
     * 在构造前解密 apiKey（存储层为密文，使用时需明文）。
     *
     * @param provider 供应商配置
     * @param model 模型标识
     * @return EmbeddingModel
     */
    public EmbeddingModel createEmbeddingModel(AiProvider provider, String model) {
        if (provider == null) {
            throw new BusinessException("AI供应商配置不能为空");
        }
        if (!StringUtils.hasText(model)) {
            throw new BusinessException("模型标识不能为空");
        }
        AiProvider decryptedProvider = decryptProvider(provider);
        AiProviderAdapter adapter = getRequired(decryptedProvider.getAdapterCode());
        return adapter.createEmbeddingModel(decryptedProvider, model);
    }

    /**
     * 解密 apiKey：存储层为密文，构造模型需要明文。
     * 未加密时原样返回，避免无谓的对象重建。
     *
     * @param provider 供应商配置
     * @return apiKey 解密后的供应商配置
     */
    private AiProvider decryptProvider(AiProvider provider) {
        String stored = provider.getApiKey();
        if (!StringUtils.hasText(stored)) {
            return provider;
        }
        String plain = aiSecretCrypto.resolvePlainSecret(stored);
        if (plain == null ? stored == null : plain.equals(stored)) {
            return provider;
        }
        AiProvider decryptedProvider = new AiProvider();
        decryptedProvider.setId(provider.getId());
        decryptedProvider.setTenantId(provider.getTenantId());
        decryptedProvider.setProviderName(provider.getProviderName());
        decryptedProvider.setProviderType(provider.getProviderType());
        decryptedProvider.setAdapterCode(provider.getAdapterCode());
        decryptedProvider.setLogo(provider.getLogo());
        decryptedProvider.setApiKey(plain);
        decryptedProvider.setBaseUrl(provider.getBaseUrl());
        decryptedProvider.setModels(provider.getModels());
        decryptedProvider.setDefaultModel(provider.getDefaultModel());
        decryptedProvider.setIsDefault(provider.getIsDefault());
        decryptedProvider.setStatus(provider.getStatus());
        decryptedProvider.setRemark(provider.getRemark());
        return decryptedProvider;
    }
}
