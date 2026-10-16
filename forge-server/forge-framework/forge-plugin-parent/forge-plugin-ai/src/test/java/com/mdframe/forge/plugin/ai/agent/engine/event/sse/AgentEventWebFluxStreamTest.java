package com.mdframe.forge.plugin.ai.agent.engine.event.sse;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentSessionAccess;
import com.mdframe.forge.plugin.ai.agent.engine.controller.AgentEngineController;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.BaseSubscriber;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentEventWebFluxStreamTest {
    private final AiChatSessionService sessions = mock(AiChatSessionService.class);
    private final AgentEventWebFluxStream stream = new AgentEventWebFluxStream(new AgentSessionAccess(sessions));

    @Test
    void subscriptionEndpointReusesStreamPermission() throws Exception {
        var method = AgentEngineController.class.getMethod("subscribeEvents", String.class,
                HttpServletResponse.class);
        assertThat(method.getAnnotation(SaCheckPermission.class).value()).containsExactly("ai:engine:stream");
    }

    @Test
    void foreignAndAnonymousSubscriptionsFailBeforeOpeningStream() {
        assertThatThrownBy(() -> stream.subscribe("fixture")).hasMessageContaining("登录");
        configureSession(32L, 7L);
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            assertThatThrownBy(() -> stream.subscribe("fixture")).hasMessageContaining("无权");
        }
    }

    @Test
    void disconnectingOneReaderDoesNotLoseOtherReaderEvents() {
        configureSession(31L, 7L);
        var first = new ArrayList<String>();
        var second = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var a = stream.subscribe("fixture").subscribe(event -> first.add(event.data()));
            var b = stream.subscribe("fixture").subscribe(event -> second.add(event.data()));
            stream.emit(event("first", AgentEventType.TEXT_BLOCK_DELTA));
            a.dispose();
            stream.emit(event("second", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(first).containsExactly("first");
            assertThat(second).containsExactly("first", "second");
            b.dispose();
        }
    }

    @Test
    void publisherIdentityCannotCrossUsersOrTenants() {
        configureSession(31L, 7L);
        var received = new ArrayList<String>();
        reactor.core.Disposable subscriber;
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            subscriber = stream.subscribe("fixture").subscribe(event -> received.add(event.data()));
        }
        stream.emit(event("anonymous", AgentEventType.TEXT_BLOCK_DELTA));
        try (var ignored = LoginUserContextHolder.open(user(32L, 7L))) {
            stream.emit(event("foreign-user", AgentEventType.TEXT_BLOCK_DELTA));
        }
        try (var ignored = LoginUserContextHolder.open(user(31L, 9L))) {
            stream.emit(event("foreign-tenant", AgentEventType.TEXT_BLOCK_DELTA));
        }
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            stream.emit(event("owned", AgentEventType.TEXT_BLOCK_DELTA));
        }
        assertThat(received).containsExactly("owned");
        subscriber.dispose();
    }

    @Test
    void terminalEventCompletesReadersAndNewSubscriptionDoesNotReplayOldEvents() {
        configureSession(31L, 7L);
        var completed = new AtomicBoolean();
        var fresh = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            stream.subscribe("fixture").subscribe(event -> { }, error -> { }, () -> completed.set(true));
            stream.emit(event("end", AgentEventType.AGENT_END));
            assertThat(completed).isTrue();
            var next = stream.subscribe("fixture").subscribe(event -> fresh.add(event.data()));
            stream.emit(event("new", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(fresh).containsExactly("new");
            next.dispose();
        }
    }

    @Test
    void oldBackpressuredConnectionCannotRemoveNewRegistration() {
        configureSession(31L, 7L);
        var old = new BaseSubscriber<ServerSentEvent<String>>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) { /* 故意保持零需求直到旧连接取消。 */ }
        };
        var received = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            stream.subscribe("fixture").subscribe(old);
            stream.emit(event("end", AgentEventType.AGENT_END));
            var fresh = stream.subscribe("fixture").subscribe(event -> received.add(event.data()));
            old.cancel();
            stream.emit(event("new", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(received).containsExactly("new");
            fresh.dispose();
        }
    }

    @Test
    void lastDisconnectReleasesOldBuffer() {
        configureSession(31L, 7L);
        var received = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var old = stream.subscribe("fixture").subscribe();
            old.dispose();
            stream.emit(event("no-reader", AgentEventType.TEXT_BLOCK_DELTA));
            var fresh = stream.subscribe("fixture").subscribe(event -> received.add(event.data()));
            stream.emit(event("new", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(received).containsExactly("new");
            fresh.dispose();
        }
    }

    @Test
    void confirmationTerminalCompletesOldRoundAndNewSubscriptionReceivesResume() {
        configureSession(31L, 7L);
        var completed = new AtomicBoolean();
        var oldEvents = new ArrayList<String>();
        var newEvents = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var old = stream.subscribe("fixture").subscribe(event -> oldEvents.add(event.data()),
                    error -> { throw new AssertionError(error); }, () -> completed.set(true));
            stream.emit(event("confirm", AgentEventType.REQUIRE_USER_CONFIRM));
            assertThat(completed).isTrue();
            var fresh = stream.subscribe("fixture").subscribe(event -> newEvents.add(event.data()));
            old.dispose();
            stream.emit(event("resumed", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(oldEvents).containsExactly("confirm");
            assertThat(newEvents).containsExactly("resumed");
            fresh.dispose();
        }
    }

    @Test
    void overflowFailsExplicitlyAndOldConnectionCannotRemoveReplacement() {
        configureSession(31L, 7L);
        var failure = new AtomicReference<Throwable>();
        var old = new BaseSubscriber<ServerSentEvent<String>>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) { /* 故意模拟无消费能力的慢连接。 */ }

            @Override
            protected void hookOnError(Throwable error) { failure.set(error); }
        };
        var freshEvents = new ArrayList<String>();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            stream.subscribe("fixture").subscribe(old);
            for (int index = 0; index < 257; index++) {
                stream.emit(event("slow-" + index, AgentEventType.TEXT_BLOCK_DELTA));
            }
            var fresh = stream.subscribe("fixture").subscribe(event -> freshEvents.add(event.data()));
            old.requestUnbounded();
            assertThat(failure.get()).isNotNull().hasMessageContaining("事件接收中断");
            stream.emit(event("fresh", AgentEventType.TEXT_BLOCK_DELTA));
            assertThat(freshEvents).containsExactly("fresh");
            fresh.dispose();
        }
    }

    @Test
    void concurrentPublishersDoNotSilentlyDropNonSerializedEmissions() throws Exception {
        configureSession(31L, 7L);
        var received = new CopyOnWriteArrayList<String>();
        var failures = new CopyOnWriteArrayList<Throwable>();
        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(2);
        pool.setMaxPoolSize(2);
        pool.setQueueCapacity(2);
        pool.initialize();
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            var reader = stream.subscribe("fixture").subscribe(event -> received.add(event.data()), failures::add);
            try {
                var first = pool.submit(() -> publishRange("first"));
                var second = pool.submit(() -> publishRange("second"));
                first.get(3, TimeUnit.SECONDS);
                second.get(3, TimeUnit.SECONDS);
                assertThat(received).hasSize(200).doesNotHaveDuplicates();
                assertThat(failures).isEmpty();
            } finally {
                reader.dispose();
            }
        } finally {
            pool.shutdown();
        }
    }

    private void publishRange(String prefix) {
        try (var ignored = LoginUserContextHolder.open(user(31L, 7L))) {
            for (int index = 0; index < 100; index++) {
                stream.emit(event(prefix + index, AgentEventType.TEXT_BLOCK_DELTA));
            }
        }
    }

    private void configureSession(Long userId, Long tenantId) {
        when(sessions.getById("fixture")).thenReturn(AiChatSession.builder()
                .id("fixture").userId(userId).tenantId(tenantId).status("0").build());
    }

    private static AgentEvent event(String data, AgentEventType type) {
        return AgentEvent.of("fixture", 0, type, data);
    }

    private static LoginUser user(Long userId, Long tenantId) {
        var user = new LoginUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        return user;
    }
}
