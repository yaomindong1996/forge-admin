package com.mdframe.forge.plugin.capability.identity.security;

import com.mdframe.forge.starter.datascope.context.DataScopeContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 在 Capability 协议完成凭据校验后，以可信租户身份执行租户表访问。
 */
public final class CapabilityTenantContext {

    private CapabilityTenantContext() {
    }

    public static <T> T execute(Long tenantId, Supplier<T> action) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Capability 租户身份无效");
        }
        Objects.requireNonNull(action, "action 不能为空");
        Long previousTenantId = TenantContextHolder.getTenantId();
        Boolean previousIgnore = TenantContextHolder.getIgnoreValue();
        boolean previousDataScopeSkip = DataScopeContextHolder.isSkip();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            DataScopeContextHolder.skipDataScope();
            return action.get();
        } finally {
            restore(previousTenantId, previousIgnore, previousDataScopeSkip);
        }
    }

    public static void execute(Long tenantId, Runnable action) {
        Objects.requireNonNull(action, "action 不能为空");
        execute(tenantId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 执行尚未解析出租户的全局凭据查询；只跳过用户数据权限，不跳过调用方显式安全条件。
     */
    public static <T> T executeCredentialLookup(Supplier<T> action) {
        Objects.requireNonNull(action, "action 不能为空");
        boolean previousDataScopeSkip = DataScopeContextHolder.isSkip();
        try {
            DataScopeContextHolder.skipDataScope();
            return action.get();
        } finally {
            restoreDataScope(previousDataScopeSkip);
        }
    }

    private static void restore(
            Long previousTenantId,
            Boolean previousIgnore,
            boolean previousDataScopeSkip) {
        TenantContextHolder.clear();
        if (previousTenantId != null) {
            TenantContextHolder.setTenantId(previousTenantId);
        }
        if (previousIgnore != null) {
            TenantContextHolder.setIgnore(previousIgnore);
        }
        restoreDataScope(previousDataScopeSkip);
    }

    private static void restoreDataScope(boolean previousDataScopeSkip) {
        if (previousDataScopeSkip) {
            DataScopeContextHolder.skipDataScope();
        } else {
            DataScopeContextHolder.clearSkip();
        }
    }
}
