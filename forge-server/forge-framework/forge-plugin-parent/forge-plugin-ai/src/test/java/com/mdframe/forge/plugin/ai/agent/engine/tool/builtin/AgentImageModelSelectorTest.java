package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.multimodal.image.AiImageGenerationService;
import com.mdframe.forge.plugin.ai.multimodal.image.domain.AiImageGenerateRecord;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentImageModelSelectorTest {
    private final AiAgentService agents = mock(AiAgentService.class);
    private final AiModelMapper models = mock(AiModelMapper.class);
    private final AiProviderMapper providers = mock(AiProviderMapper.class);
    private final AgentImageModelSelector selector = new AgentImageModelSelector(agents, models, providers);
    private final ToolContext context = ToolContext.of("fixture", 21L, 7L, 0);

    @ParameterizedTest
    @ValueSource(strings = {"image_generation", "image"})
    void selectsOnlyConfiguredEnabledImageModelIncludingLegacyType(String type) {
        var agent = configureAgent("{\"imageModelId\":89}");
        var model = configureModel(type, 7L);
        configureProvider(7L);
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThat(selector.requireModel(context)).isSameAs(model);
            agent.setExtraConfig("{\"imageModelId\":\"89\"}");
            assertThat(selector.requireModel(context)).isSameAs(model);
        }
        verify(models, times(2)).selectEnabledById(89L);
        verify(providers, times(2)).selectEnabledById(81L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "[]", "{", "{\"imageModelId\":0}",
            "{\"imageModelId\":-1}", "{\"imageModelId\":89.0}", "{\"imageModelId\":true}",
            "{\"imageModelId\":\"9223372036854775808\"}", "{\"imageModelId\":89} {}",
            "{\"imageModelId\":89,\"imageModelId\":90}"})
    void rejectsMissingMalformedAmbiguousAndOutOfRangeConfigurationBeforeModelAccess(String config) {
        configureAgent(config);
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("配置");
        }
        verifyNoInteractions(models, providers);
    }

    @Test
    void rejectsAnonymousAndForgedToolTenantBeforeAgentAccess() {
        assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("登录");
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> selector.requireModel(ToolContext.of("fixture", 21L, 9L, 0)))
                    .hasMessageContaining("身份");
        }
        verifyNoInteractions(agents, models, providers);
    }

    @Test
    void rejectsDisabledOrRecreatedAgentBeforeSelectingModel() {
        var agent = configureAgent("{\"imageModelId\":89}");
        try (var ignored = LoginUserContextHolder.open(user())) {
            when(agents.getByCode("agent")).thenReturn(null);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("停用");
            when(agents.getByCode("agent")).thenReturn(agent);
            agent.setId(22L);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("停用");
        }
        verifyNoInteractions(models, providers);
    }

    @Test
    void rejectsForeignAgentModelAndProviderAndNonImageModel() {
        var agent = configureAgent("{\"imageModelId\":89}");
        var model = configureModel("image_generation", 7L);
        var provider = configureProvider(9L);
        try (var ignored = LoginUserContextHolder.open(user())) {
            agent.setTenantId(9L);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("不可访问");
            agent.setTenantId(7L);
            model.setTenantId(9L);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("模型不可用");
            model.setTenantId(7L);
            model.setModelType("chat");
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("模型不可用");
            model.setModelType("image_generation");
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("供应商不可用");
            provider.setTenantId(7L);
            when(providers.selectEnabledById(81L)).thenReturn(null);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("供应商不可用");
            when(models.selectEnabledById(89L)).thenReturn(null);
            assertThatThrownBy(() -> selector.requireModel(context)).hasMessageContaining("模型不可用");
        }
    }

    @Test
    void modelSuppliedIdsProvidersAndSecretsCannotOverrideServerBinding() {
        configureAgent("{\"imageModelId\":89}");
        configureModel("image_generation", 7L);
        configureProvider(7L);
        var service = mock(AiImageGenerationService.class);
        when(service.generate(any())).thenReturn(101L);
        try (var ignored = LoginUserContextHolder.open(user())) {
            var result = new ImageGenerateTool(service, selector).execute(Map.of("prompt", "fixture",
                    "modelId", 90L, "providerId", 999L, "apiKey", "untrusted-fixture-value"), context);
            assertThat(result.isSuccess()).isTrue();
        }
        var record = ArgumentCaptor.forClass(AiImageGenerateRecord.class);
        verify(service).generate(record.capture());
        assertThat(record.getValue().getModelId()).isEqualTo(89L);
        assertThat(record.getValue().getUserId()).isEqualTo(31L);
        verify(models, never()).selectEnabledById(90L);
        verify(providers, never()).selectEnabledById(999L);
    }

    private AiAgent configureAgent(String config) {
        var agent = new AiAgent();
        agent.setId(21L);
        agent.setTenantId(7L);
        agent.setAgentCode("agent");
        agent.setExtraConfig(config);
        when(agents.getById(21L)).thenReturn(agent);
        when(agents.getByCode("agent")).thenReturn(agent);
        return agent;
    }

    private AiModel configureModel(String type, Long tenantId) {
        var model = new AiModel();
        model.setId(89L);
        model.setProviderId(81L);
        model.setTenantId(tenantId);
        model.setModelType(type);
        when(models.selectEnabledById(89L)).thenReturn(model);
        return model;
    }

    private AiProvider configureProvider(Long tenantId) {
        var provider = new AiProvider();
        provider.setId(81L);
        provider.setTenantId(tenantId);
        when(providers.selectEnabledById(81L)).thenReturn(provider);
        return provider;
    }

    private static LoginUser user() {
        var user = new LoginUser();
        user.setUserId(31L);
        user.setTenantId(7L);
        return user;
    }
}
