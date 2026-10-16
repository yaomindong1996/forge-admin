package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentLoopTaskExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolExecutor;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentToolPromptFactory;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationObservation;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationOutcome;
import com.mdframe.forge.plugin.ai.invocation.AiInvocationPhase;
import com.mdframe.forge.plugin.ai.invocation.service.AiModelInvocationRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct 循环核心。
 * 纯 ReAct 循环（推理→行动迭代），底层用 Spring AI ChatModel。
 * 不使用 Spring AI 的 ChatClient.tools() / ToolCallAdvisor——工具调用循环完全自控。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactLoop {

    private final AgentEventPublisher eventPublisher;
    private final AiModelInvocationRecorder invocationRecorder;
    private final AgentToolExecutor toolExecutor;
    private final AgentLoopTaskExecutor loopExecutor;

    /**
     * 执行 ReAct 循环
     */
    public Flux<AgentEvent> run(ReactContext context) {
        return run(context, () -> { });
    }

    public Flux<AgentEvent> run(ReactContext context, Runnable onTerminated) {
        Sinks.Many<AgentEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        String requestId = UUID.randomUUID().toString();
        long startedAtMs = System.currentTimeMillis();
        LoopTelemetry telemetry = new LoopTelemetry();
        Runnable task = () -> {
            try {
                executeLoop(context, sink, telemetry);
            } catch (Exception ex) {
                log.error("[ReactLoop] 循环失败: sessionId={}, exceptionType={}",
                        context.getSessionId(), ex.getClass().getSimpleName());
                telemetry.outcome = AiInvocationOutcome.FAILED;
                telemetry.phase = AiInvocationPhase.STREAMING;
                emit(context, sink, AgentEventType.AGENT_END, Map.of("error", "AI 处理失败，请稍后重试"));
            } finally {
                try {
                    finishRun(context, sink, telemetry, requestId, startedAtMs);
                } finally {
                    onTerminated.run();
                }
            }
        };
        try {
            loopExecutor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            telemetry.outcome = AiInvocationOutcome.FAILED;
            emit(context, sink, AgentEventType.AGENT_END, Map.of("error", "Agent 服务繁忙，请稍后重试"));
            try {
                finishRun(context, sink, telemetry, requestId, startedAtMs);
            } finally {
                onTerminated.run();
            }
        }
        return sink.asFlux();
    }

    private void finishRun(ReactContext context, Sinks.Many<AgentEvent> sink, LoopTelemetry telemetry,
                           String requestId, long startedAtMs) {
        if (context.isExecutionLost()) {
            telemetry.outcome = AiInvocationOutcome.FAILED;
            // 只结束旧请求的直接流；绝不能通过 publisher 收口新 owner 的数据库运行态。
            sink.tryEmitNext(AgentEvent.of(context.getSessionId(), context.getTurnIndex(), AgentEventType.ERROR,
                    com.alibaba.fastjson2.JSON.toJSONString(Map.of(
                            "message", "Agent 执行占用已失效，请刷新会话查看状态后重试"))));
        }
        try {
            recordTelemetry(context, requestId, startedAtMs, telemetry);
        } finally {
            // loop 才是事件缓冲的所有者；HTTP 断连不能提前刷新/释放后台执行。
            try {
                eventPublisher.flush();
            } finally {
                sink.tryEmitComplete();
            }
        }
    }

    private void executeLoop(ReactContext context, Sinks.Many<AgentEvent> sink, LoopTelemetry telemetry) {
        emitStart(context, sink);
        while (context.getTurnIndex() < context.getMaxIters() && !context.isCancelled()) {
            context.verifyExecution();
            if (context.isCancelled()) {
                break;
            }
            if (context.pendingToolCall() != null) {
                AgentToolExecutor.Outcome outcome = toolExecutor.drain(context, sink);
                if (outcome == AgentToolExecutor.Outcome.INTERRUPTED) {
                    telemetry.outcome = AiInvocationOutcome.CANCELLED;
                    telemetry.phase = AiInvocationPhase.STREAMING;
                    return;
                }
                if (outcome == AgentToolExecutor.Outcome.CANCELLED) {
                    break;
                }
                context.completeToolTurn();
                continue;
            }
            ChatResponse response = reason(context, sink, telemetry);
            context.verifyExecution();
            if (context.isCancelled() || response == null || response.getResult() == null) {
                break;
            }
            AssistantMessage assistant = response.getResult().getOutput();
            if (!assistant.hasToolCalls()) {
                break;
            }
            // 先保存整轮声明和调用 ID；只有全部结果返回后才允许下一次模型调用。
            context.beginToolTurn(assistant);
        }
        finishLoop(context, sink, telemetry);
    }

    private void emitStart(ReactContext context, Sinks.Many<AgentEvent> sink) {
        emit(context, sink, AgentEventType.AGENT_START, Map.of("agentCode", context.getAgentCode()));
        if (context.getConfirmedToolCallId() != null) {
            emit(context, sink, AgentEventType.USER_CONFIRM_RESULT,
                    Map.of("confirmed", true, "toolCallId", context.getConfirmedToolCallId()));
        }
        if (context.getImageFileIds() != null && !context.getImageFileIds().isEmpty()) {
            emit(context, sink, AgentEventType.HINT_BLOCK,
                    Map.of("hint", "当前对话包含图片附件，请确认模型支持视觉能力。"));
        }
    }

    private ChatResponse reason(ReactContext context, Sinks.Many<AgentEvent> sink, LoopTelemetry telemetry) {
        try {
            Prompt prompt = AgentToolPromptFactory.create(context);
            emit(context, sink, AgentEventType.MODEL_CALL_START, Map.of("turn", context.getTurnIndex()));
            telemetry.dispatched = true;
            ChatResponse response = callModel(context, sink, context.getTurnIndex(), prompt);
            if (!context.isCancelled() && (response == null || response.getResult() == null)) {
                throw new com.mdframe.forge.starter.core.exception.BusinessException("模型未返回有效响应");
            }
            applyUsage(response, telemetry);
            String usage = buildUsageJson(response);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("turn", context.getTurnIndex());
            if (usage != null) {
                data.put("usage", com.alibaba.fastjson2.JSON.parseObject(usage));
            }
            emit(context, sink, AgentEventType.MODEL_CALL_END, data);
            return response;
        } catch (Exception ex) {
            log.warn("[ReactLoop] 模型调用未完成: sessionId={}, exceptionType={}",
                    context.getSessionId(), ex.getClass().getSimpleName());
            telemetry.phase = AiInvocationPhase.STREAMING;
            if (context.isCancelled()) {
                telemetry.outcome = AiInvocationOutcome.CANCELLED;
                return null;
            }
            telemetry.outcome = AiInvocationOutcome.FAILED;
            telemetry.error = "AI 模型调用失败，请稍后重试";
            emit(context, sink, AgentEventType.MODEL_CALL_END, Map.of("error", telemetry.error));
            return null;
        }
    }

    private void finishLoop(ReactContext context, Sinks.Many<AgentEvent> sink, LoopTelemetry telemetry) {
        if (context.isCancelled()) {
            telemetry.outcome = AiInvocationOutcome.CANCELLED;
            telemetry.phase = AiInvocationPhase.STREAMING;
            emit(context, sink, AgentEventType.AGENT_END, Map.of("aborted", true));
            return;
        }
        if (context.getTurnIndex() >= context.getMaxIters()) {
            telemetry.outcome = AiInvocationOutcome.FAILED;
            telemetry.error = "超过最大迭代次数";
            emit(context, sink, AgentEventType.EXCEED_MAX_ITERS, Map.of("maxIters", context.getMaxIters()));
        }
        emit(context, sink, AgentEventType.AGENT_END, telemetry.error == null
                ? Map.of("turns", context.getTurnIndex()) : Map.of("error", telemetry.error));
    }

    private void emit(ReactContext context, Sinks.Many<AgentEvent> sink, AgentEventType type, Map<String, ?> data) {
        AgentEvent event = AgentEvent.of(context.getSessionId(), context.getTurnIndex(), type,
                com.alibaba.fastjson2.JSON.toJSONString(data));
        publish(context, sink, event);
    }

    private void publish(ReactContext context, Sinks.Many<AgentEvent> sink, AgentEvent event) {
        if (context.isExecutionLost()) {
            return;
        }
        context.verifyExecution();
        eventPublisher.publish(event);
        sink.tryEmitNext(event);
    }

    /**
     * 流式调用模型：底层用 {@code chatModel.stream(prompt)}，在 loop 线程用 {@code toIterable()}
     * 阻塞拉取逐块消费——正文按 TEXT_BLOCK_START/DELTA/END 拆分发出，思考内容按 THINKING_BLOCK_DELTA 发出。
     * 消费过程中用 {@link MessageAggregator} 聚合出完整响应，供上层解析 tool_call 与用量（usage）。
     *
     * <p>所有事件仍在 loop 线程发射（toIterable 在当前线程拉取，不切线程），保持“事件发布均在
     * loop 线程”的不变量，与 HTTP 客户端连接生命周期无关。</p>
     */
    private ChatResponse callModel(ReactContext ctx, Sinks.Many<AgentEvent> sink, int turn, Prompt prompt) {
        ctx.verifyExecution();
        if (ctx.isCancelled()) {
            return null;
        }
        ChatModel chatModel = ctx.getChatModel();

        boolean[] textStarted = {false};
        boolean receivedResult = false;
        StringBuilder thinking = new StringBuilder();
        AtomicReference<ChatResponse> aggregateRef = new AtomicReference<>();

        // MessageAggregator：透传原始 chunk，同时在流完成时回调聚合后的完整响应
        Flux<ChatResponse> stream = new MessageAggregator().aggregate(
                chatModel.stream(prompt).takeUntilOther(ctx.cancellationSignal()), aggregateRef::set);
        for (ChatResponse chunk : stream.toIterable()) {
            if (ctx.isCancelled()) {
                break;
            }
            if (chunk == null || chunk.getResult() == null) {
                continue;
            }
            receivedResult = true;
            AssistantMessage out = chunk.getResult().getOutput();

            // 思考增量（reasoning）：来自模型 metadata（DeepSeek/DashScope 等），逐块发出
            String reasoning = extractReasoningContent(out);
            if (reasoning != null && !reasoning.isEmpty()) {
                thinking.append(reasoning);
                emit(ctx, sink, AgentEventType.THINKING_BLOCK_DELTA, Map.of("text", reasoning));
            }

            // 正文增量：首块发 TEXT_BLOCK_START，随后逐块 TEXT_BLOCK_DELTA
            String text = out.getText();
            if (text != null && !text.isEmpty()) {
                if (!textStarted[0]) {
                    AgentEvent textStart = AgentEvent.of(ctx.getSessionId(), turn,
                            AgentEventType.TEXT_BLOCK_START, null);
                    publish(ctx, sink, textStart);
                    textStarted[0] = true;
                }
                emit(ctx, sink, AgentEventType.TEXT_BLOCK_DELTA, Map.of("text", text));
            }
        }
        // 有正文时收口 TEXT_BLOCK_END
        if (textStarted[0]) {
            AgentEvent textEnd = AgentEvent.of(ctx.getSessionId(), turn, AgentEventType.TEXT_BLOCK_END, null);
            publish(ctx, sink, textEnd);
        }

        return receivedResult ? withReasoning(aggregateRef.get(), thinking.toString()) : null;
    }

    private ChatResponse withReasoning(ChatResponse response, String reasoning) {
        if (response == null || response.getResult() == null || reasoning.isEmpty()) {
            return response;
        }
        List<Generation> generations = new ArrayList<>(response.getResults());
        Generation first = generations.get(0);
        AssistantMessage output = first.getOutput();
        Map<String, Object> properties = new LinkedHashMap<>(output.getMetadata());
        properties.put("reasoningContent", reasoning);
        AssistantMessage combined = AssistantMessage.builder().content(output.getText())
                .toolCalls(output.getToolCalls()).properties(properties).build();
        generations.set(0, new Generation(combined, first.getMetadata()));
        return new ChatResponse(generations, response.getMetadata());
    }

    /**
     * 从模型 metadata 提取思考内容（reasoning）。兼容 reasoningContent / reasoning_content /
     * reasoning 三种键，与 {@code AiClientImpl} 保持一致。
     */
    private String extractReasoningContent(AssistantMessage message) {
        if (message == null || message.getMetadata() == null) {
            return null;
        }
        Map<String, Object> metadata = message.getMetadata();
        for (String key : new String[]{"reasoningContent", "reasoning_content", "reasoning"}) {
            Object v = metadata.get(key);
            if (v instanceof String s) {
                return s;
            }
        }
        return null;
    }

    /**
     * 从聚合响应提取用量 JSON（决策19）。无用量返回 null，不写入 MODEL_CALL_END。
     */
    private String buildUsageJson(ChatResponse resp) {
        try {
            if (resp == null || resp.getMetadata() == null) {
                return null;
            }
            Usage usage = resp.getMetadata().getUsage();
            if (usage == null) {
                return null;
            }
            Long prompt = toLong(usage.getPromptTokens());
            Long completion = toLong(usage.getCompletionTokens());
            Long total = toLong(usage.getTotalTokens());
            if (prompt == null && completion == null && total == null) {
                return null;
            }
            return "{\"promptTokens\":" + prompt + ",\"completionTokens\":" + completion
                    + ",\"totalTokens\":" + total + "}";
        } catch (Exception e) {
            log.debug("[ReactLoop] 用量解析跳过: {}", e.getMessage());
            return null;
        }
    }

    private Long toLong(Number number) {
        return number == null ? null : number.longValue();
    }

    /**
     * 把聚合响应的用量写入埋点累加器（决策19 + task10）。多轮时以最后一轮为准，解析异常静默跳过。
     */
    private void applyUsage(ChatResponse resp, LoopTelemetry tel) {
        try {
            if (resp == null || resp.getMetadata() == null) {
                return;
            }
            Usage usage = resp.getMetadata().getUsage();
            if (usage == null) {
                return;
            }
            tel.promptTokens = toLong(usage.getPromptTokens());
            tel.completionTokens = toLong(usage.getCompletionTokens());
            tel.totalTokens = toLong(usage.getTotalTokens());
        } catch (Exception e) {
            log.debug("[ReactLoop] 用量埋点跳过: {}", e.getMessage());
        }
    }

    /**
     * 最小对话埋点（task10）：在循环所有退出路径统一记录一条调用观测，复用 {@link AiModelInvocationRecorder}。
     * 引擎侧不解析路由/供应商/模型/定价，这些字段置空；至少覆盖发送(dispatched)、完成/错误/中断(outcome)、
     * token 用量与总耗时(latency)。埋点失败绝不影响主流程。
     */
    private void recordTelemetry(ReactContext ctx, String requestId, long startedAtMs, LoopTelemetry tel) {
        try {
            long latency = System.currentTimeMillis() - startedAtMs;
            if (latency < 0) {
                latency = 0;
            }
            Long tenantId = ctx.getTenantId() != null ? ctx.getTenantId() : 1L;
            AiInvocationObservation obs = new AiInvocationObservation(
                    requestId, tenantId, ctx.getUserId(), ctx.getAgentCode(), ctx.getSessionId(),
                    tel.phase, tel.dispatched, tel.outcome,
                    null, null, null,
                    null, null, null, null,
                    null, null, null,
                    latency, tel.promptTokens, tel.completionTokens, tel.totalTokens,
                    null, null);
            invocationRecorder.record(obs);
        } catch (Exception e) {
            log.debug("[ReactLoop] 对话埋点记录跳过: sessionId={}, {}", ctx.getSessionId(), e.getMessage());
        }
    }

    /**
     * 单次 run 的埋点累加器：全程在同一 loop 线程内读写，无需同步。
     * 默认视为成功完成；发生模型错误/超限时置 FAILED，HITL 中断置 CANCELLED。
     */
    private static class LoopTelemetry {
        private boolean dispatched = false;
        private AiInvocationOutcome outcome = AiInvocationOutcome.SUCCESS;
        private AiInvocationPhase phase = AiInvocationPhase.COMPLETED;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
        private String error;
    }


}
