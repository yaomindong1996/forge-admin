package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.service.AgentRuntimeFactory;
import com.mdframe.forge.plugin.ai.agent.engine.lease.AgentSessionLeaseRegistry;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * ReAct Agent 入口。
 * 提供 execute（新对话）和 resume（HITL恢复）两个方法。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactAgent {

    private final ReactLoop reactLoop;
    private final InterruptStore interruptStore;
    private final AgentRuntimeFactory runtimeFactory;
    private final AgentEventPublisher eventPublisher;
    private final AgentSessionLeaseRegistry leases;

    /**
     * 执行新对话
     */
    public Flux<AgentEvent> execute(ReactContext ctx) {
        return execute(ctx, context -> { });
    }

    public Flux<AgentEvent> execute(ReactContext ctx, Consumer<ReactContext> beforeExecute) {
        AgentCallerGuard.requireOwner(ctx);
        var lease = leases.acquire(ctx);
        try {
            // 先占用会话再落库，竞争失败的新请求不能覆盖正在运行的持久化状态。
            beforeExecute.accept(ctx);
            ctx.verifyExecution();
            return runReserved(ctx, lease);
        } catch (RuntimeException | Error ex) {
            lease.close();
            throw ex;
        }
    }

    private Flux<AgentEvent> runReserved(ReactContext ctx, AgentSessionLeaseRegistry.Lease lease) {
        try {
            // 客户端断连不等于后台任务结束；在真实循环终止时清理，才能继续停止/确认。
            return reactLoop.run(ctx, lease::close);
        } catch (RuntimeException | Error ex) {
            lease.close();
            throw ex;
        }
    }

    /**
     * 停止指定会话
     */
    public void cancel(String sessionId) {
        leases.cancel(sessionId);
    }

    /**
     * 恢复中断的对话（HITL）
     *
     * @param interruptId 中断ID
     * @param confirmed   用户是否确认
     */
    public Flux<AgentEvent> resume(String interruptId, boolean confirmed) {
        return resume(interruptId, confirmed, context -> { });
    }

    public Flux<AgentEvent> resume(String interruptId, boolean confirmed, Consumer<ReactContext> beforeResume) {
        InterruptStore.InterruptState state = interruptStore.get(interruptId);
        if (state == null) {
            return Flux.just(AgentEvent.of("", 0, AgentEventType.AGENT_END,
                    "{\"error\":\"中断已过期或不存在\"}"));
        }

        ReactContext ctx = state.getContext();
        AgentCallerGuard.requireOwner(ctx);
        // 会话占用也要原子化，避免确认已消费之后才发现同一会话被新请求抢先启动。
        var lease = leases.acquire(ctx);
        try {
            if (confirmed) {
                // 在消费前重新读取当前配置与授权；模型/工具对象不从 Redis 反序列化。
                runtimeFactory.restore(ctx);
            }
            ctx.verifyExecution();
            if (!interruptStore.consume(interruptId, state)) {
                throw new BusinessException(409, "确认已处理或中断已过期");
            }
            beforeResume.accept(ctx);
            if (!confirmed) {
                return reject(ctx, lease);
            }
            ctx.setConfirmedToolCallId(ctx.pendingToolCall().id());
            return runReserved(ctx, lease);
        } catch (RuntimeException | Error ex) {
            lease.close();
            throw ex;
        }
    }

    private Flux<AgentEvent> reject(ReactContext ctx, AgentSessionLeaseRegistry.Lease lease) {
        // 保留拒绝即终止语义，绝不执行其它尚未开始的工具。
        AgentEvent result = AgentEvent.of(ctx.getSessionId(), ctx.getTurnIndex(),
                AgentEventType.USER_CONFIRM_RESULT, "{\"confirmed\":false}");
        AgentEvent end = AgentEvent.of(ctx.getSessionId(), ctx.getTurnIndex(),
                AgentEventType.AGENT_END, "{\"aborted\":true}");
        try {
            ctx.verifyExecution();
            eventPublisher.publish(result);
            ctx.verifyExecution();
            eventPublisher.publish(end);
            return Flux.just(result, end);
        } finally {
            lease.close();
        }
    }
}
