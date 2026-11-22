package com.mdframe.forge.plugin.ai.health;

import com.mdframe.forge.plugin.ai.model.constant.AiModelType;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.support.AiProviderFailureDiagnostics;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.util.List;
import com.mdframe.forge.starter.core.enums.EnableStatus;

@Slf4j @Service @RequiredArgsConstructor
public class AiModelConnectionTestService {
    private final AiModelMapper modelMapper;
    private final AiProviderService providerService;
    private final AiProviderAdapterRegistry adapterRegistry;
    private final AiModelHealthRegistry healthRegistry;
    private final AiModelFailureClassifier failureClassifier;

    public String test(Long modelPk) {
        AiModel model = modelMapper.selectEnabledById(modelPk);
        if (model == null) throw new BusinessException("模型不存在或已停用");
        AiProvider provider = providerService.getById(model.getProviderId());
        if (provider == null || !EnableStatus.DISABLED.matches(provider.getStatus())) throw new BusinessException("模型供应商不存在或已停用");
        Long tenantId = model.getTenantId() != null ? model.getTenantId() : SessionHelper.getTenantId();
        if (tenantId == null) throw new BusinessException("无法确定当前模型所属租户");

        AiModelType modelType = AiModelType.fromCode(model.getModelType());
        if (modelType == null) {
            modelType = AiModelType.CHAT;
        }

        // 按模型类型路由连接测试（多模态理解模型走 Chat 协议）
        return switch (modelType) {
            case CHAT, VISION, VIDEO_UNDERSTANDING, AUDIO_UNDERSTANDING -> testChatModel(model, provider, tenantId);
            case EMBEDDING -> testEmbeddingModel(model, provider, tenantId);
            case RERANK -> testRerankModel(model, provider, tenantId);
            case IMAGE_GENERATION, ASR, TTS, VIDEO_GENERATION -> {
                log.info("[AI模型测试] 类型{}连接测试暂未实现, modelId={}", modelType.getCode(), model.getId());
                throw new BusinessException(modelType.getCode() + "类型模型连接测试暂未实现，将在后续版本支持");
            }
        };
    }

    private String testChatModel(AiModel model, AiProvider provider, Long tenantId) {
        AiModelHealthKey key = new AiModelHealthKey(tenantId, provider.getId(), model.getId());
        AiModelHealthLease lease = healthRegistry.acquireManualProbe(key);
        try {
            adapterRegistry.createChatModel(provider, new AiModelRuntimeOptions(model.getModelId(), 0D, 32))
                    .call(new Prompt(List.of(new UserMessage("请只回复 OK"))));
            lease.success();
            log.info("[AI模型测试] Chat连接成功, providerId={}, modelId={}", provider.getId(), model.getId());
            return "连接成功";
        } catch (BusinessException e) {
            lease.cancel();
            throw e;
        } catch (Exception e) {
            return handleTestFailure(e, provider, model, lease);
        } finally {
            lease.close();
        }
    }

    private String testEmbeddingModel(AiModel model, AiProvider provider, Long tenantId) {
        AiModelHealthKey key = new AiModelHealthKey(tenantId, provider.getId(), model.getId());
        AiModelHealthLease lease = healthRegistry.acquireManualProbe(key);
        try {
            // 调供应商 Embedding 接口，返回非空向量即视为连接成功
            float[] vector = adapterRegistry.createEmbeddingModel(provider, model.getModelId()).embed("连接测试");
            if (vector == null || vector.length == 0) {
                throw new BusinessException("Embedding模型返回空向量");
            }
            lease.success();
            log.info("[AI模型测试] Embedding连接成功, providerId={}, modelId={}, dimension={}",
                    provider.getId(), model.getId(), vector.length);
            return "连接成功";
        } catch (BusinessException e) {
            lease.cancel();
            throw e;
        } catch (Exception e) {
            return handleTestFailure(e, provider, model, lease);
        } finally {
            lease.close();
        }
    }

    private String testRerankModel(AiModel model, AiProvider provider, Long tenantId) {
        // Rerank 模型测试：通过适配器注册表获取 Rerank 适配器，调用 rerank 验证返回分数
        // 一期 Rerank 适配器尚未实现，返回提示信息
        log.info("[AI模型测试] Rerank连接测试将在Rerank适配器实现后支持, modelId={}", model.getId());
        throw new BusinessException("Rerank模型连接测试将在Rerank适配器实现后支持");
    }

    private String handleTestFailure(Exception e, AiProvider provider, AiModel model, AiModelHealthLease lease) {
        AiModelFailureCategory category = failureClassifier.classify(e);
        if (category == AiModelFailureCategory.VALIDATION
                || category == AiModelFailureCategory.CONTENT_POLICY
                || category == AiModelFailureCategory.CANCELLED) {
            lease.cancel();
        } else {
            lease.failure(category);
        }
        AiProviderFailureDiagnostics d = AiProviderFailureDiagnostics.from(e);
        log.warn("[AI模型测试] 连接失败, providerId={}, modelId={}, category={}, httpStatus={}, errorCode={}, exceptionType={}",
                provider.getId(), model.getId(), category, d.httpStatus(), d.errorCode(), e.getClass().getSimpleName());
        throw new BusinessException(switch (category) {
            case AUTHENTICATION -> "模型连接失败：API Key 无效或无权限，请检查供应商密钥";
            case MODEL_UNAVAILABLE -> "模型连接失败：模型标识不可用，请检查供应商配置和模型标识";
            case RATE_LIMIT -> "模型连接失败：供应商限流，请稍后重试";
            case TIMEOUT, NETWORK -> "模型连接失败：网络不可达或超时，请检查 Base URL 与出网";
            case PROVIDER_5XX -> "模型连接失败：供应商服务异常，请稍后重试";
            default -> "模型连接失败，请检查供应商配置和模型标识";
        });
    }
}
