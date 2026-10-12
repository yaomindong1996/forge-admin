package com.mdframe.forge.plugin.ai.agent.engine.event.persistence;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactLoop;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentLoopTaskExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.mapper.AgentEventMapper;
import com.mdframe.forge.plugin.ai.agent.engine.event.sse.AgentEventWebFluxStream;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolExecutor;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentEventPipelineTest {
    @Test
    void should_persistShortStreamOnRealTrustedLoopThread_beforeCompletionAndRelease() {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class))).thenReturn(answer());
            var events = fixture.run(context(31L, 7L, "short"));
            assertThat(fixture.stored).extracting(AgentEventEntity::getEventType)
                    .containsExactlyElementsOf(events.stream().map(event -> event.getEventType().getCode()).toList());
            assertThat(fixture.stored).allMatch(row -> row.getTenantId().equals(7L));
            assertThat(fixture.threads).allMatch(thread -> thread.startsWith("synthetic-agent-events-"));
            verify(fixture.mapper, times(1)).insertBatch(any());
            assertThat(fixture.releases).hasValue(1);
        }
    }

    @Test
    void should_flushPreviouslyAcceptedAuditOnly_when_leaseIsLost() {
        try (var fixture = new Fixture()) {
            var context = context(31L, 7L, "lost");
            when(fixture.model.stream(any(Prompt.class))).thenAnswer(call -> {
                context.setExecutionLost(true);
                context.setCancelled(true);
                return Flux.empty();
            });
            var events = fixture.run(context);
            assertThat(events).extracting(AgentEvent::getEventType)
                    .containsExactly(AgentEventType.AGENT_START, AgentEventType.MODEL_CALL_START, AgentEventType.ERROR);
            assertThat(fixture.stored).extracting(AgentEventEntity::getEventType)
                    .containsExactly("AGENT_START", "MODEL_CALL_START");
            assertThat(fixture.releases).hasValue(1);
            verifyNoInteractions(fixture.tools);
        }
    }

    @Test
    void should_notCarryBufferIntoOtherTenant_when_poolThreadIsReused() throws Exception {
        try (var fixture = new Fixture()) {
            when(fixture.model.stream(any(Prompt.class))).thenReturn(answer());
            fixture.run(context(31L, 7L, "first"));
            int firstCount = fixture.stored.size();
            fixture.run(context(41L, 9L, "second"));
            assertThat(fixture.stored.subList(0, firstCount))
                    .allMatch(row -> row.getTenantId().equals(7L) && row.getSessionId().equals("first"));
            assertThat(fixture.stored.subList(firstCount, fixture.stored.size()))
                    .allMatch(row -> row.getTenantId().equals(9L) && row.getSessionId().equals("second"));
            fixture.pool.submit(() -> {
                assertThat(LoginUserContextHolder.current()).isNull();
                fixture.persistence.flush();
            }).get(3, TimeUnit.SECONDS);
            verify(fixture.mapper, times(2)).insertBatch(any());
        }
    }

    @Test
    void should_finishAndReleaseWithoutReplayingModel_when_auditDatabaseFails() {
        try (var fixture = new Fixture()) {
            doThrow(new IllegalStateException("synthetic-sensitive-db-body"))
                    .when(fixture.mapper).insertBatch(any());
            when(fixture.model.stream(any(Prompt.class))).thenReturn(answer());
            assertThat(fixture.run(context(31L, 7L, "db-failed"))).last()
                    .satisfies(event -> assertThat(event.getEventType()).isEqualTo(AgentEventType.AGENT_END));
            assertThat(fixture.releases).hasValue(1);
            verify(fixture.mapper, times(1)).insertBatch(any());
            verify(fixture.model, times(1)).stream(any(Prompt.class));
            verifyNoInteractions(fixture.tools);
        }
    }

    @Test
    void should_persistFinalEventsDespiteHttpDisconnect_withoutStoppingWorker() throws Exception {
        try (var fixture = new Fixture()) {
            var started = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            when(fixture.model.stream(any(Prompt.class))).thenAnswer(call -> {
                started.countDown();
                if (!release.await(3, TimeUnit.SECONDS)) {
                    throw new AssertionError("synthetic model gate timed out");
                }
                return answer();
            });
            var context = context(31L, 7L, "disconnect");
            context.setChatModel(fixture.model);
            try (var ignored = LoginUserContextHolder.open(AgentEventPersistenceTest.user(31L, 7L))) {
                var reader = fixture.loop.run(context, fixture.releases::incrementAndGet).subscribe();
                try {
                    assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
                    reader.dispose();
                    assertThat(fixture.releases).hasValue(0);
                    assertThat(context.isCancelled()).isFalse();
                } finally {
                    release.countDown();
                }
            }
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                    .untilAsserted(() -> assertThat(fixture.releases).hasValue(1));
            assertThat(fixture.stored).last().satisfies(row -> assertThat(row.getEventType()).isEqualTo("AGENT_END"));
            verify(fixture.mapper, times(1)).insertBatch(any());
        }
    }

    private static Flux<ChatResponse> answer() {
        return Flux.just(new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                .content("合成短回答").build()))));
    }

    private static ReactContext context(Long userId, Long tenantId, String sessionId) {
        var context = new ReactContext();
        context.setUserId(userId);
        context.setTenantId(tenantId);
        context.setSessionId(sessionId);
        context.setAgentCode("synthetic-agent");
        context.setMaxIters(3);
        return context;
    }

    private static final class Fixture implements AutoCloseable {
        private final AgentEventMapper mapper = mock(AgentEventMapper.class);
        private final AtomicLong ids = new AtomicLong(9007199254740992L);
        private final AgentEventPersistence persistence = new AgentEventPersistence(
                mapper, entity -> ids.incrementAndGet());
        private final List<AgentEventEntity> stored = new CopyOnWriteArrayList<>();
        private final List<String> threads = new CopyOnWriteArrayList<>();
        private final ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        private final ChatModel model = mock(ChatModel.class);
        private final AgentToolExecutor tools = mock(AgentToolExecutor.class);
        private final AtomicInteger releases = new AtomicInteger();
        private final ReactLoop loop;

        private Fixture() {
            pool.setCorePoolSize(1);
            pool.setMaxPoolSize(1);
            pool.setQueueCapacity(2);
            pool.setThreadNamePrefix("synthetic-agent-events-");
            pool.initialize();
            when(mapper.insertBatch(any())).thenAnswer(call -> {
                List<AgentEventEntity> batch = call.getArgument(0);
                stored.addAll(batch);
                threads.add(Thread.currentThread().getName());
                return batch.size();
            });
            var publisher = new AgentEventPublisher(persistence, mock(AgentEventWebFluxStream.class), List.of());
            loop = new ReactLoop(publisher, mock(AiModelInvocationRecorder.class), tools,
                    new AgentLoopTaskExecutor(pool, new AgentExecutionTaskDecorator()));
        }

        private List<AgentEvent> run(ReactContext context) {
            context.setChatModel(model);
            int expected = releases.get() + 1;
            try (var ignored = LoginUserContextHolder.open(
                    AgentEventPersistenceTest.user(context.getUserId(), context.getTenantId()))) {
                var events = loop.run(context, releases::incrementAndGet).collectList().block(Duration.ofSeconds(3));
                org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(3))
                        .untilAsserted(() -> assertThat(releases).hasValue(expected));
                return events;
            }
        }

        @Override
        public void close() {
            pool.shutdown();
            assertThat(LoginUserContextHolder.current()).isNull();
        }
    }
}
