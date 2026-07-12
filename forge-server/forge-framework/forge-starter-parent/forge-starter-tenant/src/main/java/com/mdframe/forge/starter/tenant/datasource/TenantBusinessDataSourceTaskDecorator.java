package com.mdframe.forge.starter.tenant.datasource;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * 将租户和业务数据源上下文传递到异步任务线程。
 */
@Component
@ConditionalOnProperty(prefix = "forge.business.datasource", name = "enabled", havingValue = "true")
public class TenantBusinessDataSourceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long tenantId = TenantContextHolder.getTenantId();
        boolean ignoreTenant = TenantContextHolder.isIgnore();
        String dsKey = DynamicDataSourceContextHolder.peek();
        ExecutionIdentity executionIdentity = ExecutionIdentityContextHolder.current().orElse(null);
        return () -> {
            Long previousTenantId = TenantContextHolder.getTenantId();
            boolean previousIgnoreTenant = TenantContextHolder.isIgnore();
            String previousDsKey = DynamicDataSourceContextHolder.peek();
            ExecutionIdentityContextHolder.Scope identityScope = executionIdentity == null
                    ? ExecutionIdentityContextHolder.suspend()
                    : ExecutionIdentityContextHolder.open(executionIdentity);
            applyTenantContext(tenantId, ignoreTenant);
            boolean pushed = StringUtils.isNotBlank(dsKey);
            if (pushed) {
                DynamicDataSourceContextHolder.push(dsKey);
            } else if (StringUtils.isNotBlank(previousDsKey)) {
                DynamicDataSourceContextHolder.clear();
            }
            try {
                runnable.run();
            } finally {
                identityScope.close();
                if (pushed) {
                    DynamicDataSourceContextHolder.poll();
                } else if (StringUtils.isNotBlank(previousDsKey)) {
                    DynamicDataSourceContextHolder.push(previousDsKey);
                }
                applyTenantContext(previousTenantId, previousIgnoreTenant);
            }
        };
    }

    private void applyTenantContext(Long tenantId, boolean ignoreTenant) {
        TenantContextHolder.clear();
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }
        if (ignoreTenant) {
            TenantContextHolder.setIgnore(true);
        }
    }
}
