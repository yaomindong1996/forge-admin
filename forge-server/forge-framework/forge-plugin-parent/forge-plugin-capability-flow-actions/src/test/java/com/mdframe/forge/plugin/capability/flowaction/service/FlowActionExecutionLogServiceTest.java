package com.mdframe.forge.plugin.capability.flowaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.flowaction.domain.AiCapabilityFlowActionLog;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionExecutionLogMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowActionExecutionLogServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlowActionExecutionLogMapper mapper = mock(FlowActionExecutionLogMapper.class);
    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
    private final FlowActionExecutionLogService service = new FlowActionExecutionLogService(
            mapper, objectMapper, transactionManager);

    @Test
    void shouldReserveBeforeSideEffectAndPersistSafeResult() {
        when(mapper.updateResultByIdentity(any())).thenReturn(1);
        AtomicInteger sideEffects = new AtomicInteger();

        Map<String, Object> result = service.execute(
                descriptor(), identity(), input("同意"), "req-1", () -> {
                    sideEffects.incrementAndGet();
                    return Map.of("executeStatus", "SUCCESS", "message", "完成",
                            "correlationId", "req-1", "idempotentHit", false);
                });

        assertThat(sideEffects).hasValue(1);
        assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
        verify(mapper).insert(any(AiCapabilityFlowActionLog.class));
        verify(mapper).updateResultByIdentity(any(AiCapabilityFlowActionLog.class));
    }

    @Test
    void shouldRejectSameKeyWithDifferentRequestDigest() {
        AiCapabilityFlowActionLog existing = new AiCapabilityFlowActionLog();
        existing.setRequestDigest("sha256:other");
        existing.setExecuteStatus("SUCCESS");
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(existing);

        assertThatThrownBy(() -> service.execute(
                descriptor(), identity(), input("同意"), "req-1", Map::of))
                .isInstanceOf(BusinessException.class)
                .hasMessage("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldNotRunSideEffectWhenConcurrentReservationAlreadyExists() {
        Map<String, Object> input = input("同意");
        AiCapabilityFlowActionLog existing = new AiCapabilityFlowActionLog();
        existing.setRequestDigest(service.requestDigest(descriptor(), input));
        existing.setExecuteStatus("RUNNING");
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(null, existing);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(mapper).insert(any(AiCapabilityFlowActionLog.class));
        AtomicInteger sideEffects = new AtomicInteger();

        assertThatThrownBy(() -> service.execute(
                descriptor(), identity(), input, "req-1", () -> {
                    sideEffects.incrementAndGet();
                    return Map.of();
                }))
                .isInstanceOf(BusinessException.class)
                .hasMessage("IDEMPOTENCY_CONFLICT");

        assertThat(sideEffects).hasValue(0);
        verify(mapper, never()).updateResultByIdentity(any());
    }

    @Test
    void shouldRecoverFailedRemoteActionWithSameCanonicalRequest() {
        Map<String, Object> input = input("同意");
        AiCapabilityFlowActionLog existing = recoverableLog(input, "FAILED");
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(existing);
        when(mapper.updateResultByIdentity(any())).thenReturn(1);
        AtomicInteger remoteCalls = new AtomicInteger();

        Map<String, Object> result = service.execute(
                descriptor(), identity(), input, "req-retry", () -> {
                    remoteCalls.incrementAndGet();
                    return Map.of("executeStatus", "SUCCESS", "message", "已恢复",
                            "correlationId", "req-retry", "idempotentHit", false);
                });

        assertThat(remoteCalls).hasValue(1);
        assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
        verify(mapper, org.mockito.Mockito.times(2)).updateResultByIdentity(existing);
    }

    @Test
    void shouldRecoverStaleRunningReservationButNotActiveOne() {
        Map<String, Object> input = input("同意");
        AiCapabilityFlowActionLog stale = recoverableLog(input, "RUNNING");
        stale.setUpdateTime(LocalDateTime.now().minusMinutes(1));
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(stale);
        when(mapper.updateResultByIdentity(any())).thenReturn(1);

        Map<String, Object> result = service.execute(
                descriptor(), identity(), input, "req-retry", () -> Map.of(
                        "executeStatus", "SUCCESS", "message", "已恢复",
                        "correlationId", "req-retry", "idempotentHit", false));

        assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
    }

    @Test
    void shouldRecognizeCompletedReplayBeforeActionableTaskPrecheck() throws Exception {
        Map<String, Object> input = input("同意");
        AiCapabilityFlowActionLog existing = recoverableLog(input, "SUCCESS");
        existing.setResultSnapshot(objectMapper.writeValueAsString(Map.of(
                "executeStatus", "SUCCESS", "message", "已完成",
                "correlationId", "req-1", "idempotentHit", false)));
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(existing);

        assertThat(service.isRecoverableRequest(descriptor(), identity(), input)).isTrue();
    }

    @Test
    void shouldRejectActiveRunningReplayBeforeActionableTaskPrecheck() {
        Map<String, Object> input = input("同意");
        AiCapabilityFlowActionLog existing = recoverableLog(input, "RUNNING");
        existing.setUpdateTime(LocalDateTime.now());
        when(mapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenReturn(existing);

        assertThatThrownBy(() -> service.isRecoverableRequest(descriptor(), identity(), input))
                .isInstanceOf(BusinessException.class)
                .hasMessage("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldRollbackActionTransactionAndPersistFailureWhenSuccessAuditUpdateFails() {
        when(mapper.updateResultByIdentity(any()))
                .thenThrow(new IllegalStateException("audit update failed"))
                .thenReturn(1);
        AtomicInteger sideEffects = new AtomicInteger();

        assertThatThrownBy(() -> service.execute(
                descriptor(), identity(), input("同意"), "req-1", () -> {
                    sideEffects.incrementAndGet();
                    return Map.of("executeStatus", "SUCCESS", "message", "完成");
                }))
                .isInstanceOf(com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException.class)
                .hasMessage("FLOW_AUDIT_UNAVAILABLE");

        assertThat(sideEffects).hasValue(1);
        assertThat(transactionManager.rollbacks).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(2);
        verify(mapper, org.mockito.Mockito.times(2)).updateResultByIdentity(any());
    }

    @Test
    void shouldExecuteAtMostOnceForTwentyConcurrentRequests() throws Exception {
        FlowActionExecutionLogMapper concurrentMapper = mock(FlowActionExecutionLogMapper.class);
        AtomicReference<AiCapabilityFlowActionLog> persisted = new AtomicReference<>();
        when(concurrentMapper.selectByIdempotency(1L, 301L, 10L, "APPROVE", "flow-action-key-1001"))
                .thenAnswer(invocation -> persisted.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            AiCapabilityFlowActionLog candidate = invocation.getArgument(0);
            if (!persisted.compareAndSet(null, snapshot(candidate))) {
                throw new DuplicateKeyException("duplicate");
            }
            return 1;
        }).when(concurrentMapper).insert(any(AiCapabilityFlowActionLog.class));
        when(concurrentMapper.updateResultByIdentity(any())).thenAnswer(invocation -> {
            persisted.set(snapshot(invocation.getArgument(0)));
            return 1;
        });
        FlowActionExecutionLogService concurrentService = new FlowActionExecutionLogService(
                concurrentMapper, objectMapper, new RecordingTransactionManager());
        AtomicInteger sideEffects = new AtomicInteger();
        CountDownLatch actionStarted = new CountDownLatch(1);
        CountDownLatch actionRelease = new CountDownLatch(1);
        CyclicBarrier start = new CyclicBarrier(20);
        var executor = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < 20; index++) {
                String requestId = "req-" + index;
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        concurrentService.execute(
                                descriptor(), identity(), input("同意"), requestId, () -> {
                                    sideEffects.incrementAndGet();
                                    actionStarted.countDown();
                                    try {
                                        actionRelease.await();
                                    }
                                    catch (InterruptedException exception) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException(exception);
                                    }
                                    return Map.of("executeStatus", "SUCCESS", "message", "完成");
                                });
                    }
                    catch (BusinessException ignored) {
                        // RUNNING 并发冲突符合幂等契约。
                    }
                    return null;
                }));
            }
            assertThat(actionStarted.await(5, TimeUnit.SECONDS)).isTrue();
            actionRelease.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }
        finally {
            actionRelease.countDown();
            executor.shutdownNow();
        }

        assertThat(sideEffects).hasValue(1);
    }

    private static AiCapabilityFlowActionLog snapshot(AiCapabilityFlowActionLog source) {
        AiCapabilityFlowActionLog target = new AiCapabilityFlowActionLog();
        target.setRequestDigest(source.getRequestDigest());
        target.setExecuteStatus(source.getExecuteStatus());
        target.setResultSnapshot(source.getResultSnapshot());
        return target;
    }

    private AiCapabilityFlowActionLog recoverableLog(Map<String, Object> input, String status) {
        AiCapabilityFlowActionLog log = new AiCapabilityFlowActionLog();
        log.setId(100L);
        log.setTenantId(1L);
        log.setRequestId("req-1");
        log.setClientId(301L);
        log.setCapabilityId(10L);
        log.setOperation("APPROVE");
        log.setActorType("USER");
        log.setActorUserId(101L);
        log.setServiceUserId(999L);
        log.setActiveOrgId(201L);
        log.setRequestDigest(service.requestDigest(descriptor(), input));
        log.setExecuteStatus(status);
        log.setCreateTime(LocalDateTime.now().minusMinutes(1));
        return log;
    }

    private Map<String, Object> input(String comment) {
        return Map.of("recordId", "1001", "idempotencyKey", "flow-action-key-1001",
                "arguments", Map.of("taskId", "task-1", "comment", comment));
    }

    private SecureActionDescriptor descriptor() {
        return new SecureActionDescriptor(10L, "purchase.order.flow.approve", "同意采购单",
                "同意", "1.0.0", "FLOW_ACTION", "purchase/order/APPROVE", "3", "FLOW", "MEDIUM",
                "purchase", "order", "APPROVE", 3, "ai:businessFlow:view",
                Set.of(), Set.of(), objectMapper.createObjectNode(),
                objectMapper.createObjectNode(), objectMapper.createObjectNode());
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        return new ExecutionIdentity(user, "USER", 101L, 999L,
                301L, "agent_client", "token-1", Set.of("capability:invoke"));
    }

    private static final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

        private int commits;
        private int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
