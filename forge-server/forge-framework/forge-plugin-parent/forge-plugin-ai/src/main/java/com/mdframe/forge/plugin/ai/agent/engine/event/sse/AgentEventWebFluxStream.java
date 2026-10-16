package com.mdframe.forge.plugin.ai.agent.engine.event.sse;

import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentSessionAccess;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentSessionAccess.SessionKey;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 每个可信会话共用有界 sink，支持多订阅；旧连接断开不能删掉新连接。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentEventWebFluxStream {
    private static final int BUFFER_SIZE = 256;
    private final AgentSessionAccess access;
    private final ConcurrentHashMap<SessionKey, Subscription> sessions = new ConcurrentHashMap<>();

    public Flux<ServerSentEvent<String>> subscribe(String sessionId) {
        SessionKey key = access.requireOwned(sessionId);
        // 身份校验在请求线程完成，不能依赖 SSE 订阅线程仍带 Servlet ThreadLocal。
        return Flux.defer(() -> {
            Subscription subscription = sessions.compute(key, (ignored, previous) -> {
                Subscription current = previous == null ? new Subscription() : previous;
                current.readers.incrementAndGet();
                return current;
            });
            return subscription.sink.asFlux().map(this::toSse)
                    .doFinally(signal -> release(key, subscription));
        });
    }

    public void emit(AgentEvent event) {
        SessionKey key;
        try {
            var caller = AgentCallerGuard.requireCaller();
            key = new SessionKey(event.getSessionId(), caller.getTenantId(), caller.getUserId());
        } catch (RuntimeException ex) {
            log.debug("[AgentSse] 缺少可信执行身份，跳过转发: exceptionType={}", ex.getClass().getSimpleName());
            return;
        }
        Subscription subscription = sessions.get(key);
        if (subscription == null) {
            return;
        }
        // 只序列化同一个 sink 的发射；不能在持有 Map.compute 的锁时等它，避免取消回调反向加锁。
        synchronized (subscription) {
            Sinks.EmitResult result = subscription.sink.tryEmitNext(event);
            if (result.isFailure()) {
                sessions.remove(key, subscription);
                subscription.sink.tryEmitError(new BusinessException(503, "Agent 事件接收中断，请刷新会话查看结果"));
                return;
            }
            if (event.getEventType() == AgentEventType.AGENT_END
                    || event.getEventType() == AgentEventType.REQUIRE_USER_CONFIRM) {
                sessions.remove(key, subscription);
                subscription.sink.tryEmitComplete();
            }
        }
    }

    private void release(SessionKey key, Subscription subscription) {
        AtomicBoolean close = new AtomicBoolean();
        sessions.computeIfPresent(key, (ignored, current) -> {
            if (current != subscription || current.readers.decrementAndGet() > 0) {
                return current;
            }
            close.set(true);
            return null;
        });
        if (close.get()) {
            synchronized (subscription) {
                subscription.sink.tryEmitComplete();
            }
        }
    }

    private ServerSentEvent<String> toSse(AgentEvent event) {
        return ServerSentEvent.<String>builder().id(String.valueOf(event.getTimestamp()))
                .event(event.getEventType().getCode()).data(event.getData() == null ? "" : event.getData()).build();
    }

    private static final class Subscription {
        private final AtomicInteger readers = new AtomicInteger();
        private final Sinks.Many<AgentEvent> sink = Sinks.many().multicast()
                .onBackpressureBuffer(BUFFER_SIZE, false);
    }
}
