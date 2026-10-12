package com.mdframe.forge.plugin.ai.agent.engine.lease;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** 本地 Map 只维护工作线程与心跳生命周期；执行许可必须来自 Redis，绝不回退到单机锁。 */
@Slf4j
@Component
public class AgentSessionLeaseRegistry {
    private final AgentSessionLeaseStore store;
    private final AgentLeaseHeartbeatScheduler scheduler;
    private final AgentSessionLeaseProperties properties;
    private final ConcurrentMap<String, Lease> active = new ConcurrentHashMap<>();

    public AgentSessionLeaseRegistry(AgentSessionLeaseStore store,
            @Qualifier("forgeAgentLeaseScheduler") AgentLeaseHeartbeatScheduler scheduler,
            AgentSessionLeaseProperties properties) {
        properties.validate();
        this.store = store;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    public Lease acquire(ReactContext context) {
        AgentCallerGuard.requireOwner(context);
        var owner = new AgentSessionLeaseStore.Owner(AgentSessionLeaseStore.key(context.getSessionId()),
                UUID.randomUUID().toString(), context.getUserId(), context.getTenantId());
        var lease = new Lease(context, owner);
        if (!store.acquire(owner, properties.getTtl().toMillis())) {
            throw new BusinessException(409, "该会话正在处理中，请稍后重试");
        }
        if (active.putIfAbsent(context.getSessionId(), lease) != null) {
            lease.close();
            throw new BusinessException(409, "该会话正在处理中，请稍后重试");
        }
        try {
            lease.heartbeat = scheduler.schedule(lease::heartbeat, properties.getHeartbeatInterval());
            if (lease.heartbeat == null) {
                throw new IllegalStateException("Agent 心跳未调度");
            }
            context.attachExecutionGuard(lease::verify);
            lease.verify();
            return lease;
        } catch (RuntimeException ex) {
            lease.close();
            throw ex;
        }
    }

    public void cancel(String sessionId) {
        var caller = AgentCallerGuard.requireCaller();
        store.cancel(sessionId, caller.getUserId(), caller.getTenantId());
        Lease lease = active.get(sessionId);
        if (lease != null) {
            AgentCallerGuard.requireOwner(lease.context);
            lease.context.setCancelled(true);
        }
    }

    @PreDestroy
    public void shutdown() {
        // 关闭时先停止后续动作，不能提前释放仍在执行外部 I/O 的占用；崩溃由 TTL 回收。
        active.values().forEach(Lease::invalidate);
    }

    public final class Lease implements AutoCloseable {
        private final ReactContext context;
        private final AgentSessionLeaseStore.Owner owner;
        private final AtomicBoolean closed = new AtomicBoolean();
        private volatile ScheduledFuture<?> heartbeat;
        private volatile long validUntil = System.nanoTime() + properties.getTtl().toNanos();

        private Lease(ReactContext context, AgentSessionLeaseStore.Owner owner) {
            this.context = context;
            this.owner = owner;
        }

        public void verify() {
            if (closed.get() || context.isExecutionLost() || System.nanoTime() >= validUntil) {
                invalidate();
                throw new BusinessException(409, "Agent 会话执行许可已失效，请重新发起");
            }
            long started = System.nanoTime();
            try {
                long result = store.renew(owner, properties.getTtl().toMillis());
                if ((result != 1 && result != 2) || System.nanoTime() >= started + properties.getTtl().toNanos()) {
                    invalidate();
                    throw new BusinessException(409, "Agent 会话执行许可已失效，请重新发起");
                }
                validUntil = started + properties.getTtl().toNanos();
                if (result == 2) {
                    context.setCancelled(true);
                }
            } catch (RuntimeException ex) {
                invalidate();
                throw ex;
            }
        }

        private void heartbeat() {
            if (closed.get() || context.isExecutionLost()) {
                return;
            }
            try {
                verify();
            } catch (RuntimeException ex) {
                log.warn("Agent 租约续租失败，停止后续执行: exceptionType={}", ex.getClass().getSimpleName());
            }
        }

        private void invalidate() {
            context.setExecutionLost(true);
            context.setCancelled(true);
            ScheduledFuture<?> task = heartbeat;
            if (task != null) {
                task.cancel(false);
            }
        }

        @Override
        public void close() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            ScheduledFuture<?> task = heartbeat;
            if (task != null) {
                task.cancel(false);
            }
            active.remove(context.getSessionId(), this);
            try {
                store.release(owner);
            } catch (RuntimeException ex) {
                // 条件释放失败由 TTL 回收，不能影响 finally 中 SSE/埋点的清理，更不能盲删新 owner。
                log.warn("Agent 租约释放未完成，等待 TTL 回收: exceptionType={}", ex.getClass().getSimpleName());
            }
        }
    }
}
