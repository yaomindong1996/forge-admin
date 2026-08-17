package com.mdframe.forge.starter.cache.managed;

import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheTransactionExecutorTest {

    private final CacheTransactionExecutor executor = new CacheTransactionExecutor();

    @AfterEach
    void cleanTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldRunImmediatelyWithoutTransaction() {
        AtomicInteger counter = new AtomicInteger();

        executor.afterCommit(counter::incrementAndGet);

        assertEquals(1, counter.get());
    }

    @Test
    void shouldRunOnlyAfterCommit() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        executor.afterCommit(counter::incrementAndGet);
        assertEquals(0, counter.get());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, counter.get());
    }

    @Test
    void shouldSkipActionWhenTransactionRollsBack() {
        AtomicInteger counter = new AtomicInteger();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        executor.afterCommit(counter::incrementAndGet);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertEquals(0, counter.get());
    }
}
