package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolBindingResolver;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
import com.mdframe.forge.plugin.ai.routing.AiModelRouter;
import com.mdframe.forge.plugin.ai.routing.RouteDecision;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteReason;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelRouteSource;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.file.core.FileManager;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentRuntimeFactoryTest {

    private final AiAgentService agents = mock(AiAgentService.class);
    private final AiProviderAdapterRegistry providers = mock(AiProviderAdapterRegistry.class);
    private final AiProviderMapper mapper = mock(AiProviderMapper.class);
    private final FileManager files = mock(FileManager.class);
    private final AgentToolBindingResolver bindings = mock(AgentToolBindingResolver.class);
    private final AiModelRouter router = mock(AiModelRouter.class);
    private final AiModelMapper modelMapper = mock(AiModelMapper.class);
    private final AgentRuntimeFactory factory = new AgentRuntimeFactory(
            agents, providers, mapper, files, bindings, router, modelMapper);

    @Test
    void should_reloadCurrentRuntimeWithoutExpandingBudget_when_confirmationResumes() {
        AiAgent configuration = configuration();
        AiProvider provider = new AiProvider();
        provider.setTenantId(7L);
        ChatModel currentModel = mock(ChatModel.class);
        List<AgentTool> currentTools = List.of(mock(AgentTool.class));
        when(agents.getByCode("fixture-agent")).thenReturn(configuration);
        when(mapper.selectById(81L)).thenReturn(provider);
        when(providers.createChatModel(eq(provider), any())).thenReturn(currentModel);
        when(bindings.resolve(configuration)).thenReturn(currentTools);
        when(files.getAccessUrl("101", 3600)).thenReturn("https://fixture.invalid/fresh-image");
        ReactContext context = context();
        context.setTurnIndex(2);
        context.setMaxIters(3);
        context.setSystemPrompt("old prompt");
        try (var ignored = LoginUserContextHolder.open(user())) {
            factory.restore(context);
            configuration.setMaxIters(2);
            factory.restore(context);
        }
        assertThat(context.getChatModel()).isSameAs(currentModel);
        assertThat(context.getBoundTools()).isSameAs(currentTools);
        assertThat(context.getToolGroupMode()).isEqualTo("fixture-current-mode");
        assertThat(context.getSystemPrompt()).isEqualTo("current prompt");
        assertThat(context.getTurnIndex()).isEqualTo(2);
        assertThat(context.getMaxIters()).isEqualTo(2);
        assertThat(context.getFileUrlResolver().apply(101L)).endsWith("fresh-image");
        verify(providers, org.mockito.Mockito.times(2)).createChatModel(provider,
                new AiModelRuntimeOptions("fixture-model", 0.42, 64));
    }

    @Test
    void should_failBeforeModelOrBindingAccess_when_agentMissingOrForeign() {
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> factory.requireAgent("missing")).hasMessageContaining("停用");
            AiAgent foreign = configuration();
            foreign.setTenantId(9L);
            when(agents.getByCode("foreign")).thenReturn(foreign);
            assertThatThrownBy(() -> factory.requireAgent("foreign")).hasMessageContaining("无权");
        }
        verifyNoInteractions(providers, bindings, mapper);
    }

    @Test
    void should_rejectRecreatedAgent_when_codeMatchesButIdChanged() {
        AiAgent configuration = configuration();
        configuration.setId(22L);
        when(agents.getByCode("fixture-agent")).thenReturn(configuration);
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> factory.restore(context())).hasMessageContaining("不匹配");
        }
        verifyNoInteractions(mapper, providers, bindings);
    }

    @Test
    void should_rejectForeignProvider_when_restoringOwnedAgent() {
        AiProvider foreign = new AiProvider();
        foreign.setTenantId(9L);
        when(mapper.selectById(81L)).thenReturn(foreign);
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> factory.attach(context(), configuration())).hasMessageContaining("不可访问");
        }
        verifyNoInteractions(providers, bindings);
    }

    @Test
    void should_fallbackToTenantDefaultProvider_when_pinnedProviderDeleted() {
        AiAgent agent = configuration();
        AiProvider fallback = new AiProvider();
        fallback.setId(99L);
        fallback.setTenantId(7L);
        fallback.setStatus("0");
        fallback.setDefaultModel("qwen-plus");
        ChatModel chatModel = mock(ChatModel.class);
        when(mapper.selectById(81L)).thenReturn(null);
        when(mapper.selectDefaultProvider()).thenReturn(fallback);
        when(providers.createChatModel(eq(fallback), any())).thenReturn(chatModel);
        when(bindings.resolve(agent)).thenReturn(List.of());

        ReactContext context = context();
        try (var ignored = LoginUserContextHolder.open(user())) {
            factory.attach(context, agent);
        }

        assertThat(context.getChatModel()).isSameAs(chatModel);
        verify(providers).createChatModel(fallback, new AiModelRuntimeOptions("qwen-plus", 0.42, 64));
    }

    @Test
    void should_fallbackToFirstEnabledProviderAndModelTable_when_providerUnsetAndNoDefaultFlag() {
        AiAgent agent = configuration();
        agent.setProviderId(null);
        agent.setModelName(null);
        AiProvider enabled = new AiProvider();
        enabled.setId(100L);
        enabled.setTenantId(7L);
        enabled.setStatus("0");
        ChatModel chatModel = mock(ChatModel.class);
        when(mapper.selectDefaultProvider()).thenReturn(null);
        when(mapper.selectFirstEnabledProvider()).thenReturn(enabled);
        when(modelMapper.selectEnabledDefaultModelId(100L)).thenReturn("deepseek-chat");
        when(providers.createChatModel(eq(enabled), any())).thenReturn(chatModel);
        when(bindings.resolve(agent)).thenReturn(List.of());

        ReactContext context = context();
        try (var ignored = LoginUserContextHolder.open(user())) {
            factory.attach(context, agent);
        }

        assertThat(context.getChatModel()).isSameAs(chatModel);
        verify(providers).createChatModel(enabled, new AiModelRuntimeOptions("deepseek-chat", 0.42, 64));
    }

    @Test
    void should_resolveViaModelRouter_when_agentUsesPolicyMode() {
        AiAgent agent = configuration();
        agent.setModelSelectionMode("POLICY");
        agent.setProviderId(null);
        agent.setRoutePolicyId(501L);
        AiProvider routedProvider = new AiProvider();
        routedProvider.setId(105L);
        routedProvider.setTenantId(7L);
        AiModel routedModel = new AiModel();
        routedModel.setModelId("gpt-4o-mini");
        ChatModel chatModel = mock(ChatModel.class);
        when(router.preview(any())).thenReturn(new RouteDecision(
                routedProvider, routedModel, AiModelRouteSource.POLICY, AiModelRouteReason.POLICY_PRIORITY, 501L, List.of()));
        when(providers.createChatModel(eq(routedProvider), any())).thenReturn(chatModel);
        when(bindings.resolve(agent)).thenReturn(List.of());

        ReactContext context = context();
        try (var ignored = LoginUserContextHolder.open(user())) {
            factory.attach(context, agent);
        }

        assertThat(context.getChatModel()).isSameAs(chatModel);
        verify(providers).createChatModel(routedProvider, new AiModelRuntimeOptions("gpt-4o-mini", 0.42, 64));
    }

    private AiAgent configuration() {
        AiAgent agent = new AiAgent();
        agent.setId(21L);
        agent.setTenantId(7L);
        agent.setAgentCode("fixture-agent");
        agent.setProviderId(81L);
        agent.setModelName("fixture-model");
        agent.setTemperature(new BigDecimal("0.42"));
        agent.setMaxTokens(64);
        agent.setMaxIters(10);
        agent.setToolGroupMode("fixture-current-mode");
        agent.setSystemPrompt("current prompt");
        return agent;
    }

    private ReactContext context() {
        ReactContext context = new ReactContext();
        context.setAgentCode("fixture-agent");
        context.setAgentId(21L);
        context.setTenantId(7L);
        context.setUserId(31L);
        return context;
    }

    private LoginUser user() {
        LoginUser user = new LoginUser();
        user.setUserId(31L);
        user.setTenantId(7L);
        return user;
    }
}
