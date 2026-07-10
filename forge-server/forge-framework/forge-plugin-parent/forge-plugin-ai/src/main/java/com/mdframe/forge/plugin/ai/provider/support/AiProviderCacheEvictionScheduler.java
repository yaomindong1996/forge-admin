package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.plugin.ai.client.ChatClientCache;
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

    public void scheduleAfterCommit(AiProvider provider) {
        if (provider == null || provider.getTenantId() == null || provider.getId() == null) {
            throw new BusinessException("AI 供应商缓存失效参数不完整");
        }
        Long tenantId = provider.getTenantId();
        Long providerId = provider.getId();
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    chatClientCache.evictByProvider(tenantId, providerId);
                }
            });
            return;
        }
        chatClientCache.evictByProvider(tenantId, providerId);
    }
}
