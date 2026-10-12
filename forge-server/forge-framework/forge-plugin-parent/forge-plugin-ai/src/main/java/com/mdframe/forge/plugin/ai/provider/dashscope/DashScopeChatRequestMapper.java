package com.mdframe.forge.plugin.ai.provider.dashscope;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.Base64;
import java.util.function.Consumer;

/** 将 Spring AI 消息映射为 DashScope 原生 input/parameters，而非 OpenAI 请求。 */
final class DashScopeChatRequestMapper {

    private DashScopeChatRequestMapper() {
    }

    record Request(String path, ObjectNode body) { }

    static Request request(Prompt prompt, ChatOptions defaults, boolean stream) {
        ChatOptions options = prompt.getOptions();
        ObjectNode body = DashScopeNativeClient.JSON.createObjectNode();
        body.put("model", options != null && options.getModel() != null ? options.getModel() : defaults.getModel());
        ArrayNode messages = body.putObject("input").putArray("messages");
        boolean multimodal = prompt.getInstructions().stream()
                .anyMatch(message -> message instanceof MediaContent media && !media.getMedia().isEmpty());
        for (Message message : prompt.getInstructions()) {
            addMessage(messages, message, multimodal);
        }
        ObjectNode parameters = body.putObject("parameters");
        parameters.put("result_format", "message");
        parameters.put("incremental_output", stream);
        addOptions(parameters, defaults);
        addOptions(parameters, options);
        return new Request(multimodal ? "/services/aigc/multimodal-generation/generation"
                : "/services/aigc/text-generation/generation", body);
    }

    private static void addOptions(ObjectNode parameters, ChatOptions options) {
        if (options == null) {
            return;
        }
        put(options.getTemperature(), value -> parameters.put("temperature", value));
        put(options.getMaxTokens(), value -> parameters.put("max_tokens", value));
        put(options.getTopP(), value -> parameters.put("top_p", value));
        put(options.getTopK(), value -> parameters.put("top_k", value));
        if (options.getStopSequences() != null) {
            parameters.set("stop", DashScopeNativeClient.JSON.valueToTree(options.getStopSequences()));
        }
        if (options instanceof ToolCallingChatOptions tools && tools.getToolCallbacks() != null) {
            // 明确空列表也覆盖缓存默认工具，不能跨 Agent 混入其它会话的声明。
            parameters.remove("tools");
            if (!tools.getToolCallbacks().isEmpty()) {
                ArrayNode declarations = parameters.putArray("tools");
                tools.getToolCallbacks().forEach(tool -> addTool(declarations, tool));
            }
        }
    }

    private static <T> void put(T value, Consumer<T> writer) {
        if (value != null) {
            writer.accept(value);
        }
    }

    private static void addTool(ArrayNode tools, ToolCallback callback) {
        var definition = callback.getToolDefinition();
        ObjectNode function = tools.addObject().put("type", "function").putObject("function");
        function.put("name", definition.name()).put("description", definition.description());
        function.set("parameters", DashScopeNativeClient.parse(definition.inputSchema()));
    }

    private static void addMessage(ArrayNode messages, Message message, boolean multimodal) {
        if (message instanceof ToolResponseMessage responses) {
            for (var response : responses.getResponses()) {
                messages.addObject().put("role", "tool").put("name", response.name())
                        .put("tool_call_id", response.id()).put("content", response.responseData());
            }
            return;
        }
        ObjectNode node = messages.addObject().put("role", message.getMessageType().getValue());
        if (multimodal) {
            addMedia(node.putArray("content"), message);
        } else {
            node.put("content", message.getText() == null ? "" : message.getText());
        }
        if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
            Object reasoning = assistant.getMetadata().get("reasoningContent");
            if (reasoning instanceof String text && !text.isEmpty()) {
                node.put("reasoning_content", text);
            }
            ArrayNode calls = node.putArray("tool_calls");
            for (var call : assistant.getToolCalls()) {
                calls.addObject().put("id", call.id()).put("type", "function").putObject("function")
                        .put("name", call.name()).put("arguments", call.arguments());
            }
        }
    }

    private static void addMedia(ArrayNode content, Message message) {
        if (message.getText() != null && !message.getText().isBlank()) {
            content.addObject().put("text", message.getText());
        }
        if (message instanceof MediaContent media) {
            for (Media item : media.getMedia()) {
                String value = item.getData() instanceof byte[] bytes
                        ? "data:" + item.getMimeType() + ";base64," + Base64.getEncoder().encodeToString(bytes)
                        : item.getData().toString();
                String key = item.getMimeType().getType();
                content.addObject().put(key, value);
            }
        }
    }
}
