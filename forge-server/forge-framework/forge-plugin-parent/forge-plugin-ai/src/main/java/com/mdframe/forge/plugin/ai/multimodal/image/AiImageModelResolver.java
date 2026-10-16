package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.model.constant.AiModelType;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 同时保护页面与 Agent 入口；排队后重新检查，不能使用请求时缓存的凭据和实体。 */
@Component
@RequiredArgsConstructor
public class AiImageModelResolver {
    private final AiModelMapper models;
    private final AiProviderMapper providers;

    public Binding require(Long modelId) {
        var caller = AgentCallerGuard.requireCaller();
        AiModel model = modelId == null ? null : models.selectEnabledById(modelId);
        if (model == null || !Objects.equals(model.getTenantId(), caller.getTenantId())
                || AiModelType.fromCode(model.getModelType()) != AiModelType.IMAGE_GENERATION) {
            throw new BusinessException("图像模型不存在、已停用或不可访问");
        }
        AiProvider provider = providers.selectEnabledById(model.getProviderId());
        if (provider == null || !Objects.equals(provider.getTenantId(), caller.getTenantId())) {
            throw new BusinessException("图像供应商不存在、已停用或不可访问");
        }
        return new Binding(model, provider);
    }

    public record Binding(AiModel model, AiProvider provider) { }
}
