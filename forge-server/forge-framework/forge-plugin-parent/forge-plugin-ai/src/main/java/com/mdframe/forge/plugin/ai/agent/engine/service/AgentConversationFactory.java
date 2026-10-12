package com.mdframe.forge.plugin.ai.agent.engine.service;

import com.mdframe.forge.plugin.ai.agent.domain.AiAgent;
import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.plugin.ai.agent.engine.ReactRequest;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentSessionAccess;
import com.mdframe.forge.plugin.ai.agent.engine.context.AgentCallerGuard;
import com.mdframe.forge.plugin.ai.agent.engine.context.ContextTrimmer;
import com.mdframe.forge.plugin.ai.chat.domain.AiChatRecord;
import com.mdframe.forge.plugin.ai.chat.service.AiChatRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 先验证会话归属，再装配历史、附件和当前运行时；不在流式入口里堆跨域逻辑。 */
@Component
@RequiredArgsConstructor
public class AgentConversationFactory {
    private final AgentRuntimeFactory runtimeFactory;
    private final AiChatRecordService recordService;
    private final ContextTrimmer contextTrimmer;
    private final AgentSessionAccess access;

    public ReactContext seed(ReactRequest request) {
        access.validateSessionId(request.getSessionId());
        var user = AgentCallerGuard.requireCaller();
        ReactContext ctx = new ReactContext();
        ctx.setUserId(user.getUserId());
        ctx.setTenantId(user.getTenantId());
        ctx.setSessionId(request.getSessionId());
        return ctx;
    }

    /** 在 ReactAgent 占用会话后的回调内调用，竞争请求不能提前创建/更新会话。 */
    public void populate(ReactRequest request, ReactContext ctx) {
        AgentCallerGuard.requireOwner(ctx);
        ctx.verifyExecution();
        AiAgent agent = runtimeFactory.requireAgent(request.getAgentCode());
        access.prepare(request.getSessionId(), agent.getAgentCode(), request.getMessage());
        ctx.setAgentCode(agent.getAgentCode());
        ctx.setAgentId(agent.getId());
        ctx.setTenantId(agent.getTenantId());
        ctx.setMaxIters(agent.getMaxIters() != null ? agent.getMaxIters() : 10);
        ctx.setToolGroupMode(agent.getToolGroupMode() != null ? agent.getToolGroupMode() : "all");
        ctx.setSystemPrompt(agent.getSystemPrompt());
        ctx.verifyExecution();
        ctx.setUserMessage(resolveUserMessage(request));
        ctx.setHistory(resolveHistory(request, ctx));

        // 传递图片附件
        if (request.getImageFileIds() != null && !request.getImageFileIds().isEmpty()) {
            ctx.setImageFileIds(request.getImageFileIds());
        }

        runtimeFactory.attach(ctx, agent);

    }


    private List<org.springframework.ai.chat.messages.Message> resolveHistory(ReactRequest request, ReactContext ctx) {
        Long cutoffRecordId = resolveHistoryCutoffRecordId(request);
        int fetch = contextTrimmer.getMaxHistoryMessages();
        List<AiChatRecord> records = cutoffRecordId != null
                ? recordService.listRecentBySessionBeforeRecord(request.getSessionId(), cutoffRecordId, fetch)
                : recordService.listRecentBySession(request.getSessionId(), fetch);
        List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>(records.size());
        for (AiChatRecord record : records) {
            org.springframework.ai.chat.messages.Message message = toMessage(record);
            if (message != null) {
                messages.add(message);
            }
        }
        // 长会话上下文裁剪：按 token 预算保留最近若干条，避免撑爆模型上下文窗口。
        // system + user 先占预算，历史只能在会话归属验证之后读取。
        int reserved = contextTrimmer.estimateTokens(ctx.getSystemPrompt())
                + contextTrimmer.estimateTokens(ctx.getUserMessage());
        return contextTrimmer.trim(messages, reserved);
    }

    private Long resolveHistoryCutoffRecordId(ReactRequest request) {
        if (request.getEditUserRecordId() != null) {
            return request.getEditUserRecordId();
        }
        if (request.getRetryOfRecordId() != null) {
            AiChatRecord latestUser = recordService.findLatestUserBySession(request.getSessionId());
            return latestUser != null ? latestUser.getId() : request.getRetryOfRecordId();
        }
        return null;
    }

    private String resolveUserMessage(ReactRequest request) {
        if (request.getMessage() != null) {
            return request.getMessage();
        }
        if (request.getRetryOfRecordId() != null) {
            AiChatRecord latestUser = recordService.findLatestUserBySession(request.getSessionId());
            return latestUser != null ? latestUser.getContent() : null;
        }
        return null;
    }

    private org.springframework.ai.chat.messages.Message toMessage(AiChatRecord record) {
        if (record == null || record.getRole() == null) {
            return null;
        }
        return switch (record.getRole()) {
            case "system" -> new org.springframework.ai.chat.messages.SystemMessage(record.getContent());
            case "user" -> new org.springframework.ai.chat.messages.UserMessage(record.getContent());
            case "assistant" -> new org.springframework.ai.chat.messages.AssistantMessage(record.getContent());
            default -> null;
        };
    }
}
