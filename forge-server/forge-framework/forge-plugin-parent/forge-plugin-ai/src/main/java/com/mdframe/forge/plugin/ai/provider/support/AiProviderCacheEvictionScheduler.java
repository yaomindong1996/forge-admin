package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.plugin.ai.client.ChatClientCache;
import com.mdframe.forge.plugin.ai.health.AiModelHealthRegistry;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 在供应商配置事务提交后清理对应的模型客户端缓存。
 */
@Component
@RequiredArgsConstructor
public class AiProviderCacheEvictionScheduler {

    private final ChatClientCache chatClientCache;
    private final AiModelHealthRegistry healthRegistry;

    public void scheduleAfterCommit(AiProvider provider) {
        if (provider == null || provider.getTenantId() == null || provider.getId() == null) {
            throw new BusinessException("AI 供应商缓存失效参数不完整");
        }
        Long tenantId = provider.getTenantId();
        Long providerId = provider.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                throw new BusinessException("AI供应商缓存失效事务同步未启用");
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictRuntimeState(tenantId, providerId);
                }
            });
            return;
        }
        evictRuntimeState(tenantId, providerId);
    }

    private void evictRuntimeState(Long tenantId, Long providerId) {
        chatClientCache.evictByProvider(tenantId, providerId);
        healthRegistry.resetProvider(tenantId, providerId);
    }
}
