package com.mdframe.forge.starter.flow.service.support;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowCleanupTransactionExecutorTest {

    @Test
    void eachExecutionShouldUseAnIndependentTransaction() {
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();
        FlowCleanupTransactionExecutor executor = new FlowCleanupTransactionExecutor(transactionManager);

        assertThat(executor.execute(() -> "first")).isEqualTo("first");
        assertThatThrownBy(() -> executor.execute(() -> {
            throw new IllegalStateException("failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(executor.execute(() -> "second")).isEqualTo("second");

        assertThat(transactionManager.propagationBehaviors)
                .containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
                .hasSize(3);
        assertThat(transactionManager.commits).isEqualTo(2);
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    void requiresNewShouldSuspendAndResumeAnExistingTransaction() {
        SuspendingTransactionManager transactionManager = new SuspendingTransactionManager();
        FlowCleanupTransactionExecutor executor = new FlowCleanupTransactionExecutor(transactionManager);
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

        outerTransaction.executeWithoutResult(status -> {
            int outerTransactionId = transactionManager.currentTransactionId();

            assertThat(executor.execute(transactionManager::currentTransactionId))
                    .isNotEqualTo(outerTransactionId);
            assertThat(transactionManager.currentTransactionId()).isEqualTo(outerTransactionId);
        });

        assertThat(transactionManager.suspends).isEqualTo(1);
        assertThat(transactionManager.resumes).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(2);
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final List<Integer> propagationBehaviors = new ArrayList<>();
        private int commits;
        private int rollbacks;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            propagationBehaviors.add(definition.getPropagationBehavior());
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            commits++;
        }

        @Override
        public void rollback(TransactionStatus status) {
            rollbacks++;
        }
    }

    private static final class SuspendingTransactionManager extends AbstractPlatformTransactionManager {

        private final ThreadLocal<TransactionContext> currentTransaction = new ThreadLocal<>();
        private int nextTransactionId;
        private int suspends;
        private int resumes;
        private int commits;

        @Override
        protected Object doGetTransaction() {
            return new TransactionObject(currentTransaction.get());
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((TransactionObject) transaction).context != null;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            TransactionContext context = new TransactionContext(++nextTransactionId);
            ((TransactionObject) transaction).context = context;
            currentTransaction.set(context);
        }

        @Override
        protected Object doSuspend(Object transaction) {
            TransactionContext suspended = currentTransaction.get();
            currentTransaction.remove();
            suspends++;
            return suspended;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            currentTransaction.set((TransactionContext) suspendedResources);
            resumes++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
            currentTransaction.remove();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            currentTransaction.remove();
        }

        private int currentTransactionId() {
            return currentTransaction.get().id;
        }

        private static final class TransactionObject {

            private TransactionContext context;

            private TransactionObject(TransactionContext context) {
                this.context = context;
            }
        }

        private record TransactionContext(int id) {
        }
    }
}
