package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolBindingResolver;
import com.mdframe.forge.plugin.ai.agent.service.AiAgentService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.mapper.AiProviderMapper;
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
    private final AgentRuntimeFactory factory = new AgentRuntimeFactory(agents, providers, mapper, files, bindings);

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
