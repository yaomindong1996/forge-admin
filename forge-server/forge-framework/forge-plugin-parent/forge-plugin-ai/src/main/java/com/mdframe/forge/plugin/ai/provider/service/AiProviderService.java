package com.mdframe.forge.plugin.ai.provider.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderBaseUrlPolicy;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderTestDTO;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderCacheEvictionScheduler;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderSecretMasker;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderService extends ServiceImpl<AiProviderMapper, AiProvider> {

    private static final int CONNECTION_TEST_MAX_TOKENS = 32;
    private static final String CONNECTION_TEST_FAILURE_MESSAGE = "连接失败，请检查供应商配置和网络状态";

    private final AiProviderAdapterRegistry adapterRegistry;
    private final AiProviderCacheEvictionScheduler evictionScheduler;

    /**
     * 获取默认供应商。
     *
     * @return 默认供应商
     */
    public AiProvider getDefaultProvider() {
        return baseMapper.selectDefaultProvider();
    }

    public Page<AiProvider> pageProviders(Integer pageNum, Integer pageSize,
                                          String providerName, String providerType, String status) {
        return baseMapper.selectProviderPage(
                new Page<>(pageNum, pageSize), providerName, providerType, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createProvider(AiProviderSaveDTO request) {
        if (request == null) {
            throw new BusinessException("AI供应商配置不能为空");
        }
        AiProvider provider = new AiProvider();
        applySaveRequest(provider, request);
        provider.setAdapterCode(resolveCreateAdapterCode(request.getAdapterCode()));
        provider.setApiKey(requireSecret(request.getApiKey()));
        normalizeProviderConnection(provider);
        if (!save(provider)) {
            throw new BusinessException("AI供应商新增失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProvider(AiProviderSaveDTO request) {
        if (request == null || request.getId() == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        AiProvider provider = requireProvider(request.getId());
        String persistedSecret = provider.getApiKey();
        String persistedAdapterCode = provider.getAdapterCode();
        applySaveRequest(provider, request);
        provider.setAdapterCode(resolveUpdateAdapterCode(request.getAdapterCode(), persistedAdapterCode));
        provider.setApiKey(resolveUpdateSecret(request.getApiKey(), persistedSecret));
        normalizeProviderConnection(provider);
        if (!updateById(provider)) {
            throw new BusinessException("AI供应商更新失败");
        }
        evictionScheduler.scheduleAfterCommit(provider);
    }

    /**
     * 测试供应商连接。
     *
     * @param request 已保存供应商 ID 或未保存的完整配置
     * @return 安全的连接测试结果
     */
    public String testConnection(AiProviderTestDTO request) {
        AiProvider provider = resolveTestProvider(request);
        String model = resolveTestModel(provider);
        AiModelRuntimeOptions options = new AiModelRuntimeOptions(
                model, 0D, CONNECTION_TEST_MAX_TOKENS);
        try {
            ChatModel chatModel = adapterRegistry.createChatModel(provider, options);
            Prompt prompt = new Prompt(List.of(
                    new UserMessage("请只回复 OK。若为推理模型，也请在最终答案中明确输出 OK。")));
            ChatResponse response = chatModel.call(prompt);
            AssistantMessage message = response != null && response.getResult() != null
                    ? response.getResult().getOutput() : null;
            String content = message != null ? message.getText() : null;
            String reasoningContent = extractReasoningContent(message);
            log.info("[AI供应商测试] 连接成功, providerId={}, adapterCode={}, model={}",
                    provider.getId(), provider.getAdapterCode(), model);
            return buildTestResult(model, content, reasoningContent);
        } catch (BusinessException e) {
            log.warn("[AI供应商测试] 配置校验失败, providerId={}, adapterCode={}, exceptionType={}",
                    provider.getId(), provider.getAdapterCode(), e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            log.warn("[AI供应商测试] 连接失败, providerId={}, adapterCode={}, exceptionType={}",
                    provider.getId(), provider.getAdapterCode(), e.getClass().getSimpleName());
            throw new BusinessException(CONNECTION_TEST_FAILURE_MESSAGE);
        }
    }

    public AiProviderVO toSafeView(AiProvider provider) {
        if (provider == null) {
            return null;
        }
        AiProviderVO view = new AiProviderVO();
        view.setId(provider.getId());
        view.setTenantId(provider.getTenantId());
        view.setProviderName(provider.getProviderName());
        view.setProviderType(provider.getProviderType());
        view.setAdapterCode(provider.getAdapterCode());
        view.setLogo(provider.getLogo());
        view.setApiKey(AiProviderSecretMasker.mask(provider.getApiKey()));
        view.setBaseUrl(provider.getBaseUrl());
        view.setModels(provider.getModels());
        view.setDefaultModel(provider.getDefaultModel());
        view.setIsDefault(provider.getIsDefault());
        view.setStatus(provider.getStatus());
        view.setRemark(provider.getRemark());
        view.setCreateBy(provider.getCreateBy());
        view.setCreateTime(provider.getCreateTime());
        view.setCreateDept(provider.getCreateDept());
        view.setUpdateBy(provider.getUpdateBy());
        view.setUpdateTime(provider.getUpdateTime());
        return view;
    }

    /**
     * 删除供应商。
     *
     * @param id 供应商 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long id) {
        AiProvider provider = requireProvider(id);
        if (!removeById(id)) {
            throw new BusinessException("AI供应商删除失败");
        }
        evictionScheduler.scheduleAfterCommit(provider);
        log.info("[AI供应商] 删除供应商, tenantId={}, providerId={}", provider.getTenantId(), id);
    }

    /**
     * 设为默认供应商（先清除其他默认，再设当前）。
     *
     * @param id 供应商 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_NO)
                .eq(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_YES));
        update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getIsDefault, AiConstants.IS_DEFAULT_YES)
                .eq(AiProvider::getId, id));
        log.info("[AI供应商] 设为默认供应商, id={}", id);
    }

    private void applySaveRequest(AiProvider provider, AiProviderSaveDTO request) {
        setIfPresent(request.getProviderName(), provider::setProviderName);
        setIfPresent(request.getProviderType(), provider::setProviderType);
        setIfPresent(request.getLogo(), provider::setLogo);
        setIfPresent(request.getBaseUrl(), provider::setBaseUrl);
        setIfPresent(request.getModels(), provider::setModels);
        setIfPresent(request.getDefaultModel(), provider::setDefaultModel);
        setIfPresent(request.getIsDefault(), provider::setIsDefault);
        setIfPresent(request.getStatus(), provider::setStatus);
        setIfPresent(request.getRemark(), provider::setRemark);
    }

    private void setIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private String resolveCreateAdapterCode(String submitted) {
        if (submitted == null) {
            return AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode();
        }
        return requireAdapterCode(submitted);
    }

    private String resolveUpdateAdapterCode(String submitted, String persisted) {
        if (submitted == null) {
            return requireAdapterCode(persisted);
        }
        return requireAdapterCode(submitted);
    }

    private String requireAdapterCode(String adapterCode) {
        if (!StringUtils.hasText(adapterCode)) {
            throw new BusinessException("AI供应商连接协议不能为空");
        }
        String code = AiProviderAdapterCode.require(adapterCode.trim()).getCode();
        adapterRegistry.getRequired(code);
        return code;
    }

    private String requireSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException("API Key不能为空");
        }
        return secret.trim();
    }

    private String resolveUpdateSecret(String submitted, String persisted) {
        if (submitted == null || AiProviderSecretMasker.isUnchangedMask(submitted, persisted)) {
            return persisted;
        }
        return requireSecret(submitted);
    }

    private void normalizeProviderConnection(AiProvider provider) {
        if (!StringUtils.hasText(provider.getProviderName())) {
            throw new BusinessException("供应商名称不能为空");
        }
        if (!StringUtils.hasText(provider.getProviderType())) {
            throw new BusinessException("供应商类型不能为空");
        }
        provider.setApiKey(requireSecret(provider.getApiKey()));
        provider.setBaseUrl(AiProviderBaseUrlPolicy.normalizeAndValidate(
                provider.getAdapterCode(), provider.getBaseUrl()));
    }

    private AiProvider resolveTestProvider(AiProviderTestDTO request) {
        if (request == null) {
            throw new BusinessException("连接测试配置不能为空");
        }
        boolean hasInlineConfiguration = hasInlineConfiguration(request);
        if (request.getId() != null) {
            if (hasInlineConfiguration) {
                throw new BusinessException("已保存供应商测试只能提交ID");
            }
            return requireProvider(request.getId());
        }
        if (!hasInlineConfiguration) {
            throw new BusinessException("未保存供应商测试必须提交完整配置");
        }
        AiProvider provider = new AiProvider();
        provider.setProviderName(StringUtils.hasText(request.getProviderName())
                ? request.getProviderName() : "未保存供应商");
        provider.setProviderType(request.getProviderType());
        provider.setAdapterCode(requireAdapterCode(request.getAdapterCode()));
        provider.setApiKey(requireSecret(request.getApiKey()));
        provider.setBaseUrl(request.getBaseUrl());
        if (!StringUtils.hasText(request.getDefaultModel())) {
            throw new BusinessException("默认模型不能为空");
        }
        provider.setDefaultModel(request.getDefaultModel().trim());
        return provider;
    }

    private boolean hasInlineConfiguration(AiProviderTestDTO request) {
        return request.getProviderName() != null
                || request.getProviderType() != null
                || request.getAdapterCode() != null
                || request.getApiKey() != null
                || request.getBaseUrl() != null
                || request.getDefaultModel() != null;
    }

    private AiProvider requireProvider(Long id) {
        if (id == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        AiProvider provider = getById(id);
        if (provider == null) {
            throw new BusinessException("AI供应商不存在");
        }
        return provider;
    }

    private String resolveTestModel(AiProvider provider) {
        if (StringUtils.hasText(provider.getDefaultModel())) {
            return provider.getDefaultModel();
        }
        if (AiProviderAdapterCode.DASHSCOPE_NATIVE.getCode().equals(provider.getAdapterCode())) {
            return "qwen-plus";
        }
        return "gpt-3.5-turbo";
    }

    private String buildTestResult(String model, String content, String reasoningContent) {
        String normalizedContent = StringUtils.hasText(content) ? content.trim() : "";
        String normalizedReasoning = StringUtils.hasText(reasoningContent) ? reasoningContent.trim() : "";

        StringBuilder result = new StringBuilder("连接成功");
        if (StringUtils.hasText(model)) {
            result.append("\n模型: ").append(model);
        }
        if (StringUtils.hasText(normalizedReasoning)) {
            result.append("\n\n思考过程:\n").append(normalizedReasoning);
        }
        if (StringUtils.hasText(normalizedContent)) {
            result.append("\n\n回复内容:\n").append(normalizedContent);
        } else if (StringUtils.hasText(normalizedReasoning)) {
            result.append("\n\n回复内容:\n")
                    .append("(模型已返回思考过程，但未输出可见最终答案。通常是推理模型在测试模式下的正常表现。)");
        } else {
            result.append("\n\n回复内容:\n")
                    .append("(模型已连通，但当前响应未返回可见文本。)");
        }
        return result.toString();
    }

    private String extractReasoningContent(AssistantMessage message) {
        if (message == null || message.getMetadata() == null) {
            return null;
        }
        Map<String, Object> metadata = message.getMetadata();
        Object reasoning = metadata.get("reasoningContent");
        if (reasoning instanceof String reasoningText) {
            return reasoningText;
        }
        reasoning = metadata.get("reasoning_content");
        if (reasoning instanceof String reasoningText) {
            return reasoningText;
        }
        reasoning = metadata.get("reasoning");
        return reasoning instanceof String reasoningText ? reasoningText : null;
    }
}
