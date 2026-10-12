package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.mdframe.forge.plugin.ai.session.domain.AiChatSession;
import com.mdframe.forge.plugin.ai.session.enums.AiChatSessionStatus;
import com.mdframe.forge.plugin.ai.session.service.AiChatSessionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 订阅和初始请求都从可信登录上下文检查已持久化的会话归属。 */
@Component
@RequiredArgsConstructor
public class AgentSessionAccess {
    private final AiChatSessionService sessions;

    public void prepare(String sessionId, String agentCode, String firstMessage) {
        validateSessionId(sessionId);
        var user = AgentCallerGuard.requireCaller();
        // 跨租户已有 ID 在租户查询下不可见；唯一主键冲突必须拒绝，不能先读取其历史。
        var session = sessions.getOrCreate(sessionId, user.getUserId(), user.getTenantId(), agentCode, firstMessage);
        requireOwner(session);
    }

    public SessionKey requireOwned(String sessionId) {
        validateSessionId(sessionId);
        var user = AgentCallerGuard.requireCaller();
        requireOwner(sessions.getById(sessionId));
        return new SessionKey(sessionId, user.getTenantId(), user.getUserId());
    }

    private void requireOwner(AiChatSession session) {
        var user = AgentCallerGuard.requireCaller();
        if (session == null || !AiChatSessionStatus.ACTIVE.matches(session.getStatus())
                || !Objects.equals(session.getTenantId(), user.getTenantId())
                || !Objects.equals(session.getUserId(), user.getUserId())) {
            throw new BusinessException(403, "无权访问该 Agent 会话或会话已删除");
        }
    }

    public void validateSessionId(String id) {
        if (id == null || id.isBlank() || id.length() > 64) {
            throw new BusinessException(400, "会话标识不能为空且不能超过 64 字符");
        }
    }

    public record SessionKey(String sessionId, Long tenantId, Long userId) { }
}
