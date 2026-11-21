package com.mdframe.forge.admin.ai.hub;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.springframework.stereotype.Component;

/**
 * 全局 AI 助手身份与权限校验组件。
 */
@Component
class HubAccess {

    static final String PERM_HUB_USE = "ai:hub:use";
    static final String PERM_ENGINE_STREAM = "ai:engine:stream";
    static final String PERM_AGENT_LIST = "ai:agent:list";
    static final String PERM_TODO_LIST = "flow:task:todo:list";

    Actor current() {
        Long tenantId = SessionHelper.getTenantId();
        Long userId = SessionHelper.getUserId();
        if (tenantId == null || tenantId <= 0 || userId == null || userId <= 0) {
            throw new BusinessException(401, "请先登录后使用 AI 助手");
        }
        return new Actor(tenantId, userId, SessionHelper.getActiveOrgId());
    }

    record Actor(Long tenantId, Long userId, Long orgId) {
        boolean can(String permission) {
            return SessionHelper.hasPermission(permission);
        }

        void require(String permission) {
            if (!can(permission)) {
                throw new BusinessException(403, "当前账号缺少操作权限");
            }
        }
    }
}
