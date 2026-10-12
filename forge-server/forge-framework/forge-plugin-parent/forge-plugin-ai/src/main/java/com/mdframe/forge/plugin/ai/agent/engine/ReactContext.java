package com.mdframe.forge.plugin.ai.agent.engine;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.Data;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ReAct 循环上下文
 */
@Data
public class ReactContext {

    private String agentCode;
    private Long agentId;
    private Long tenantId;
    /** 发起用户ID（请求线程注入，供埋点归属；loop 线程无 Sa-Token 上下文） */
    private Long userId;
    private String sessionId;
    private int turnIndex;
    private int maxIters;
    private String toolGroupMode;

    private ChatModel chatModel;
    private AiAgent agent;

    private String systemPrompt;
    private String userMessage;

    /**
     * 图片附件（fileId列表），用于多模态消息
     */
    private List<Long> imageFileIds;

    /**
     * 文件访问URL构建函数（由外部注入，用于将fileId转为可访问URL）
     */
    private java.util.function.Function<Long, String> fileUrlResolver;

    /**
     * 对话历史
     */
    private List<Message> history = new ArrayList<>();

    /**
     * 取消标识
     */
    private volatile boolean cancelled;

    /** 失去分布式所有权与用户主动停止不同，旧运行不能再发布终止事件覆盖新运行。 */
    @JsonIgnore
    private volatile boolean executionLost;
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Runnable executionGuard = () -> { };

    /** 只在当前 loop 线程修改；人工恢复通过显式快照重建，不序列化 Spring AI 模型对象。 */
    private List<Message> toolMessages = new ArrayList<>();
    private List<AssistantMessage.ToolCall> pendingToolCalls = new ArrayList<>();
    private int pendingToolIndex;
    private String confirmedToolCallId;
    private final Sinks.Empty<Void> cancellation = Sinks.empty();

    /**
     * 本 Agent 运行时应向模型声明的工具列表（已解析为 AgentTool，去重）。
     * 由 AgentEngineService.buildContext 依据「工具绑定表 + 知识库绑定」解析注入；
     * 空列表表示本次不声明任何工具（普通对话 Agent，行为与历史一致）。
     * 工具 Prompt 工厂据此复制当前模型的 ToolCallingChatOptions。
     */
    private List<AgentTool> boundTools = new ArrayList<>();

    public void beginToolTurn(AssistantMessage assistant) {
        if (pendingToolCall() != null) {
            throw new BusinessException("仍有待处理工具，不能开始新模型轮次");
        }
        Set<String> ids = new HashSet<>();
        for (Message message : toolMessages) {
            if (message instanceof AssistantMessage prior) {
                prior.getToolCalls().forEach(call -> ids.add(call.id()));
            }
        }
        List<AssistantMessage.ToolCall> calls = assistant.getToolCalls();
        for (AssistantMessage.ToolCall call : calls) {
            if (call.id() == null || call.id().isBlank() || !ids.add(call.id())
                    || call.name() == null || call.name().isBlank() || !"function".equals(call.type())) {
                throw new BusinessException("模型返回无效或重复的工具调用");
            }
        }
        pendingToolCalls = List.copyOf(calls);
        pendingToolIndex = 0;
        toolMessages.add(assistant);
    }

    public AssistantMessage.ToolCall pendingToolCall() {
        return pendingToolIndex < pendingToolCalls.size() ? pendingToolCalls.get(pendingToolIndex) : null;
    }

    public void addToolResult(AssistantMessage.ToolCall call, String result) {
        if (pendingToolCall() == null || !pendingToolCall().id().equals(call.id())) {
            throw new BusinessException("工具结果与当前待执行调用不匹配");
        }
        var response = new ToolResponseMessage.ToolResponse(call.id(), call.name(), result);
        toolMessages.add(ToolResponseMessage.builder().responses(List.of(response)).build());
        pendingToolIndex++;
        confirmedToolCallId = null;
    }

    public void completeToolTurn() {
        if (pendingToolCall() != null) {
            throw new BusinessException("工具调用结果未完整返回");
        }
        pendingToolCalls = new ArrayList<>();
        pendingToolIndex = 0;
        turnIndex++;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if (cancelled) {
            cancellation.tryEmitEmpty();
        }
    }

    public Mono<Void> cancellationSignal() {
        return cancellation.asMono();
    }

    public void attachExecutionGuard(Runnable guard) {
        executionGuard = java.util.Objects.requireNonNull(guard);
    }

    public void verifyExecution() {
        executionGuard.run();
    }

    /**
     * 构建发送给 LLM 的消息列表
     */
    public List<Message> buildMessages() {
        List<Message> messages = new ArrayList<>();

        // 系统提示词
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 历史消息
        messages.addAll(history);

        // 用户消息（支持多模态）
        if (userMessage != null && !userMessage.isBlank()) {
            UserMessage userMsg = buildUserMessage(userMessage);
            messages.add(userMsg);
        }

        // assistant 工具声明与 tool 结果必须保留原调用 ID 和顺序，不能替换为普通文本。
        messages.addAll(toolMessages);

        return messages;
    }

    /**
     * 构建用户消息，如果存在图片附件则构造多模态消息
     */
    private UserMessage buildUserMessage(String text) {
        if (imageFileIds == null || imageFileIds.isEmpty() || fileUrlResolver == null) {
            return new UserMessage(text);
        }

        try {
            List<Media> mediaList = new ArrayList<>();
            for (Long fileId : imageFileIds) {
                String url = fileUrlResolver.apply(fileId);
                if (url != null && !url.isBlank()) {
                    mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(url)));
                }
            }
            if (!mediaList.isEmpty()) {
                return UserMessage.builder()
                        .text(text)
                        .media(mediaList)
                        .build();
            }
        } catch (Exception e) {
            // 多模态构造失败，降级为纯文本
        }
        return new UserMessage(text);
    }
}
