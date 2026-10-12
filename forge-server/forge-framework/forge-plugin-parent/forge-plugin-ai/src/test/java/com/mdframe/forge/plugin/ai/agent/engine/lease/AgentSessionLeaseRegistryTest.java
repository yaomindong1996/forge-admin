package com.mdframe.forge.plugin.ai.agent.engine.lease;

import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactLoop;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentExecutionTaskDecorator;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentLoopTaskExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolExecutor;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.service.AgentRuntimeFactory;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class AgentSessionLeaseRegistryTest {
    private final AgentLeaseFixture fixture = new AgentLeaseFixture();

    @Test
    void should_allowOnlyOneExecution_when_twoNodesRaceWith32Requests() throws Exception {
        var firstLoop = mock(ReactLoop.class);
        var secondLoop = mock(ReactLoop.class);
        AtomicReference<Runnable> terminated = new AtomicReference<>();
        for (var loop : new ReactLoop[] {firstLoop, secondLoop}) {
            when(loop.run(any(), any())).thenAnswer(call -> {
                terminated.set(call.getArgument(1));
                return Flux.never();
            });
        }
        var first = agent(firstLoop, fixture.registry());
        var second = agent(secondLoop, fixture.registry());
        var requests = new ThreadPoolTaskExecutor();
        requests.setCorePoolSize(8);
        requests.setMaxPoolSize(8);
        requests.setQueueCapacity(32);
        requests.initialize();
        var gate = new CountDownLatch(1);
        AtomicInteger effects = new AtomicInteger();
        try {
            var results = new ArrayList<Future<Boolean>>();
            for (int index = 0; index < 32; index++) {
                ReactAgent node = index % 2 == 0 ? first : second;
                results.add(requests.submit(() -> compete(node, gate, effects)));
            }
            gate.countDown();
            int accepted = 0;
            for (var result : results) {
                accepted += result.get(5, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertThat(accepted).isEqualTo(1);
            assertThat(effects).hasValue(1);
            terminated.get().run();
            assertThat(fixture.occupied("session")).isFalse();
        } finally {
            gate.countDown();
            requests.shutdown();
        }
    }

    @Test
    void should_stopBeforeModelOrTool_and_notPublishStaleEnd_when_newNodeOwnsTheLease() {
        var first = fixture.registry();
        var second = fixture.registry();
        var publisher = mock(AgentEventPublisher.class);
        var model = mock(ChatModel.class);
        var tools = mock(AgentToolExecutor.class);
        var replacement = new AtomicReference<AgentSessionLeaseRegistry.Lease>();
        doAnswer(call -> {
            var event = (com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent) call.getArgument(0);
            if (event.getEventType() == AgentEventType.AGENT_START) {
                fixture.advance(60001);
                replacement.set(second.acquire(context()));
            }
            return null;
        }).when(publisher).publish(any());
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.setQueueCapacity(1);
        pool.initialize();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var loop = new ReactLoop(publisher, mock(AiModelInvocationRecorder.class), tools,
                    new AgentLoopTaskExecutor(pool, new AgentExecutionTaskDecorator()));
            var context = context();
            context.setMaxIters(3);
            context.setAgentId(21L);
            context.setChatModel(model);
            var result = agent(loop, first).execute(context).collectList().block(Duration.ofSeconds(3));
            assertThat(result).extracting(event -> event.getEventType())
                    .containsExactly(AgentEventType.AGENT_START, AgentEventType.ERROR);
            assertThat(result.get(1).getData()).contains("占用已失效");
            verify(publisher, never()).publish(org.mockito.ArgumentMatchers.argThat(event ->
                    event.getEventType() == AgentEventType.ERROR || event.getEventType() == AgentEventType.AGENT_END));
            assertThat(context.isExecutionLost()).isTrue();
            verify(model, never()).stream(any(org.springframework.ai.chat.prompt.Prompt.class));
            verifyNoInteractions(tools);
            assertThat(fixture.occupied("session")).isTrue();
            replacement.get().close();
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void should_allowOnlyOneNodeBeforeSideEffects_and_keepLeaseAfterDisconnect() {
        var firstLoop = mock(ReactLoop.class);
        var secondLoop = mock(ReactLoop.class);
        AtomicReference<Runnable> terminated = new AtomicReference<>();
        when(firstLoop.run(any(), any())).thenAnswer(call -> {
            terminated.set(call.getArgument(1));
            return Flux.never();
        });
        var first = agent(firstLoop, fixture.registry());
        var second = agent(secondLoop, fixture.registry());
        AtomicInteger writes = new AtomicInteger();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var reader = first.execute(context(), ctx -> writes.incrementAndGet()).subscribe();
            reader.dispose();
            assertThatThrownBy(() -> second.execute(context(), ctx -> writes.incrementAndGet()))
                    .hasMessageContaining("正在处理中");
            assertThat(writes).hasValue(1);
            assertThat(fixture.occupied("session")).isTrue();
            verifyNoInteractions(secondLoop);
            terminated.get().run();
            assertThat(fixture.occupied("session")).isFalse();
            when(secondLoop.run(any(), any())).thenReturn(Flux.never());
            second.execute(context(), ctx -> writes.incrementAndGet());
            assertThat(writes).hasValue(2);
        }
    }

    @Test
    void should_renewLiveLease_and_stopOldOwnerWithoutDeletingReplacement_when_ttlExpires() {
        var first = fixture.registry();
        var second = fixture.registry();
        var oldContext = context();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var oldLease = first.acquire(oldContext);
            fixture.advance(50000);
            fixture.heartbeat();
            fixture.advance(50000);
            assertThatThrownBy(() -> second.acquire(context())).hasMessageContaining("正在处理中");
            fixture.advance(60001);
            var replacement = second.acquire(context());
            assertThatThrownBy(oldContext::verifyExecution).hasMessageContaining("失效");
            assertThat(oldContext.isExecutionLost()).isTrue();
            assertThat(oldContext.isCancelled()).isTrue();
            oldLease.close();
            oldLease.close();
            assertThat(fixture.occupied("session")).isTrue();
            replacement.verify();
            replacement.close();
            assertThat(fixture.occupied("session")).isFalse();
        }
    }

    @Test
    void should_cancelOwningNodeFromAnotherNode_but_rejectForeignCaller() {
        var first = fixture.registry();
        var second = fixture.registry();
        var context = context();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var lease = first.acquire(context);
            try (var foreign = LoginUserContextHolder.open(user(99L, 7L))) {
                assertThatThrownBy(() -> second.cancel("session")).hasMessageContaining("无权");
            }
            try (var foreign = LoginUserContextHolder.open(user(31L, 8L))) {
                assertThatThrownBy(() -> second.cancel("session")).hasMessageContaining("无权");
            }
            assertThat(context.isCancelled()).isFalse();
            second.cancel("session");
            fixture.heartbeat();
            assertThat(context.isCancelled()).isTrue();
            assertThat(context.isExecutionLost()).isFalse();
            assertThat(fixture.occupied("session")).isTrue();
            lease.close();
        }
    }

    @Test
    void should_failClosedAndCancelStream_when_redisIsUnavailable() {
        var registry = fixture.registry();
        var context = context();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var lease = registry.acquire(context);
            fixture.available = false;
            fixture.heartbeat();
            assertThat(context.isExecutionLost()).isTrue();
            assertThat(context.cancellationSignal().blockOptional()).isEmpty();
            assertThatThrownBy(context::verifyExecution).hasMessageContaining("失效");
            lease.close();
            var loop = mock(ReactLoop.class);
            var agent = agent(loop, fixture.registry());
            assertThatThrownBy(() -> agent.execute(context(), ctx -> {
                throw new AssertionError("Redis 故障不允许落库");
            })).isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
            verifyNoInteractions(loop);
        }
    }

    @Test
    void should_validateIdentityAndId_and_keepScopeFreeSchedulerConfiguration() {
        var registry = fixture.registry();
        assertThatThrownBy(() -> registry.acquire(context())).hasMessageContaining("登录");
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var context = context();
            context.setUserId(99L);
            assertThatThrownBy(() -> registry.acquire(context)).hasMessageContaining("无权");
            assertThatThrownBy(() -> registry.cancel(" ")).hasMessageContaining("标识");
        }
        var properties = new AgentSessionLeaseProperties();
        properties.setHeartbeatInterval(java.time.Duration.ofSeconds(30));
        assertThatThrownBy(() -> new AgentSessionLeaseConfiguration().forgeAgentLeaseScheduler(properties))
                .hasMessageContaining("租约");
    }

    @Test
    void should_keepLeaseUntilActualTermination_when_preparationOrLoopSubmissionFails() {
        var loop = mock(ReactLoop.class);
        var agent = agent(loop, fixture.registry());
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            assertThatThrownBy(() -> agent.execute(context(), ctx -> {
                throw new IllegalStateException("fixture prepare failure");
            })).hasMessage("fixture prepare failure");
            assertThat(fixture.occupied("session")).isFalse();
            assertThatThrownBy(() -> agent.execute(context(), ctx -> {
                throw new AssertionError("fixture fatal preparation error");
            })).isInstanceOf(AssertionError.class);
            assertThat(fixture.occupied("session")).isFalse();
            when(loop.run(any(), any())).thenThrow(new IllegalStateException("fixture submit failure"));
            assertThatThrownBy(() -> agent.execute(context())).hasMessage("fixture submit failure");
            assertThat(fixture.occupied("session")).isFalse();
        }
    }

    private ReactAgent agent(ReactLoop loop, AgentSessionLeaseRegistry registry) {
        return new ReactAgent(loop, mock(InterruptStore.class), mock(AgentRuntimeFactory.class),
                mock(AgentEventPublisher.class), registry);
    }

    private boolean compete(ReactAgent node, CountDownLatch gate, AtomicInteger effects) throws Exception {
        gate.await(3, TimeUnit.SECONDS);
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            node.execute(context(), ctx -> effects.incrementAndGet());
            return true;
        } catch (com.mdframe.forge.starter.core.exception.BusinessException ex) {
            assertThat(ex.getCode()).isEqualTo(409);
            return false;
        }
    }

    private ReactContext context() {
        var context = new ReactContext();
        context.setSessionId("session");
        context.setAgentCode("fixture");
        context.setUserId(31L);
        context.setTenantId(7L);
        return context;
    }

    private LoginUser user(Long id, Long tenant) {
        var user = new LoginUser();
        user.setUserId(id);
        user.setTenantId(tenant);
        return user;
    }
}
