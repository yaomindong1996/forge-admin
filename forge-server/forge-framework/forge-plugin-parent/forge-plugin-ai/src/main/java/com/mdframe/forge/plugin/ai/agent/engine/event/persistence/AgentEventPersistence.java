package com.mdframe.forge.plugin.ai.agent.engine.event.persistence;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.event.mapper.AgentEventMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 在已传播可信身份的 loop 线程有序缓冲；终态及 loop finally 必须刷新，禁止再异步分派。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventPersistence {
    private final AgentEventMapper agentEventMapper;
    private final IdentifierGenerator idGenerator;

    private static final int BATCH_SIZE = 20;
    private final ThreadLocal<Batch> buffer = new ThreadLocal<>();

    public void persist(AgentEvent event) {
        LoginUser caller = AgentCallerGuard.requireCaller();
        if (event == null || event.getEventType() == null || !StringUtils.hasText(event.getSessionId())
                || event.getSessionId().length() > 64) {
            throw new BusinessException("Agent 事件标识无效");
        }
        Batch batch = buffer.get();
        if (batch != null) {
            requireOwner(batch, caller);
            if (!Objects.equals(batch.sessionId(), event.getSessionId())) {
                flush();
                batch = null;
            }
        }
        if (batch == null) {
            batch = new Batch(event.getSessionId(), caller.getTenantId(), caller.getUserId(), new ArrayList<>());
            buffer.set(batch);
        }
        batch.events().add(toEntity(event, caller));
        if (batch.events().size() >= BATCH_SIZE || event.getEventType() == AgentEventType.AGENT_END
                || event.getEventType() == AgentEventType.REQUIRE_USER_CONFIRM) {
            flush();
        }
    }

    /** 不调用模型/工具；失去 lease 也只保存此前已接受的审计事件，不改其它执行的消息状态。 */
    public void flush() {
        Batch batch = buffer.get();
        buffer.remove();
        if (batch == null || batch.events().isEmpty()) {
            return;
        }
        requireOwner(batch, AgentCallerGuard.requireCaller());
        writeBatch(batch.events());
    }

    private void writeBatch(List<AgentEventEntity> events) {
        try {
            if (agentEventMapper.insertBatch(events) != events.size()) {
                log.warn("[AgentEvent] 批量落库数量不符, batchSize={}", events.size());
            }
        } catch (RuntimeException ex) {
            // SQL 异常可能带事件正文，审计失败不重试业务，缓冲已移除避免污染下一请求。
            log.error("[AgentEvent] 批量持久化失败, batchSize={}, exceptionType={}",
                    events.size(), ex.getClass().getSimpleName());
        }
    }

    private void requireOwner(Batch batch, LoginUser caller) {
        if (!Objects.equals(batch.tenantId(), caller.getTenantId())
                || !Objects.equals(batch.userId(), caller.getUserId())) {
            throw new BusinessException(403, "Agent 事件执行身份发生变化");
        }
    }

    private AgentEventEntity toEntity(AgentEvent event, LoginUser caller) {
        AgentEventEntity entity = new AgentEventEntity();
        entity.setId(idGenerator.nextId(entity).longValue());
        entity.setTenantId(caller.getTenantId());
        entity.setSessionId(event.getSessionId());
        entity.setTurnIndex(event.getTurnIndex());
        entity.setEventType(event.getEventType().getCode());
        entity.setEventData(event.getData());
        entity.setParentId(event.getParentId());
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private record Batch(String sessionId, Long tenantId, Long userId, List<AgentEventEntity> events) {
    }
}
