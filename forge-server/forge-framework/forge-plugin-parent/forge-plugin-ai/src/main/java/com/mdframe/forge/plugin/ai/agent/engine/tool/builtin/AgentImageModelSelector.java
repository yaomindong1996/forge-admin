package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.constant.AiModelType;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 只从当前服务端 Agent 配置选择图像模型，模型生成的参数不能覆盖配置或供应商。 */
@Component
@RequiredArgsConstructor
public class AgentImageModelSelector {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private final AiAgentService agents;
    private final AiModelMapper models;
    private final AiProviderMapper providers;

    public AiModel requireModel(ToolContext context) {
        var caller = AgentCallerGuard.requireCaller();
        if (context == null || !Objects.equals(context.getTenantId(), caller.getTenantId())) {
            throw new BusinessException(403, "图像工具执行身份无效");
        }
        var stored = agents.getById(context.getAgentId());
        if (stored == null || !Objects.equals(stored.getTenantId(), caller.getTenantId())) {
            throw new BusinessException("Agent 不存在或不可访问");
        }
        var agent = agents.getByCode(stored.getAgentCode());
        if (agent == null || !Objects.equals(agent.getId(), context.getAgentId())
                || !Objects.equals(agent.getTenantId(), caller.getTenantId())) {
            throw new BusinessException("Agent 不存在或已停用");
        }
        AiModel model = models.selectEnabledById(modelId(agent.getExtraConfig()));
        if (model == null || !Objects.equals(model.getTenantId(), caller.getTenantId())
                || AiModelType.fromCode(model.getModelType()) != AiModelType.IMAGE_GENERATION) {
            throw new BusinessException("Agent 绑定的图像模型不可用");
        }
        var provider = providers.selectEnabledById(model.getProviderId());
        if (provider == null || !Objects.equals(provider.getTenantId(), caller.getTenantId())) {
            throw new BusinessException("图像模型供应商不可用");
        }
        return model;
    }

    private Long modelId(String raw) {
        try {
            JsonNode root = JSON.readTree(raw == null ? "{}" : raw);
            JsonNode id = root == null ? null : root.get("imageModelId");
            if (id != null && id.asText().matches("[1-9][0-9]{0,18}")
                    && (id.isTextual() || id.isIntegralNumber())) {
                return Long.valueOf(id.asText());
            }
        } catch (java.io.IOException | NumberFormatException ex) {
            throw new BusinessException("Agent 图像模型配置无效");
        }
        throw new BusinessException("请在 Agent extraConfig 中配置 imageModelId");
    }
}
