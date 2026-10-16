package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentEnginePersistMetaTest {

    @Test
    void should_writeExactLongIdsAsStrings_in_actualPersistMetaSse() {
        var request = new ReactRequest();
        request.setSessionId("synthetic-meta-session");
        var context = new ReactContext();
        context.setSessionId(request.getSessionId());
        context.setAgentCode("synthetic-meta-agent");
        var conversations = mock(AgentConversationFactory.class);
        var persister = mock(AgentChatPersister.class);
        var agent = mock(ReactAgent.class);
        when(conversations.seed(request)).thenReturn(context);
        when(persister.openForStream(request, context.getAgentCode()))
                .thenReturn(new AgentChatPersister.OpenResult(9007199254740993L, 9007199254740995L));
        when(agent.execute(eq(context), any())).thenAnswer(call -> {
            Consumer<ReactContext> prepare = call.getArgument(1);
            prepare.accept(context);
            return Flux.just(AgentEvent.of(request.getSessionId(), 0, AgentEventType.AGENT_END, "{}"));
        });
        var service = new AgentEngineService(agent, conversations, persister);
        var events = service.stream(request).collectList().block(Duration.ofSeconds(3));
        assertThat(events).hasSize(2);
        assertThat(events.get(0).event()).isEqualTo(AgentEventType.PERSIST_META.getCode());
        var payload = JSON.parseObject(events.get(0).data());
        assertThat(payload.get("userRecordId")).isInstanceOf(String.class).isEqualTo("9007199254740993");
        assertThat(payload.get("assistantRecordId")).isInstanceOf(String.class).isEqualTo("9007199254740995");
    }
}
