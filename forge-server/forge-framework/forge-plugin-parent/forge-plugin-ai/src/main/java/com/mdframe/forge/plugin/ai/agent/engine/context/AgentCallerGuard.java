package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.mdframe.forge.plugin.ai.agent.engine.ReactContext;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;

import java.util.Objects;

/** 请求线程只使用认证上下文，不能用恢复请求中的 userId/tenantId 授权。 */
public final class AgentCallerGuard {

    private AgentCallerGuard() {
    }

    public static LoginUser requireCaller() {
        LoginUser user;
        try {
            user = SessionHelper.getLoginUser();
        } catch (RuntimeException ex) {
            throw new BusinessException(401, "请登录后执行 Agent");
        }
        if (user == null || user.getUserId() == null || user.getUserId() <= 0
                || user.getTenantId() == null || user.getTenantId() <= 0) {
            throw new BusinessException(401, "请登录后执行 Agent");
        }
        return user;
    }

    public static void requireOwner(ReactContext context) {
        LoginUser user = requireCaller();
        if (!Objects.equals(user.getUserId(), context.getUserId())
                || !Objects.equals(user.getTenantId(), context.getTenantId())) {
            throw new BusinessException(403, "无权访问该 Agent 会话");
        }
    }
}
