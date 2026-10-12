package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;

/** 中断消息白名单，仅包含文本与工具往返值；不能通过类型名恢复任意 Java 对象。 */
public record AgentMessageSnapshot(MessageType role, String text, List<Call> calls,
                                   List<Response> responses, String reasoning) {

    public AgentMessageSnapshot {
        if (role == null) {
            throw new BusinessException("中断消息类型无效");
        }
        calls = calls == null ? List.of() : List.copyOf(calls);
        responses = responses == null ? List.of() : List.copyOf(responses);
    }

    public static AgentMessageSnapshot from(Message message) {
        if (message instanceof UserMessage user && !user.getMedia().isEmpty()) {
            // 当前入口的附件按 ReactContext.fileId 保存，恢复重取 URL；拒绝未知媒体历史，不能静默丢弃。
            throw new BusinessException("中断历史包含无法恢复的媒体，请重新发起对话");
        }
        List<Call> calls = message instanceof AssistantMessage assistant
                ? assistant.getToolCalls().stream().map(Call::from).toList() : List.of();
        List<Response> responses = message instanceof ToolResponseMessage tool
                ? tool.getResponses().stream().map(Response::from).toList() : List.of();
        return new AgentMessageSnapshot(message.getMessageType(), message.getText(), calls, responses,
                reasoning(message));
    }

    public Message restore() {
        String content = text == null ? "" : text;
        return switch (role) {
            case SYSTEM -> new SystemMessage(content);
            case USER -> new UserMessage(content);
            case ASSISTANT -> AssistantMessage.builder().content(content)
                    .toolCalls(calls.stream().map(Call::restore).toList())
                    .properties(reasoning == null ? Map.of() : Map.of("reasoningContent", reasoning)).build();
            case TOOL -> ToolResponseMessage.builder().responses(responses.stream().map(Response::restore).toList())
                    .build();
        };
    }

    public static String reasoning(Message message) {
        for (String key : List.of("reasoningContent", "reasoning_content", "reasoning")) {
            Object value = message.getMetadata().get(key);
            if (value instanceof String text && !text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    public record Call(String id, String type, String name, String arguments) {
        public Call {
            if (id == null || id.isBlank() || name == null || name.isBlank() || !"function".equals(type)) {
                throw new BusinessException("中断工具调用无效");
            }
        }

        public static Call from(AssistantMessage.ToolCall call) {
            return new Call(call.id(), call.type(), call.name(), call.arguments());
        }

        public AssistantMessage.ToolCall restore() {
            return new AssistantMessage.ToolCall(id, type, name, arguments);
        }
    }

    public record Response(String id, String name, String content) {
        public Response {
            if (id == null || id.isBlank() || name == null || name.isBlank()) {
                throw new BusinessException("中断工具结果无效");
            }
        }

        public static Response from(ToolResponseMessage.ToolResponse response) {
            return new Response(response.id(), response.name(), response.responseData());
        }

        public ToolResponseMessage.ToolResponse restore() {
            return new ToolResponseMessage.ToolResponse(id, name, content);
        }
    }
}
