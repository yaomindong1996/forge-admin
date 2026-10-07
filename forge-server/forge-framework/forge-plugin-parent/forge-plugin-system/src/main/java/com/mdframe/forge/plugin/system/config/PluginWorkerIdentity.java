package com.mdframe.forge.plugin.system.config;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;

import java.util.function.Supplier;

/** 只允许专用认证过滤器创建请求身份。 */
public record PluginWorkerIdentity(Long tenantId, String workerId) {
    static final String ATTRIBUTE = PluginWorkerIdentity.class.getName();

    public static PluginWorkerIdentity required(HttpServletRequest request) {
        if (request.getAttribute(ATTRIBUTE) instanceof PluginWorkerIdentity identity) {
            return identity;
        }
        throw new BusinessException(403, "执行器未认证");
    }

    public <T> T inTenant(Supplier<T> action) {
        Long previous = TenantContextHolder.getTenantId();
        boolean ignored = TenantContextHolder.isIgnore();
        try {
            // 不继承通用 API 配置的免租户标记；机器仅能访问部署方绑定的租户。
            TenantContextHolder.setIgnore(false);
            TenantContextHolder.setTenantId(tenantId);
            return action.get();
        } finally {
            TenantContextHolder.setIgnore(ignored);
            if (previous == null) {
                TenantContextHolder.clear();
                TenantContextHolder.setIgnore(ignored);
            } else {
                TenantContextHolder.setTenantId(previous);
            }
        }
    }
}
