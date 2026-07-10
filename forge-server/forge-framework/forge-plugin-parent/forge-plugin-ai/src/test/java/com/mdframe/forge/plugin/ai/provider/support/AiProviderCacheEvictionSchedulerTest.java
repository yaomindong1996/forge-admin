package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.plugin.ai.client.ChatClientCache;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiProviderCacheEvictionSchedulerTest {

    @Mock
    private ChatClientCache cache;

    @AfterEach
    void cleanTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void activeTransactionShouldEvictOnlyAfterCommit() {
        beginTransactionSynchronization();
        AiProviderCacheEvictionScheduler scheduler = new AiProviderCacheEvictionScheduler(cache);

        scheduler.scheduleAfterCommit(provider());

        verify(cache, never()).evictByProvider(1L, 10L);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(cache).evictByProvider(1L, 10L);
    }

    @Test
    void rollbackShouldNotEvict() {
        beginTransactionSynchronization();
        AiProviderCacheEvictionScheduler scheduler = new AiProviderCacheEvictionScheduler(cache);

        scheduler.scheduleAfterCommit(provider());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(cache, never()).evictByProvider(1L, 10L);
    }

    @Test
    void noTransactionShouldEvictImmediately() {
        AiProviderCacheEvictionScheduler scheduler = new AiProviderCacheEvictionScheduler(cache);

        scheduler.scheduleAfterCommit(provider());

        verify(cache).evictByProvider(1L, 10L);
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private AiProvider provider() {
        AiProvider provider = new AiProvider();
        provider.setTenantId(1L);
        provider.setId(10L);
        return provider;
    }
}
