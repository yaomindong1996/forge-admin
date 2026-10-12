package com.mdframe.forge.plugin.ai.agent.engine.tool;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEvent;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventPublisher;
import com.mdframe.forge.plugin.ai.agent.engine.event.AgentEventType;
import com.mdframe.forge.plugin.ai.agent.engine.hitl.InterruptStore;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 唯一执行入口：当前绑定实例 + 现有权限决策；原始模型/Advisor 不得绕过该链路。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolExecutor {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private static final TypeReference<Map<String, Object>> ARGUMENT_TYPE = new TypeReference<>() { };

    private final PermissionEngine permissionEngine;
    private final InterruptStore interruptStore;
    private final AgentEventPublisher eventPublisher;

    public enum Outcome { DONE, INTERRUPTED, CANCELLED }

    public Outcome drain(ReactContext context, Sinks.Many<AgentEvent> sink) {
        while (context.pendingToolCall() != null) {
            context.verifyExecution();
            if (context.isCancelled()) {
                return Outcome.CANCELLED;
            }
            Outcome outcome = process(context, context.pendingToolCall(), sink);
            if (outcome != Outcome.DONE) {
                return outcome;
            }
        }
        return context.isCancelled() ? Outcome.CANCELLED : Outcome.DONE;
    }

    private Outcome process(ReactContext context, AssistantMessage.ToolCall call, Sinks.Many<AgentEvent> sink) {
        boolean confirmed = call.id().equals(context.getConfirmedToolCallId());
        if (!confirmed) {
            emit(context, sink, AgentEventType.TOOL_CALL_START,
                    Map.of("tool", call.name(), "toolCallId", call.id(), "args", safeArguments(call.arguments())));
        }
        AgentTool tool = boundTool(context, call.name());
        if (tool == null) {
            deny(context, call, sink);
            return Outcome.DONE;
        }
        Map<String, Object> arguments;
        try {
            arguments = arguments(call.arguments());
        } catch (IllegalArgumentException ex) {
            complete(context, call, ToolResult.error("工具参数必须是完整 JSON 对象"), sink);
            return Outcome.DONE;
        }
        PermissionDecision decision = permissionEngine.decide(
                context.getAgentId(), call.name(), context.getToolGroupMode());
        if (context.isCancelled()) {
            return Outcome.CANCELLED;
        }
        if (decision == null || decision == PermissionDecision.DENY) {
            deny(context, call, sink);
            return Outcome.DONE;
        }
        if (decision == PermissionDecision.ASK && !confirmed) {
            context.verifyExecution();
            if (context.isCancelled()) {
                return Outcome.CANCELLED;
            }
            String interruptId = interruptStore.save(context);
            emit(context, sink, AgentEventType.REQUIRE_USER_CONFIRM,
                    Map.of("interruptId", interruptId, "tool", call.name(),
                            "toolCallId", call.id(), "args", arguments));
            emit(context, sink, AgentEventType.AGENT_END, Map.of("interrupted", true, "interruptId", interruptId));
            return Outcome.INTERRUPTED;
        }
        ToolResult result = execute(context, call, tool, arguments);
        context.verifyExecution();
        if (context.isCancelled()) {
            return Outcome.CANCELLED;
        }
        complete(context, call, result, sink);
        return Outcome.DONE;
    }

    private AgentTool boundTool(ReactContext context, String name) {
        List<AgentTool> bound = context.getBoundTools() == null ? List.of() : context.getBoundTools();
        List<AgentTool> matches = bound.stream().filter(Objects::nonNull)
                .filter(tool -> name.equals(tool.getKey())).toList();
        // 同名不同来源不能退回全局 registry 猜测；恢复后配置有歧义也必须拒绝。
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private ToolResult execute(ReactContext context, AssistantMessage.ToolCall call,
                               AgentTool tool, Map<String, Object> arguments) {
        context.verifyExecution();
        if (context.isCancelled()) {
            return ToolResult.error("会话已停止，工具未执行");
        }
        try {
            ToolContext toolContext = ToolContext.of(context.getSessionId(), context.getAgentId(),
                    context.getTenantId(), context.getTurnIndex());
            toolContext.setKnowledgeIds(AgentToolBindingResolver.knowledgeIds(
                    context.getAgent() == null ? null : context.getAgent().getKnowledgeIds()));
            ToolResult result = tool.execute(arguments, toolContext);
            return result == null ? ToolResult.error("工具未返回结果") : result;
        } catch (Exception ex) {
            log.error("[AgentTool] 执行失败: tool={}, exceptionType={}", call.name(), ex.getClass().getSimpleName());
            return ToolResult.error("工具执行失败，请稍后重试");
        }
    }

    private void deny(ReactContext context, AssistantMessage.ToolCall call, Sinks.Many<AgentEvent> sink) {
        emit(context, sink, AgentEventType.ALL_TOOLS_DENIED, Map.of("tool", call.name(), "toolCallId", call.id()));
        context.addToolResult(call, "工具未绑定或当前权限不允许执行");
    }

    private void complete(ReactContext context, AssistantMessage.ToolCall call,
                          ToolResult result, Sinks.Many<AgentEvent> sink) {
        ToolResult.Type type = result.getType() == null ? ToolResult.Type.TEXT : result.getType();
        String content = result.isSuccess() ? Objects.toString(result.getContent(), "")
                : "错误: " + Objects.toString(result.getError(), "工具执行失败");
        emit(context, sink, AgentEventType.TOOL_RESULT_START,
                Map.of("tool", call.name(), "type", type.name().toLowerCase(java.util.Locale.ROOT),
                        "toolCallId", call.id(), "success", result.isSuccess()));
        AgentEventType delta = type == ToolResult.Type.DATA
                ? AgentEventType.TOOL_RESULT_DATA_DELTA : AgentEventType.TOOL_RESULT_TEXT_DELTA;
        emit(context, sink, delta, Map.of("content", content));
        emit(context, sink, AgentEventType.TOOL_RESULT_END,
                Map.of("tool", call.name(), "toolCallId", call.id(), "success", result.isSuccess()));
        context.addToolResult(call, content);
    }

    private static Map<String, Object> arguments(String raw) {
        try {
            var node = JSON_MAPPER.readTree(raw == null ? "" : raw);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("参数不是对象");
            }
            return JSON_MAPPER.convertValue(node, ARGUMENT_TYPE);
        } catch (java.io.IOException ex) {
            throw new IllegalArgumentException("参数不是合法 JSON");
        }
    }

    private Object safeArguments(String raw) {
        try {
            return arguments(raw);
        } catch (IllegalArgumentException ex) {
            return Map.of();
        }
    }

    private void emit(ReactContext context, Sinks.Many<AgentEvent> sink, AgentEventType type, Map<String, ?> data) {
        if (context.isExecutionLost()) {
            return;
        }
        context.verifyExecution();
        AgentEvent event = AgentEvent.of(context.getSessionId(), context.getTurnIndex(), type, JSON.toJSONString(data));
        eventPublisher.publish(event);
        sink.tryEmitNext(event);
    }
}
