package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.agent.engine.ReactAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.persistence.AgentChatPersister;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 引擎服务。
 * 新入口，不替代 AiClient。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEngineService {

    private final ReactAgent reactAgent;
    private final AgentConversationFactory conversations;
    private final AgentChatPersister chatPersister;

    /**
     * 流式执行 Agent 对话
     */
    public Flux<ServerSentEvent<String>> stream(ReactRequest request) {
        // 这里只建可信身份壳；占用会话后才能创建会话、读取历史或编辑重发。
        ReactContext ctx = conversations.seed(request);

        // 3. 会话原子占用后、loop 启动前在请求线程建行，重复请求不得覆盖已有流状态。
        AtomicReference<AgentChatPersister.OpenResult> open = new AtomicReference<>();
        Flux<AgentEvent> events = reactAgent.execute(ctx, context -> {
            conversations.populate(request, context);
            context.verifyExecution();
            if (context.isCancelled()) {
                throw new BusinessException("Agent 会话已停止");
            }
            open.set(chatPersister.openForStream(request, context.getAgentCode()));
        });

        // 4. 流首 PERSIST_META 把落库 recordId 下发前端。
        AgentEvent meta = buildPersistMeta(request.getSessionId(), open.get());
        if (meta != null) {
            events = Flux.concat(Flux.just(meta), events);
        }
        return events
                .map(event -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(event.getTimestamp()))
                        .event(event.getEventType().getCode())
                        .data(event.getData() != null ? event.getData() : "")
                        .build());
    }

    /**
     * 构造流首 PERSIST_META 事件：携带本轮落库的 user/assistant recordId。
     * 在 service 层 concat 进 SSE 流，不经过 eventPublisher，故持久化监听器不会收到。
     * 结果为空或无 assistant 行（sessionId 缺失/建行失败）时返回 {@code null}，不下发。
     */
    private AgentEvent buildPersistMeta(String sessionId, AgentChatPersister.OpenResult open) {
        if (open == null || open.assistantRecordId() == null) {
            return null;
        }
        JSONObject data = new JSONObject();
        // SSE 内嵌 JSON 不经过业务 Jackson Long 序列化，必须显式字符串化避免 JS 丢精度。
        data.put("assistantRecordId", String.valueOf(open.assistantRecordId()));
        if (open.userRecordId() != null) {
            data.put("userRecordId", String.valueOf(open.userRecordId()));
        }
        return AgentEvent.of(sessionId, 0, AgentEventType.PERSIST_META, data.toJSONString());
    }

    /**
     * HITL 恢复
     */
    public Flux<ServerSentEvent<String>> resume(String interruptId, boolean confirmed) {
        // 先验证身份并原子消费确认，再接回落库状态，不能让越权请求先修改消息。
        return reactAgent.resume(interruptId, confirmed,
                        context -> chatPersister.openForResume(interruptId, confirmed))
                .map(event -> ServerSentEvent.<String>builder()
                        .id(String.valueOf(event.getTimestamp()))
                        .event(event.getEventType().getCode())
                        .data(event.getData() != null ? event.getData() : "")
                        .build());
    }

    public void stop(String sessionId) {
        reactAgent.cancel(sessionId);
    }

}
