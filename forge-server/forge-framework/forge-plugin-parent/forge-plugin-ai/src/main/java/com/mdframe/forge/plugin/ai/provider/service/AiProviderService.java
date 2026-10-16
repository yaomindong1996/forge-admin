package com.mdframe.forge.plugin.ai.provider.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.constant.AiModelType;
import com.mdframe.forge.plugin.ai.model.dto.AiModelSaveDTO;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderBaseUrlPolicy;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.dto.AiModelImportItem;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderTestDTO;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderCacheEvictionScheduler;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderSecretMasker;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.provider.support.ProviderModelFetcher;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderVO;
import com.mdframe.forge.plugin.ai.model.adapter.AiModelAdapterRegistry;
import com.mdframe.forge.plugin.ai.model.adapter.AiEmbeddingModelAdapter;
import com.mdframe.forge.plugin.ai.model.adapter.AiRerankModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.image.adapter.AiImageModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.AiAsrModelAdapter;
import com.mdframe.forge.plugin.ai.multimodal.voice.adapter.AiTtsModelAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.mdframe.forge.plugin.ai.health.AiModelHealthRegistry;
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
    private final AiModelAdapterRegistry modelAdapterRegistry;
    private final AiProviderCacheEvictionScheduler evictionScheduler;
    private final AiModelService modelService;
    private final AiModelHealthRegistry healthRegistry;
    private final AiSecretCrypto aiSecretCrypto;
    private final ProviderModelFetcher providerModelFetcher;

    /**
     * 获取默认供应商。
     *
     * @return 默认供应商
     */
    public AiProvider getDefaultProvider() {
        return baseMapper.selectDefaultProvider();
    }

    public AiProvider requireEnabledDefaultProvider() {
        List<AiProvider> providers = baseMapper.selectEnabledDefaultProviders();
        if (providers == null || providers.isEmpty()) {
            throw new BusinessException("未配置可用的默认 AI 供应商");
        }
        if (providers.size() > 1) {
            throw new BusinessException("当前租户存在多个默认 AI 供应商");
        }
        return providers.get(0);
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
        provider.setIsDefault(AiConstants.IS_DEFAULT_NO);
        provider.setAdapterCode(resolveCreateAdapterCode(request.getAdapterCode()));
        provider.setApiKey(aiSecretCrypto.encrypt(requireSecret(request.getApiKey())));
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
        String resolvedSecret = resolveUpdateSecret(request.getApiKey(), persistedSecret);
        // 如果 resolveUpdateSecret 返回的是 persisted 密文（未修改），直接保留；否则加密新明文
        provider.setApiKey(resolvedSecret == persistedSecret ? persistedSecret : aiSecretCrypto.encrypt(resolvedSecret));
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
        String modelType = request.getModelType();
        // 未指定或 Chat 类多模态理解模型（视觉/视频/音频理解均走 OpenAI 兼容 Chat 协议）走 ChatModel 路径
        if (!StringUtils.hasText(modelType) || isChatCapableType(modelType)) {
            return testChatConnection(provider);
        }
        return testNonChatConnection(provider, modelType, request);
    }

    /**
     * Chat 模型连接测试（现有逻辑）。
     */
    private String testChatConnection(AiProvider provider) {
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
            resetProviderHealth(provider);
            return buildTestResult(model, content, reasoningContent);
        } catch (BusinessException e) {
            log.warn("[AI供应商测试] 配置校验失败, providerId={}, adapterCode={}, exceptionType={}",
                    provider.getId(), provider.getAdapterCode(), e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(e);
            log.warn("[AI供应商测试] 连接失败, providerId={}, adapterCode={}, exceptionType={}, "
                            + "httpStatus={}, errorCode={}",
                    provider.getId(), provider.getAdapterCode(), e.getClass().getSimpleName(),
                    diagnostics.httpStatus(), diagnostics.errorCode());
            throw new BusinessException(CONNECTION_TEST_FAILURE_MESSAGE);
        }
    }

    /**
     * 判断模型类型是否通过 OpenAI 兼容 Chat 协议连接（多模态理解模型亦为对话协议）。
     * 未知类型按 Chat 处理，避免非 Chat 分发报错。
     */
    private boolean isChatCapableType(String modelType) {
        AiModelType type = AiModelType.fromCode(modelType);
        return type == null
                || type == AiModelType.CHAT
                || type == AiModelType.VISION
                || type == AiModelType.VIDEO_UNDERSTANDING
                || type == AiModelType.AUDIO_UNDERSTANDING;
    }

    /**
     * 非 Chat 类型连接测试（Embedding/Rerank/Image/ASR/TTS/Video），通过 model 层适配器分发。
     */
    private String testNonChatConnection(AiProvider provider, String modelType, AiProviderTestDTO request) {
        AiModelType type = AiModelType.fromCode(modelType);
        if (type == null) {
            throw new BusinessException("不支持的模型类型: " + modelType);
        }
        String model = resolveNonChatTestModel(provider, modelType, request);
        String baseUrl = AiProviderBaseUrlPolicy.normalizeAndValidate(
                provider.getAdapterCode(), provider.getProviderType(), provider.getBaseUrl());
        String apiKey = provider.getId() != null && aiSecretCrypto.isEncrypted(provider.getApiKey())
                ? aiSecretCrypto.decrypt(provider.getApiKey()) : provider.getApiKey();
        try {
            String result = dispatchNonChatTest(type, model, baseUrl, apiKey);
            log.info("[AI供应商测试] 连接成功, providerId={}, adapterCode={}, modelType={}, model={}",
                    provider.getId(), provider.getAdapterCode(), type.getCode(), model);
            resetProviderHealth(provider);
            return result;
        } catch (BusinessException e) {
            log.warn("[AI供应商测试] 配置校验失败, providerId={}, adapterCode={}, modelType={}, exceptionType={}",
                    provider.getId(), type.getCode(), e.getClass().getSimpleName());
            throw e;
        } catch (Exception e) {
            AiProviderFailureDiagnostics diagnostics = AiProviderFailureDiagnostics.from(e);
            log.warn("[AI供应商测试] 连接失败, providerId={}, adapterCode={}, modelType={}, "
                            + "exceptionType={}, httpStatus={}, errorCode={}",
                    provider.getId(), type.getCode(), e.getClass().getSimpleName(),
                    diagnostics.httpStatus(), diagnostics.errorCode());
            throw new BusinessException(CONNECTION_TEST_FAILURE_MESSAGE);
        }
    }

    /**
     * 按模型类型分发到对应适配器执行测试。
     */
    private String dispatchNonChatTest(AiModelType type, String model, String baseUrl, String apiKey) {
        return switch (type) {
            case EMBEDDING -> testEmbeddingConnection(model, baseUrl, apiKey);
            case RERANK -> testRerankConnection(model, baseUrl, apiKey);
            case IMAGE_GENERATION -> testImageConnection(model, baseUrl, apiKey);
            case TTS -> testTtsConnection(model, baseUrl, apiKey);
            case ASR -> throw new BusinessException("ASR模型需音频输入，请通过其他类型模型测试供应商连接");
            case VIDEO_GENERATION -> throw new BusinessException("视频生成模型暂无连接测试支持");
            default -> throw new BusinessException("不支持的模型类型: " + type.getCode());
        };
    }

    private String testEmbeddingConnection(String model, String baseUrl, String apiKey) {
        AiEmbeddingModelAdapter adapter = modelAdapterRegistry.getEmbedding(model);
        List<List<Float>> vectors = adapter.embed(baseUrl, apiKey, model, List.of("connection test"));
        int dimension = vectors != null && !vectors.isEmpty() && vectors.get(0) != null
                ? vectors.get(0).size() : 0;
        return "连接成功\n模型: " + model + "\n向量维度: " + dimension;
    }

    private String testRerankConnection(String model, String baseUrl, String apiKey) {
        AiRerankModelAdapter adapter = modelAdapterRegistry.getRerank(model);
        List<Float> scores = adapter.rerank(baseUrl, apiKey, model, "test",
                List.of("test passage"));
        float score = scores != null && !scores.isEmpty() ? scores.get(0) : 0;
        return "连接成功\n模型: " + model + "\n重排分数: " + score;
    }

    private String testImageConnection(String model, String baseUrl, String apiKey) {
        modelAdapterRegistry.getImage(model).generate(baseUrl, apiKey, model, "test", null, "256x256");
        return "连接成功\n模型: " + model;
    }

    private String testTtsConnection(String model, String baseUrl, String apiKey) {
        AiTtsModelAdapter adapter = modelAdapterRegistry.getTts(model);
        byte[] audio = adapter.synthesize(baseUrl, apiKey, model, "connection test");
        long size = audio != null ? audio.length : 0;
        return "连接成功\n模型: " + model + "\n音频大小: " + size + " bytes";
    }

    /**
     * 为非 Chat 类型解析测试模型标识。
     * 已保存的供应商从 DB 中查找已启用的对应类型模型；未保存的（inline）使用提交的 defaultModel。
     */
    private String resolveNonChatTestModel(AiProvider provider, String modelType, AiProviderTestDTO request) {
        if (provider.getId() != null) {
            return modelService.findEnabledModelIdByType(provider.getId(), modelType);
        }
        if (!StringUtils.hasText(request.getDefaultModel())) {
            throw new BusinessException("未保存供应商测试必须指定默认模型");
        }
        return request.getDefaultModel().trim();
    }

    /**
     * 拉取供应商可用模型列表（调 OpenAI 兼容的 /v1/models 端点）。
     *
     * @param providerId 供应商 ID
     * @return 拉取到的模型列表
     */
    public List<ProviderModelFetcher.FetchedModel> fetchModels(Long providerId) {
        AiProvider provider = requireProvider(providerId);
        String apiKey = aiSecretCrypto.isEncrypted(provider.getApiKey())
                ? aiSecretCrypto.decrypt(provider.getApiKey()) : provider.getApiKey();
        return providerModelFetcher.fetch(provider.getBaseUrl(), apiKey);
    }

    /**
     * 批量导入模型到供应商。
     * 已存在的模型自动跳过；未导入时从勾选的第一个设为默认；
     * 模型类型优先使用前端传入值，为空时根据模型标识启发式推断（{@link AiModelType#inferFromModelId}）。
     *
     * @param providerId 供应商 ID
     * @param items 导入项列表（每项含 modelId 和可选的 modelType）
     * @return 实际导入数量（跳过已存在）
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchImportModels(Long providerId, List<AiModelImportItem> items) {
        if (providerId == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        requireProvider(providerId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException("请选择要导入的模型");
        }

        java.util.Set<String> existing = new java.util.HashSet<>(
                modelService.listAllByProviderId(providerId).stream()
                        .map(com.mdframe.forge.plugin.ai.model.domain.AiModel::getModelId)
                        .toList());

        int imported = 0;
        boolean hasDefault = modelService.getDefaultModelId(providerId) != null;
        for (AiModelImportItem item : items) {
            String trimmed = item.getModelId() == null ? "" : item.getModelId().trim();
            if (!StringUtils.hasText(trimmed) || existing.contains(trimmed)) {
                continue;
            }
            // 优先使用前端传入的类型，为空时启发式推断
            String modelType = StringUtils.hasText(item.getModelType())
                    ? AiModelType.fromCode(item.getModelType()) != null
                        ? item.getModelType()
                        : AiModelType.inferFromModelId(trimmed).getCode()
                    : AiModelType.inferFromModelId(trimmed).getCode();

            AiModelSaveDTO dto = new AiModelSaveDTO();
            dto.setProviderId(providerId);
            dto.setModelType(modelType);
            dto.setModelId(trimmed);
            dto.setModelName(trimmed);
            dto.setStatus(AiConstants.STATUS_NORMAL);
            // 尚未有默认模型时，第一个导入的设为默认
            if (!hasDefault) {
                dto.setIsDefault(AiConstants.IS_DEFAULT_YES);
                hasDefault = true;
            } else {
                dto.setIsDefault(AiConstants.IS_DEFAULT_NO);
            }
            modelService.addModel(dto);
            existing.add(trimmed);
            imported++;
        }
        // 双写同步供应商的 models / defaultModel 摘要
        if (imported > 0) {
            updateModelSummary(providerId,
                    com.alibaba.fastjson2.JSON.toJSONString(modelService.getModelIdListByProviderId(providerId)),
                    modelService.getDefaultModelId(providerId));
        }
        log.info("[AI供应商] 批量导入模型, providerId={}, imported={}", providerId, imported);
        return imported;
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

    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> lockAllForDefaultSwitch() {
        List<Long> providerIds = baseMapper.selectIdsForDefaultSwitch();
        return providerIds == null ? List.of() : providerIds;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void switchDefaultProvider(Long id) {
        baseMapper.clearDefaultProviders();
        if (baseMapper.markDefaultProvider(id) <= 0) {
            throw new BusinessException("设置默认 AI 供应商失败");
        }
        log.info("[AI供应商] 设为默认供应商, id={}", id);
    }

    public void updateModelSummary(Long providerId, String models, String defaultModel) {
        if (providerId == null || baseMapper.updateModelSummary(providerId, models, defaultModel) <= 0) {
            throw new BusinessException("AI供应商模型摘要同步失败");
        }
    }

    public void lockForModelSummary(Long providerId) {
        if (providerId == null || baseMapper.selectIdForUpdate(providerId) == null) {
            throw new BusinessException("AI供应商不存在");
        }
    }

    private void applySaveRequest(AiProvider provider, AiProviderSaveDTO request) {
        setIfPresent(request.getProviderName(), provider::setProviderName);
        setIfPresent(request.getProviderType(), provider::setProviderType);
        setIfPresent(request.getLogo(), provider::setLogo);
        setIfPresent(request.getBaseUrl(), provider::setBaseUrl);
        setIfPresent(request.getModels(), provider::setModels);
        setIfPresent(request.getDefaultModel(), provider::setDefaultModel);
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
        // apiKey 在 createProvider/updateProvider 中已加密落库，此处仅校验非空
        if (!StringUtils.hasText(provider.getApiKey())) {
            throw new BusinessException("API Key不能为空");
        }
        provider.setBaseUrl(AiProviderBaseUrlPolicy.normalizeAndValidate(
                provider.getAdapterCode(), provider.getProviderType(), provider.getBaseUrl()));
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
        if (provider.getId() != null) {
            return modelService.requireEnabledDefaultModelId(provider.getId());
        }
        if (!StringUtils.hasText(provider.getDefaultModel())) {
            throw new BusinessException("请为供应商设置默认模型");
        }
        return provider.getDefaultModel().trim();
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

    private void resetProviderHealth(AiProvider provider) {
        if (provider != null && provider.getId() != null && provider.getTenantId() != null) {
            healthRegistry.resetProvider(provider.getTenantId(), provider.getId());
        }
    }
}
