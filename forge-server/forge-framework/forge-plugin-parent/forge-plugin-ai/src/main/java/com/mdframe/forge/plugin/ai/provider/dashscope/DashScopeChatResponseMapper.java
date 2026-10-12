package com.mdframe.forge.plugin.ai.provider.dashscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 流式工具参数按 index 拼接；仅结束时输出完整工具声明，避免执行半个 JSON。 */
final class DashScopeChatResponseMapper {

    private final Map<Integer, ToolDelta> toolDeltas = new LinkedHashMap<>();

    void assertComplete() {
        if (!toolDeltas.isEmpty()) {
            throw new BusinessException("通义工具参数未完整返回，请重试");
        }
    }

    ChatResponse response(JsonNode json, String model, boolean stream) {
        if (!json.path("output").path("choices").isArray() || json.path("output").path("choices").size() != 1) {
            throw new BusinessException("通义原生接口未返回单个完整对话结果");
        }
        List<Generation> generations = new ArrayList<>();
        for (JsonNode choice : json.path("output").path("choices")) {
            JsonNode message = choice.path("message");
            String finish = choice.path("finish_reason").asText("");
            List<AssistantMessage.ToolCall> calls = toolCalls(message.path("tool_calls"), finish, stream);
            var assistant = AssistantMessage.builder().content(text(message.path("content")))
                    .toolCalls(calls).properties(Map.of("reasoningContent",
                            message.path("reasoning_content").asText(""))).build();
            generations.add(new Generation(assistant, ChatGenerationMetadata.builder().finishReason(finish).build()));
        }
        var usage = json.path("usage");
        var metadata = ChatResponseMetadata.builder().id(json.path("request_id").asText(""))
                .model(model).usage(new DefaultUsage(usage.path("input_tokens").asInt(),
                        usage.path("output_tokens").asInt(), usage.path("total_tokens").asInt())).build();
        return new ChatResponse(generations, metadata);
    }

    private String text(JsonNode content) {
        if (!content.isArray()) {
            return content.asText("");
        }
        StringBuilder text = new StringBuilder();
        content.forEach(item -> text.append(item.path("text").asText("")));
        return text.toString();
    }

    private List<AssistantMessage.ToolCall> toolCalls(JsonNode calls, String finish, boolean stream) {
        List<AssistantMessage.ToolCall> result = new ArrayList<>();
        for (JsonNode call : calls) {
            if (stream) {
                toolDeltas.computeIfAbsent(call.path("index").asInt(), ignored -> new ToolDelta()).append(call);
            } else {
                result.add(new AssistantMessage.ToolCall(call.path("id").asText(), "function",
                        call.path("function").path("name").asText(),
                        call.path("function").path("arguments").asText()));
            }
        }
        if (!stream && !result.isEmpty() && !"tool_calls".equals(finish)) {
            throw new BusinessException("通义工具调用未正常结束，请重试");
        }
        if (stream && !finish.isBlank() && !"null".equals(finish)) {
            if (!toolDeltas.isEmpty() && !"tool_calls".equals(finish)) {
                throw new BusinessException("通义工具调用未正常结束，请重试");
            }
            toolDeltas.values().forEach(delta -> result.add(delta.call()));
            toolDeltas.clear();
        }
        return result;
    }

    private static final class ToolDelta {
        private String id = "";
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();

        void append(JsonNode json) {
            if (!json.path("id").asText("").isBlank()) {
                if (!id.isEmpty() && !id.equals(json.path("id").asText())) {
                    throw new BusinessException("通义工具分片调用 ID 不一致");
                }
                id = json.path("id").asText();
            }
            if (!json.path("type").asText("function").equals("function")) {
                throw new BusinessException("通义工具类型不支持");
            }
            name.append(json.path("function").path("name").asText(""));
            arguments.append(json.path("function").path("arguments").asText(""));
        }

        AssistantMessage.ToolCall call() {
            return new AssistantMessage.ToolCall(id, "function", name.toString(), arguments.toString());
        }
    }
}
