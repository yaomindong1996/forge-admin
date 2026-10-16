package com.mdframe.forge.plugin.ai.agent.engine.persistence;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventListener;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatMessageToolCall;
import com.mdframe.forge.plugin.ai.chat.enums.AiChatMessageStatus;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatMessageToolCallService;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 对话持久化编排层。
 *
 * <p>作为 {@link AgentEventListener} 挂在 {@code AgentEventPublisher} 上，在 loop 线程随事件
 * 顺序落库 ai_chat_record 与 ai_chat_message_tool_call。相比 tap SSE 响应流，这里与 HTTP
 * 客户端连接生命周期无关——客户端断连后 loop 仍在跑、事件照发，落库照常收口，从而支持
 * HITL 重连与历史回放（P0#2/#4）。</p>
 *
 * <p>身份（userId/tenantId）与建行在<b>请求线程</b>完成（{@link #openForStream}/{@link #openForResume}），
 * 因为 loop 线程无 Sa-Token 上下文。请求线程按 sessionId 把 {@link StreamState} 注册进 map，
 * loop 线程的事件回调据此查找；终止事件时移除，避免泄漏。</p>
 *
 * <p>依赖同域叶子数据服务（record/toolCall/session），不构成 Service 互相注入的业务编排链。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentChatPersister implements AgentEventListener {

    private final AiChatRecordService recordService;
    private final AiChatMessageToolCallService toolCallService;
    private final AiChatSessionService sessionService;

    /** 活跃流状态，key=sessionId。请求线程 put、loop 线程读/终止移除。 */
    private final ConcurrentHashMap<String, StreamState> stateBySession = new ConcurrentHashMap<>();

    // ==================== 请求线程：建行 / 接回 ====================

    /**
     * 新对话：请求线程调用。落用户消息行与 streaming 的 assistant 行，并注册流状态。
     * 必须在 loop 启动（reactAgent.execute）之前调用，确保事件回调能查到状态。
     *
     * @return 本轮落库的 user/assistant 行 id；sessionId 缺失或建行异常时返回 {@code null}。
     */
    public OpenResult openForStream(ReactRequest request, String agentCode) {
        String sessionId = request.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }
        try {
            Long userId = SessionHelper.getUserId();
            Long tenantId = resolveTenantId();
            LocalDateTime now = LocalDateTime.now();
            sessionService.getOrCreate(sessionId, userId, tenantId, agentCode, request.getMessage());

            if (request.getEditUserRecordId() != null) {
                return prepareEditResend(request, agentCode, userId, tenantId, now);
            } else if (request.getRetryOfRecordId() != null) {
                return prepareRetryResend(request, agentCode, userId, tenantId, now);
            } else {
                AiChatRecord userRow = AiChatRecord.builder()
                        .sessionId(sessionId).agentCode(agentCode)
                        .userId(userId).tenantId(tenantId)
                        .role("user").content(request.getMessage())
                        .attachmentJson(buildAttachmentJson(request.getImageFileIds()))
                        .status("done").createTime(now).updateTime(now)
                        .build();
                recordService.save(userRow);

                AiChatRecord assistantRow = AiChatRecord.builder()
                        .sessionId(sessionId).agentCode(agentCode)
                        .userId(userId).tenantId(tenantId)
                        .role("assistant").content("")
                        .status(AiChatMessageStatus.STREAMING.getCode()).createTime(now).updateTime(now)
                        .build();
                recordService.save(assistantRow);

                StreamState state = new StreamState(sessionId, userId, tenantId, assistantRow);
                stateBySession.put(sessionId, state);
                return new OpenResult(userRow.getId(), assistantRow.getId());
            }
        } catch (Exception e) {
            log.warn("[AgentChatPersister] 新对话建行失败, sessionId={}", sessionId, e);
            stateBySession.remove(sessionId);
            return null;
        }
    }

    /**
     * HITL 恢复：请求线程调用。
     * <ul>
     *   <li>拒绝（confirmed=false）：无后续 loop，直接把原 assistant 行与其等待中的工具行收口为 aborted。</li>
     *   <li>确认（confirmed=true）：只续跑未完成的工具；保留旧工具行与正文，
     *       接回等待确认的行并继续追加，不能按旧方式清空并重放。</li>
     * </ul>
     * 由 reactAgent.resume 验证身份并消费确认后、loop 启动前回调，越权/重复确认不得修改消息。
     */
    public void openForResume(String interruptId, boolean confirmed) {
        if (!StringUtils.hasText(interruptId)) {
            return;
        }
        try {
            AiChatRecord assistant = recordService.findByInterruptId(interruptId);
            if (assistant == null) {
                log.warn("[AgentChatPersister] 恢复未命中原消息, interruptId={}", interruptId);
                return;
            }
            String sessionId = assistant.getSessionId();
            List<AiChatMessageToolCall> prior = toolCallService.listByRecord(assistant.getId());

            if (!confirmed) {
                // 用户拒绝：等待中的工具行 → aborted，assistant 行 → aborted
                for (AiChatMessageToolCall tc : prior) {
                    if (AiChatMessageStatus.WAITING_CONFIRM.matches(tc.getStatus())
                            || AiChatMessageStatus.RUNNING.matches(tc.getStatus())) {
                        tc.setStatus(AiChatMessageStatus.ABORTED.getCode());
                        tc.setErrorMsg("用户拒绝执行工具");
                        tc.setUpdateTime(LocalDateTime.now());
                        toolCallService.updateById(tc);
                    }
                }
                assistant.setStatus(AiChatMessageStatus.ABORTED.getCode());
                assistant.setInterruptId(null);
                assistant.setErrorMsg("用户拒绝执行工具");
                assistant.setUpdateTime(LocalDateTime.now());
                recordService.updateById(assistant);
                sessionService.touchSession(sessionId);
                return;
            }

            // 只续跑待处理调用，保留前半轮已完成的工具与输出。
            assistant.setStatus(AiChatMessageStatus.STREAMING.getCode());
            assistant.setInterruptId(null);
            assistant.setErrorMsg(null);
            assistant.setUpdateTime(LocalDateTime.now());
            recordService.updateById(assistant);

            StreamState state = new StreamState(sessionId, assistant.getUserId(), assistant.getTenantId(), assistant);
            state.content.append(assistant.getContent() == null ? "" : assistant.getContent());
            state.reasoning.append(assistant.getReasoning() == null ? "" : assistant.getReasoning());
            state.toolSeq = prior.stream().map(AiChatMessageToolCall::getSeq)
                    .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(-1) + 1;
            state.openToolCall = prior.stream()
                    .filter(call -> AiChatMessageStatus.WAITING_CONFIRM.matches(call.getStatus()))
                    .reduce((previous, next) -> next).orElse(null);
            if (state.openToolCall != null) {
                state.openToolCall.setStatus(AiChatMessageStatus.RUNNING.getCode());
                state.openToolCall.setUpdateTime(LocalDateTime.now());
                toolCallService.updateById(state.openToolCall);
            }
            stateBySession.put(sessionId, state);
        } catch (Exception e) {
            log.warn("[AgentChatPersister] 恢复接回失败, interruptId={}", interruptId, e);
        }
    }

    // ==================== loop 线程：事件回调 ====================

    @Override
    public void onEvent(AgentEvent event) {
        String sessionId = event.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        StreamState state = stateBySession.get(sessionId);
        if (state == null) {
            // 非本编排层开启的流（无 sessionId / 未建行 / 已终止移除），忽略
            return;
        }
        try {
            handle(state, event);
        } catch (Exception e) {
            log.warn("[AgentChatPersister] 事件落库失败, sessionId={}, type={}", sessionId, event.getEventType(), e);
        }
    }

    private void handle(StreamState state, AgentEvent event) {
        JSONObject data = parseData(event.getData());
        switch (event.getEventType()) {
            case THINKING_BLOCK_DELTA -> {
                markFirstToken(state);
                appendIfPresent(state.reasoning, data, "text");
            }
            case TEXT_BLOCK_DELTA -> {
                markFirstToken(state);
                appendIfPresent(state.content, data, "text");
            }

            case TOOL_CALL_START -> openToolCall(state, data);
            case TOOL_RESULT_TEXT_DELTA, TOOL_RESULT_DATA_DELTA -> appendIfPresent(state.toolResult, data, "content");
            case TOOL_RESULT_END -> {
                // 缺结果/非法 JSON 不等于成功，不能因结束事件名称就美化历史记录。
                boolean success = data != null && Boolean.TRUE.equals(data.get("success"));
                String error = data != null && Boolean.FALSE.equals(data.get("success"))
                        ? "工具执行失败" : "工具未返回有效成功结果";
                closeToolCall(state, success
                                ? AiChatMessageStatus.SUCCESS.getCode() : AiChatMessageStatus.ERROR.getCode(),
                        success ? null : error);
            }
            case ALL_TOOLS_DENIED -> closeToolCall(state, AiChatMessageStatus.ABORTED.getCode(), "工具被拒绝执行");

            case REQUIRE_USER_CONFIRM -> markWaitingConfirm(state, data);

            case MODEL_CALL_END -> {
                if (data != null && data.containsKey("error")) {
                    state.modelError = data.getString("error");
                } else if (data != null && data.containsKey("usage")) {
                    // 决策19：捕获用量，settle 时落到 usage_json / token_usage。多轮时以最后一轮为准。
                    JSONObject usage = data.getJSONObject("usage");
                    if (usage != null) {
                        state.usageJson = usage.toJSONString();
                        Integer total = usage.getInteger("totalTokens");
                        if (total != null) {
                            state.tokenUsage = total;
                        }
                    }
                }
            }
            case EXCEED_MAX_ITERS -> state.modelError = "超过最大迭代次数";

            case AGENT_END -> {
                if (data != null && data.getBooleanValue("aborted")) {
                    settle(state, AiChatMessageStatus.ABORTED.getCode(), null);
                } else if (data != null && data.containsKey("error")) {
                    settle(state, AiChatMessageStatus.ERROR.getCode(), data.getString("error"));
                } else if (state.modelError != null) {
                    settle(state, AiChatMessageStatus.ERROR.getCode(), state.modelError);
                } else {
                    settle(state, AiChatMessageStatus.DONE.getCode(), null);
                }
            }
            default -> {
                /* AGENT_START / HINT_BLOCK / TEXT_BLOCK_START|END / MODEL_CALL_START /
                   TOOL_RESULT_START 等不改 ai_chat_record；逐事件明细由 ai_agent_event 承载 */
            }
        }
    }

    private void openToolCall(StreamState state, JSONObject data) {
        // 防御：上一个工具调用未正常收尾时先收口
        if (state.openToolCall != null) {
            closeToolCall(state, AiChatMessageStatus.ERROR.getCode(), "工具调用未正常结束");
        }
        state.toolResult.setLength(0);
        String toolName = data != null ? data.getString("tool") : null;
        String argsJson = data != null && data.get("args") != null ? JSON.toJSONString(data.get("args")) : null;
        LocalDateTime now = LocalDateTime.now();
        AiChatMessageToolCall call = AiChatMessageToolCall.builder()
                .tenantId(state.tenantId)
                .recordId(state.assistant.getId())
                .sessionId(state.sessionId)
                .seq(state.toolSeq++)
                .toolName(toolName)
                .toolArgsJson(argsJson)
                .status(AiChatMessageStatus.RUNNING.getCode())
                .createBy(state.userId).createTime(now)
                .updateBy(state.userId).updateTime(now)
                .build();
        toolCallService.save(call);
        state.openToolCall = call;
    }

    private void closeToolCall(StreamState state, String status, String error) {
        AiChatMessageToolCall call = state.openToolCall;
        if (call == null) {
            return;
        }
        call.setStatus(status);
        call.setErrorMsg(error);
        if (AiChatMessageStatus.SUCCESS.matches(status) && state.toolResult.length() > 0) {
            call.setToolResultJson(state.toolResult.toString());
        }
        call.setUpdateTime(LocalDateTime.now());
        toolCallService.updateById(call);
        state.openToolCall = null;
        state.toolResult.setLength(0);
    }

    private void markWaitingConfirm(StreamState state, JSONObject data) {
        String interruptId = data != null ? data.getString("interruptId") : null;
        if (state.openToolCall != null) {
            state.openToolCall.setStatus(AiChatMessageStatus.WAITING_CONFIRM.getCode());
            state.openToolCall.setUpdateTime(LocalDateTime.now());
            toolCallService.updateById(state.openToolCall);
            state.openToolCall = null;
        }
        AiChatRecord assistant = state.assistant;
        assistant.setStatus(AiChatMessageStatus.WAITING_CONFIRM.getCode());
        assistant.setInterruptId(interruptId);
        assistant.setContent(state.content.toString());
        assistant.setReasoning(nullIfEmpty(state.reasoning));
        assistant.setUsageJson(state.usageJson);
        if (state.tokenUsage != null) {
            assistant.setTokenUsage(state.tokenUsage);
        }
        assistant.setUpdateTime(LocalDateTime.now());
        recordService.updateById(assistant);
        // 当前 loop 到此返回，后续 AGENT_END{interrupted} 不应再落；移除状态即可
        state.settled.set(true);
        finish(state);
    }

    /**
     * 终止收口：写最终状态 + 正文 + 思考过程，并移除流状态。幂等，只生效一次。
     */
    private void settle(StreamState state, String status, String error) {
        if (!state.settled.compareAndSet(false, true)) {
            return;
        }
        try {
            if (state.openToolCall != null) {
                closeToolCall(state, AiChatMessageStatus.ABORTED.matches(status)
                        ? AiChatMessageStatus.ABORTED.getCode()
                        : AiChatMessageStatus.ERROR.getCode(), error);
            }
            AiChatRecord assistant = state.assistant;
            assistant.setStatus(status);
            assistant.setContent(state.content.toString());
            assistant.setReasoning(nullIfEmpty(state.reasoning));
            assistant.setUsageJson(state.usageJson);
            if (state.tokenUsage != null) {
                assistant.setTokenUsage(state.tokenUsage);
            }
            assistant.setErrorMsg(error);
            if (state.firstTokenAtMs != null) {
                assistant.setFirstTokenMs(Math.max(0L, state.firstTokenAtMs - state.openedAtMs));
            }
            assistant.setTotalMs(Math.max(0L, System.currentTimeMillis() - state.openedAtMs));
            assistant.setUpdateTime(LocalDateTime.now());
            recordService.updateById(assistant);
            sessionService.touchSession(state.sessionId);
        } finally {
            finish(state);
        }
    }

    private void finish(StreamState state) {
        stateBySession.remove(state.sessionId, state);
    }

    // ==================== 工具方法 ====================

    private JSONObject parseData(String data) {
        if (!StringUtils.hasText(data)) {
            return null;
        }
        try {
            return JSON.parseObject(data);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendIfPresent(StringBuilder buf, JSONObject data, String key) {
        if (data == null) {
            return;
        }
        String value = data.getString(key);
        if (value != null) {
            buf.append(value);
        }
    }

    private void markFirstToken(StreamState state) {
        if (state.firstTokenAtMs == null) {
            state.firstTokenAtMs = System.currentTimeMillis();
        }
    }

    private OpenResult prepareRetryResend(ReactRequest request, String agentCode,
                                         Long userId, Long tenantId, LocalDateTime now) {
        AiChatRecord latestAssistant = recordService.findLatestAssistantBySession(request.getSessionId());
        if (latestAssistant == null || !userId.equals(latestAssistant.getUserId())) {
            throw new IllegalStateException("仅允许对会话内最新 assistant 消息重试");
        }
        recordService.softDeleteById(latestAssistant.getId());
        AiChatRecord assistantRow = AiChatRecord.builder()
                .sessionId(request.getSessionId()).agentCode(agentCode)
                .userId(userId).tenantId(tenantId)
                .role("assistant").content("")
                .status(AiChatMessageStatus.STREAMING.getCode()).createTime(now).updateTime(now)
                .build();
        recordService.save(assistantRow);
        stateBySession.put(request.getSessionId(),
                new StreamState(request.getSessionId(), userId, tenantId, assistantRow));
        return new OpenResult(null, assistantRow.getId());
    }

    private OpenResult prepareEditResend(ReactRequest request, String agentCode,
                                        Long userId, Long tenantId, LocalDateTime now) {
        AiChatRecord latestUser = recordService.findLatestUserBySession(request.getSessionId());
        if (latestUser == null || !userId.equals(latestUser.getUserId())) {
            throw new IllegalStateException("仅允许对会话内最新 user 消息重发");
        }
        recordService.softDeleteFromRecordId(request.getSessionId(), latestUser.getId());
        AiChatRecord userRow = AiChatRecord.builder()
                .sessionId(request.getSessionId()).agentCode(agentCode)
                .userId(userId).tenantId(tenantId)
                .role("user").content(request.getMessage())
                .attachmentJson(buildAttachmentJson(request.getImageFileIds()))
                .status("done").createTime(now).updateTime(now)
                .build();
        recordService.save(userRow);
        AiChatRecord assistantRow = AiChatRecord.builder()
                .sessionId(request.getSessionId()).agentCode(agentCode)
                .userId(userId).tenantId(tenantId)
                .role("assistant").content("")
                .status(AiChatMessageStatus.STREAMING.getCode()).createTime(now).updateTime(now)
                .build();
        recordService.save(assistantRow);
        stateBySession.put(request.getSessionId(),
                new StreamState(request.getSessionId(), userId, tenantId, assistantRow));
        return new OpenResult(userRow.getId(), assistantRow.getId());
    }

    private String buildAttachmentJson(List<Long> imageFileIds) {
        if (imageFileIds == null || imageFileIds.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> attachments = new ArrayList<>();
        for (Long fileId : imageFileIds) {
            attachments.add(Map.of("fileId", fileId, "type", "image"));
        }
        return JSON.toJSONString(attachments);
    }

    private String nullIfEmpty(StringBuilder buf) {
        return buf.length() > 0 ? buf.toString() : null;
    }

    private Long resolveTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    /**
     * openForStream 结果：本轮落库的 user/assistant 行 id，供 {@code AgentEngineService}
     * 在 SSE 流首以 PERSIST_META 事件下发前端。retry 场景不新建 user 行，{@code userRecordId} 为 {@code null}。
     */
    public record OpenResult(Long userRecordId, Long assistantRecordId) {
    }

    /**
     * 单个流的持久化状态。同一 session 的事件由单一 loop 线程顺序回调，流内串行访问；
     * settled 用 AtomicBoolean 保证终止收口幂等（MODEL_CALL_END error 与随后 AGENT_END 不重复收口）。
     */
    private static class StreamState {
        private final String sessionId;
        private final Long userId;
        private final Long tenantId;
        private final AiChatRecord assistant;

        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();

        private int toolSeq = 0;
        private AiChatMessageToolCall openToolCall;
        private final StringBuilder toolResult = new StringBuilder();

        private String usageJson;
        private Integer tokenUsage;
        private String modelError;
        private Long openedAtMs = System.currentTimeMillis();
        private Long firstTokenAtMs;
        private final AtomicBoolean settled = new AtomicBoolean(false);

        StreamState(String sessionId, Long userId, Long tenantId, AiChatRecord assistant) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.tenantId = tenantId;
            this.assistant = assistant;
        }
    }
}
