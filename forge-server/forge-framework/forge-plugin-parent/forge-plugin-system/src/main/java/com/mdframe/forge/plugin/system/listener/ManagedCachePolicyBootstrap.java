package com.mdframe.forge.plugin.system.listener;

import com.mdframe.forge.plugin.system.service.ISysManagedCachePolicyService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用就绪后将数据库权威策略恢复到 Redis 运行时控制面。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManagedCachePolicyBootstrap {

    private static final long PLATFORM_TENANT_ID = 1L;

    private final ISysManagedCachePolicyService cachePolicyService;

    @EventListener(ApplicationReadyEvent.class)
    public void synchronize() {
        try {
            TenantContextHolder.executeWithTenant(
                    PLATFORM_TENANT_ID, cachePolicyService::synchronizePolicies);
        } catch (RuntimeException exception) {
            log.warn("初始化受管缓存策略失败，运行时继续使用代码默认策略", exception);
        }
    }
}
