package com.mdframe.forge.plugin.ai.agent.engine;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentLoopTaskExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolExecutor;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReactLoopTerminalTest {

    @Test
    void should_emitSafeFailedTerminal_when_modelStreamFails() {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class)))
                    .thenReturn(Flux.error(new IllegalStateException("synthetic-private-provider-body")));
            var events = fixture.run();
            assertThat(JSON.parseObject(last(events).getData()).getString("error"))
                    .isEqualTo("AI 模型调用失败，请稍后重试");
            assertThat(events).anyMatch(event -> event.getEventType() == AgentEventType.MODEL_CALL_END
                    && event.getData().contains("error"));
            assertThat(events).allMatch(event -> event.getData() == null
                    || !event.getData().contains("synthetic-private-provider-body"));
            assertThat(fixture.terminated).hasValue(1);
            verifyNoInteractions(fixture.tools);
        }
    }

    @Test
    void should_failInsteadOfClaimingSuccess_when_modelReturnsNoResponse() {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class))).thenReturn(Flux.empty());
            assertThat(JSON.parseObject(last(fixture.run()).getData()).getString("error"))
                    .isEqualTo("AI 模型调用失败，请稍后重试");
            assertThat(fixture.terminated).hasValue(1);
        }
    }

    @Test
    void should_emitBudgetFailure_when_iterationLimitIsReached() {
        try (var fixture = new Fixture()) {
            fixture.context.setMaxIters(0);
            var events = fixture.run();
            assertThat(events).extracting(AgentEvent::getEventType).containsExactly(
                    AgentEventType.AGENT_START, AgentEventType.EXCEED_MAX_ITERS, AgentEventType.AGENT_END);
            assertThat(JSON.parseObject(last(events).getData()).getString("error"))
                    .isEqualTo("超过最大迭代次数");
            verify(fixture.model, never()).stream(any(Prompt.class));
            assertThat(fixture.terminated).hasValue(1);
        }
    }

    @Test
    void should_keepCancellationDistinctFromModelFailure_when_stopArrives() {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class))).thenAnswer(call -> {
                fixture.context.setCancelled(true);
                return Flux.error(new IllegalStateException("synthetic cancellation"));
            });
            var events = fixture.run();
            var terminal = JSON.parseObject(last(events).getData());
            assertThat(terminal.getBooleanValue("aborted")).isTrue();
            assertThat(terminal.containsKey("error")).isFalse();
            assertThat(fixture.terminated).hasValue(1);
            verifyNoInteractions(fixture.tools);
        }
    }

    @Test
    void should_keepNormalOutput_and_releaseOnlyOnce_when_modelCompletes() {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class))).thenReturn(Flux.just(new ChatResponse(
                    List.of(new Generation(AssistantMessage.builder().content("合成回答").build())))));
            var events = fixture.run();
            assertThat(last(events).getEventType()).isEqualTo(AgentEventType.AGENT_END);
            assertThat(JSON.parseObject(last(events).getData()).containsKey("error")).isFalse();
            assertThat(events).anyMatch(event -> event.getEventType() == AgentEventType.TEXT_BLOCK_DELTA
                    && event.getData().contains("合成回答"));
            assertThat(fixture.terminated).hasValue(1);
            verifyNoInteractions(fixture.tools);
        }
    }

    private static AgentEvent last(List<AgentEvent> events) {
        return events.get(events.size() - 1);
    }

    private static final class Fixture implements AutoCloseable {
        private final ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        private final ChatModel model = mock(ChatModel.class);
        private final AgentToolExecutor tools = mock(AgentToolExecutor.class);
        private final ReactContext context = new ReactContext();
        private final AtomicInteger terminated = new AtomicInteger();
        private final ReactLoop loop;

        private Fixture() {
            pool.setCorePoolSize(1);
            pool.setMaxPoolSize(1);
            pool.setQueueCapacity(1);
            pool.initialize();
            context.setSessionId("synthetic-terminal-session");
            context.setUserId(31L);
            context.setTenantId(7L);
            context.setAgentCode("synthetic-terminal-agent");
            context.setMaxIters(3);
            context.setChatModel(model);
            context.setBoundTools(List.of());
            loop = new ReactLoop(mock(AgentEventPublisher.class), mock(AiModelInvocationRecorder.class), tools,
                    new AgentLoopTaskExecutor(pool, new AgentExecutionTaskDecorator()));
        }

        private List<AgentEvent> run() {
            // sink 完成通知可能早于 finally，显式等待真实释放回调，不靠调大超时隐藏泄漏。
            var user = new LoginUser();
            user.setUserId(31L);
            user.setTenantId(7L);
            try (var ignored = LoginUserContextHolder.open(user)) {
                var events = loop.run(context, terminated::incrementAndGet).collectList()
                        .block(Duration.ofSeconds(3));
                org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                        .untilAsserted(() -> assertThat(terminated).hasValue(1));
                return events;
            }
        }

        @Override
        public void close() {
            pool.shutdown();
        }
    }
}
