package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;

/** 身份只从可信登录上下文获取，不从 DTO 获取。 */
public record PluginTaskActor(Long tenantId, Long userId, Long deptId) {
    public static PluginTaskActor current() {
        SessionHelper.assertAdmin("只有平台超级管理员可以管理插件任务");
        Long tenant = SessionHelper.getTenantId();
        Long user = SessionHelper.getUserId();
        if (tenant == null || user == null) {
            throw new BusinessException(403, "缺少可信登录身份");
        }
        return new PluginTaskActor(tenant, user, SessionHelper.getActiveOrgId());
    }
}
