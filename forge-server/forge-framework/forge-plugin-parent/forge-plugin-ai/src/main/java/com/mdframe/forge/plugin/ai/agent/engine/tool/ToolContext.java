package com.mdframe.forge.plugin.ai.agent.engine.tool;

import lombok.Data;

import java.util.List;

/**
 * 工具执行上下文
 */
@Data
public class ToolContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 当前调用用户ID
     */
    private Long userId;

    /**
     * 当前轮次
     */
    private int turnIndex;

    /**
     * 当前 Agent 绑定的知识库ID列表（由 ReactLoop 从 agent.knowledgeIds 解析注入）。
     * RagSearchTool 在模型未显式指定 knowledge_id 时回退到此列表，实现「Agent 绑定的知识库」。
     */
    private List<Long> knowledgeIds;

    public static ToolContext of(String sessionId, Long agentId, Long tenantId, int turnIndex) {
        ToolContext ctx = new ToolContext();
        ctx.setSessionId(sessionId);
        ctx.setAgentId(agentId);
        ctx.setTenantId(tenantId);
        ctx.setTurnIndex(turnIndex);
        return ctx;
    }
}
