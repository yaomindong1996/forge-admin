package com.mdframe.forge.plugin.ai.agent.engine.hitl;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.MessageType;

/** Redis 只保存 v2 对话/位置数据，运行时模型、工具实例及权限从服务端重新装配。 */
@Data
public class AgentInterruptSnapshot {

    public static final int SCHEMA_VERSION = 2;
    private int version;
    private String interruptId;
    private String agentCode;
    private Long agentId;
    private Long tenantId;
    private Long userId;
    private String sessionId;
    private int turnIndex;
    private int maxIters;
    private String systemPrompt;
    private String userMessage;
    private List<Long> imageFileIds;
    private List<AgentMessageSnapshot> history;
    private List<AgentMessageSnapshot> toolMessages;
    private List<AgentMessageSnapshot.Call> pendingToolCalls;
    private int pendingToolIndex;

    public static AgentInterruptSnapshot from(ReactContext context, String interruptId) {
        AgentInterruptSnapshot snapshot = new AgentInterruptSnapshot();
        snapshot.setVersion(SCHEMA_VERSION);
        snapshot.setInterruptId(interruptId);
        snapshot.setAgentCode(context.getAgentCode());
        snapshot.setAgentId(context.getAgentId());
        snapshot.setTenantId(context.getTenantId());
        snapshot.setUserId(context.getUserId());
        snapshot.setSessionId(context.getSessionId());
        snapshot.setTurnIndex(context.getTurnIndex());
        snapshot.setMaxIters(context.getMaxIters());
        snapshot.setSystemPrompt(context.getSystemPrompt());
        snapshot.setUserMessage(context.getUserMessage());
        snapshot.setImageFileIds(context.getImageFileIds() == null
                ? List.of() : List.copyOf(context.getImageFileIds()));
        snapshot.setHistory(context.getHistory().stream().map(AgentMessageSnapshot::from).toList());
        snapshot.setToolMessages(context.getToolMessages().stream().map(AgentMessageSnapshot::from).toList());
        snapshot.setPendingToolCalls(context.getPendingToolCalls().stream()
                .map(AgentMessageSnapshot.Call::from).toList());
        snapshot.setPendingToolIndex(context.getPendingToolIndex());
        snapshot.validate();
        return snapshot;
    }

    public ReactContext restore() {
        validate();
        ReactContext context = new ReactContext();
        context.setAgentCode(agentCode);
        context.setAgentId(agentId);
        context.setTenantId(tenantId);
        context.setUserId(userId);
        context.setSessionId(sessionId);
        context.setTurnIndex(turnIndex);
        context.setMaxIters(maxIters);
        context.setSystemPrompt(systemPrompt);
        context.setUserMessage(userMessage);
        context.setImageFileIds(List.copyOf(imageFileIds));
        context.setHistory(new ArrayList<>(history.stream().map(AgentMessageSnapshot::restore).toList()));
        context.setToolMessages(new ArrayList<>(toolMessages.stream().map(AgentMessageSnapshot::restore).toList()));
        context.setPendingToolCalls(pendingToolCalls.stream().map(AgentMessageSnapshot.Call::restore).toList());
        context.setPendingToolIndex(pendingToolIndex);
        return context;
    }

    public void validate() {
        if (version != SCHEMA_VERSION || interruptId == null || agentCode == null || agentCode.isBlank()
                || sessionId == null || sessionId.isBlank()
                || agentId == null || tenantId == null || userId == null || agentId <= 0 || tenantId <= 0 || userId <= 0
                || turnIndex < 0 || turnIndex >= maxIters
                || history == null || toolMessages == null || imageFileIds == null
                || pendingToolCalls == null || pendingToolIndex < 0 || pendingToolIndex >= pendingToolCalls.size()) {
            throw new BusinessException("中断状态无效，请重新发起对话");
        }
        validatePendingTurn();
    }

    private void validatePendingTurn() {
        int declarationIndex = toolMessages.size() - pendingToolIndex - 1;
        if (declarationIndex < 0) {
            throw new BusinessException("中断工具位置无效");
        }
        AgentMessageSnapshot declaration = toolMessages.get(declarationIndex);
        if (declaration.role() != MessageType.ASSISTANT || !declaration.calls().equals(pendingToolCalls)
                || pendingToolCalls.stream().map(AgentMessageSnapshot.Call::id).distinct().count()
                != pendingToolCalls.size()) {
            throw new BusinessException("中断工具声明不匹配");
        }
        for (int index = 0; index < pendingToolIndex; index++) {
            var result = toolMessages.get(declarationIndex + index + 1);
            var call = pendingToolCalls.get(index);
            if (result.role() != MessageType.TOOL || result.responses().size() != 1
                    || !call.id().equals(result.responses().get(0).id())
                    || !call.name().equals(result.responses().get(0).name())) {
                throw new BusinessException("中断工具结果不匹配");
            }
        }
    }
}
