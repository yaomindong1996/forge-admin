package com.mdframe.forge.plugin.ai.agent.engine.context;

import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.context.LoginUserSnapshot;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.tenant.datasource.TenantBusinessDataSourceTaskDecorator;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 复用租户/业务库/机器身份装饰器，并补普通登录用户与 MDC；不保留 HTTP 请求或 Token。 */
@Component
public class AgentExecutionTaskDecorator implements TaskDecorator {

    private final TenantBusinessDataSourceTaskDecorator tenantDecorator = new TenantBusinessDataSourceTaskDecorator();

    @Override
    public Runnable decorate(Runnable task) {
        var user = LoginUserSnapshot.copy(AgentCallerGuard.requireCaller());
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        Runnable scoped = () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try (var ignored = LoginUserContextHolder.open(user)) {
                // Agent 工具只在已验证的当前租户执行，不能继承框架内部临时 ignoreTenant。
                TenantContextHolder.setTenantId(user.getTenantId());
                TenantContextHolder.setIgnore(false);
                restoreMdc(mdc);
                task.run();
            } finally {
                restoreMdc(previousMdc);
            }
        };
        return tenantDecorator.decorate(scoped);
    }

    private void restoreMdc(Map<String, String> values) {
        if (values == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(values);
        }
    }
}
