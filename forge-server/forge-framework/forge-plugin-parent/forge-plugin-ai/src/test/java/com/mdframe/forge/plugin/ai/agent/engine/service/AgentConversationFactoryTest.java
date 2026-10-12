package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactLoop;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.lease.AgentLeaseFixture;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentSessionAccess;
import com.mdframe.forge.plugin.ai.agent.engine.context.ContextTrimmer;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

class AgentConversationFactoryTest {
    private final AgentRuntimeFactory runtime = mock(AgentRuntimeFactory.class);
    private final AiChatRecordService records = mock(AiChatRecordService.class);
    private final AiChatSessionService sessions = mock(AiChatSessionService.class);
    private final AgentConversationFactory conversations = new AgentConversationFactory(runtime, records,
            new ContextTrimmer(), new AgentSessionAccess(sessions));

    @Test
    void seedUsesTrustedIdentityWithoutReadingHistoryOrCreatingSession() {
        try (var ignored = LoginUserContextHolder.open(user())) {
            ReactContext context = conversations.seed(request());
            assertThat(context.getUserId()).isEqualTo(31L);
            assertThat(context.getTenantId()).isEqualTo(7L);
            assertThat(context.getSessionId()).isEqualTo("fixture");
        }
        verifyNoInteractions(sessions, records, runtime);
    }

    @Test
    void initialHistoryAndAttachmentsAreLoadedOnlyAfterSessionOwnership() {
        configureOwned();
        when(records.listRecentBySession("fixture", 20)).thenReturn(List.of(AiChatRecord.builder()
                .role("user").content("old").build()));
        var request = request();
        request.setImageFileIds(List.of(101L));
        try (var ignored = LoginUserContextHolder.open(user())) {
            var context = conversations.seed(request);
            conversations.populate(request, context);
            assertThat(context.getHistory()).hasSize(1);
            assertThat(context.getUserMessage()).isEqualTo("first");
            assertThat(context.getImageFileIds()).containsExactly(101L);
            verify(runtime).attach(context, agent());
        }
        var order = inOrder(sessions, records);
        order.verify(sessions).getOrCreate("fixture", 31L, 7L, "agent", "first");
        order.verify(records).listRecentBySession("fixture", 20);
    }

    @Test
    void foreignSessionCannotReadHistoryModifyMessagesOrStartModel() {
        configureOwned();
        when(sessions.getOrCreate("fixture", 31L, 7L, "agent", "first"))
                .thenReturn(AiChatSession.builder().userId(32L).tenantId(7L).status("0").build());
        var loop = mock(ReactLoop.class);
        var persister = mock(AgentChatPersister.class);
        var engine = engine(loop, persister);
        try (var ignored = LoginUserContextHolder.open(user())) {
            assertThatThrownBy(() -> engine.stream(request())).hasMessageContaining("无权");
        }
        verifyNoInteractions(records, loop, persister);
        verify(runtime, never()).attach(any(), any());
    }

    @Test
    void competingInitialRequestCannotCreateSessionOrReadHistoryAgain() {
        configureOwned();
        when(records.listRecentBySession("fixture", 20)).thenReturn(List.of());
        var loop = mock(ReactLoop.class);
        when(loop.run(any(), any())).thenReturn(Flux.never());
        var persister = mock(AgentChatPersister.class);
        var engine = engine(loop, persister);
        try (var ignored = LoginUserContextHolder.open(user())) {
            engine.stream(request());
            assertThatThrownBy(() -> engine.stream(request())).hasMessageContaining("正在处理中");
        }
        verify(sessions, times(1)).getOrCreate("fixture", 31L, 7L, "agent", "first");
        verify(records, times(1)).listRecentBySession("fixture", 20);
        verify(persister, times(1)).openForStream(any(), eq("agent"));
    }

    private AgentEngineService engine(ReactLoop loop, AgentChatPersister persister) {
        return new AgentEngineService(new ReactAgent(loop, mock(InterruptStore.class), runtime,
                mock(AgentEventPublisher.class), new AgentLeaseFixture().registry()), conversations, persister);
    }

    private void configureOwned() {
        when(runtime.requireAgent("agent")).thenReturn(agent());
        when(sessions.getOrCreate("fixture", 31L, 7L, "agent", "first"))
                .thenReturn(AiChatSession.builder().id("fixture").userId(31L).tenantId(7L).status("0").build());
    }

    private AiAgent agent() {
        var agent = new AiAgent();
        agent.setId(21L);
        agent.setTenantId(7L);
        agent.setAgentCode("agent");
        return agent;
    }

    private ReactRequest request() {
        var request = new ReactRequest();
        request.setSessionId("fixture");
        request.setAgentCode("agent");
        request.setMessage("first");
        return request;
    }

    private static LoginUser user() {
        var user = new LoginUser();
        user.setUserId(31L);
        user.setTenantId(7L);
        return user;
    }
}
