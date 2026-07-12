package com.mdframe.forge.plugin.capability.highrisk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClient;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityClient;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityClientMapper;
import com.mdframe.forge.plugin.capability.highrisk.crypto.CapabilityPayloadCrypto;
import com.mdframe.forge.plugin.capability.highrisk.crypto.EncryptedCapabilityPayload;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HighRiskApprovalSubmissionServiceTest {

    private final CapabilityApprovalMapper approvalMapper = mock(CapabilityApprovalMapper.class);
    private final CapabilityPolicyService policyService = mock(CapabilityPolicyService.class);
    private final AiCapabilityClientMapper clientMapper = mock(AiCapabilityClientMapper.class);
    private final CapabilityPayloadCrypto crypto = mock(CapabilityPayloadCrypto.class);
    private final FlowClient flowClient = mock(FlowClient.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final HighRiskBusinessStateService stateService = mock(HighRiskBusinessStateService.class);
    private final HighRiskApprovalSubmissionService service = new HighRiskApprovalSubmissionService(
            approvalMapper, policyService, clientMapper, crypto, flowClient,
            new ObjectMapper(), transactionManager, stateService);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(policyService.requireActive(1L, 100L, "1.0.0")).thenReturn(policy());
        when(clientMapper.selectTenantById(1L, 30L)).thenReturn(client());
        when(stateService.snapshot(any(), any())).thenReturn("sha256:business");
        when(crypto.encrypt(any(), any())).thenReturn(
                new EncryptedCapabilityPayload("v1", "dek", "iv", "cipher", "tag"));
        when(flowClient.startHighRiskApprovalForDelegatedUser(
                eq("forge_capability_high_risk_approval"), anyString(), anyString(), any()))
                .thenReturn(FlowResult.success("process-1"));
        when(approvalMapper.updateState(any())).thenReturn(1);
    }

    @Test
    void shouldReserveEncryptedPayloadAndReturnPendingWithoutBusinessExecution() {
        Map<String, Object> result = service.submit(
                descriptor(), identity(), input("CONFIRMED"), "request-1");

        assertThat(result).containsEntry("executeStatus", "PENDING_APPROVAL")
                .containsKey("approvalRequestId")
                .containsEntry("idempotentHit", false);
        ArgumentCaptor<AiCapabilityApproval> captor = ArgumentCaptor.forClass(AiCapabilityApproval.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayloadCiphertext()).isEqualTo("cipher");
        assertThat(captor.getValue().getProcessInstanceId()).isEqualTo("process-1");
        verify(flowClient).startHighRiskApprovalForDelegatedUser(
                eq("forge_capability_high_risk_approval"),
                startsWith("capability-approval:"), contains("确认订单"), any());
    }

    @Test
    void shouldReuseSameApprovalForSameIdempotentRequest() {
        ArgumentCaptor<AiCapabilityApproval> captor = ArgumentCaptor.forClass(AiCapabilityApproval.class);
        service.submit(descriptor(), identity(), input("CONFIRMED"), "request-1");
        verify(approvalMapper).insert(captor.capture());
        AiCapabilityApproval existing = captor.getValue();
        existing.setExecuteStatus("PENDING_APPROVAL");
        when(approvalMapper.selectByIdempotency(1L, 30L, 100L, "order-confirm-1001"))
                .thenReturn(existing);

        Map<String, Object> reused = service.submit(
                descriptor(), identity(), input("CONFIRMED"), "request-2");

        assertThat(reused).containsEntry("idempotentHit", true)
                .containsEntry("approvalRequestId", String.valueOf(existing.getId()));
        verify(approvalMapper, times(1)).insert(any(AiCapabilityApproval.class));
        verify(flowClient, times(1)).startHighRiskApprovalForDelegatedUser(
                anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldRejectDifferentDigestForSameIdempotencyKey() {
        ArgumentCaptor<AiCapabilityApproval> captor = ArgumentCaptor.forClass(AiCapabilityApproval.class);
        service.submit(descriptor(), identity(), input("CONFIRMED"), "request-1");
        verify(approvalMapper).insert(captor.capture());
        AiCapabilityApproval existing = captor.getValue();
        existing.setExecuteStatus("PENDING_APPROVAL");
        when(approvalMapper.selectByIdempotency(1L, 30L, 100L, "order-confirm-1001"))
                .thenReturn(existing);

        assertThatThrownBy(() -> service.submit(
                descriptor(), identity(), input("CANCELLED"), "request-2"))
                .hasMessage("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldNotReuseApprovalOwnedByAnotherDelegatedUser() {
        ArgumentCaptor<AiCapabilityApproval> captor = ArgumentCaptor.forClass(AiCapabilityApproval.class);
        service.submit(descriptor(), identity(), input("CONFIRMED"), "request-1");
        verify(approvalMapper).insert(captor.capture());
        AiCapabilityApproval existing = captor.getValue();
        existing.setExecuteStatus("PENDING_APPROVAL");
        existing.setActorUserId(11L);
        when(approvalMapper.selectByIdempotency(1L, 30L, 100L, "order-confirm-1001"))
                .thenReturn(existing);

        assertThatThrownBy(() -> service.submit(
                descriptor(), identity(), input("CONFIRMED"), "request-2"))
                .hasMessage("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldLeaveReservationRecoverableWhenFlowStartFails() {
        when(flowClient.startHighRiskApprovalForDelegatedUser(anyString(), anyString(), anyString(), any()))
                .thenReturn(FlowResult.error("unavailable"));

        assertThatThrownBy(() -> service.submit(
                descriptor(), identity(), input("CONFIRMED"), "request-1"))
                .hasMessage("APPROVAL_FLOW_UNAVAILABLE");
        ArgumentCaptor<AiCapabilityApproval> captor = ArgumentCaptor.forClass(AiCapabilityApproval.class);
        verify(approvalMapper).insert(captor.capture());
        assertThat(captor.getValue().getExecuteStatus()).isEqualTo("RESERVED");
        verify(approvalMapper, never()).updateState(any());
    }

    @Test
    void shouldConvergeTwentyConcurrentRequestsToOneApprovalAndOneFlowInstance() throws Exception {
        AtomicReference<AiCapabilityApproval> stored = new AtomicReference<>();
        when(approvalMapper.selectByIdempotency(1L, 30L, 100L, "order-confirm-1001"))
                .thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            AiCapabilityApproval candidate = invocation.getArgument(0);
            if (!stored.compareAndSet(null, candidate)) {
                throw new DuplicateKeyException("duplicate");
            }
            return 1;
        }).when(approvalMapper).insert(any(AiCapabilityApproval.class));
        AtomicInteger createdFlowInstances = new AtomicInteger();
        AtomicReference<String> flowBusinessKey = new AtomicReference<>();
        when(flowClient.startHighRiskApprovalForDelegatedUser(
                anyString(), anyString(), anyString(), any())).thenAnswer(invocation -> {
            String businessKey = invocation.getArgument(1);
            if (flowBusinessKey.compareAndSet(null, businessKey)) {
                createdFlowInstances.incrementAndGet();
            }
            return FlowResult.success("process-1");
        });
        int concurrency = 20;
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(concurrency);
        try {
            List<java.util.concurrent.Future<Map<String, Object>>> futures =
                    java.util.stream.IntStream.range(0, concurrency).mapToObj(index ->
                            executor.submit(() -> {
                                ready.countDown();
                                start.await();
                                return service.submit(descriptor(), identity(),
                                        input("CONFIRMED"), "request-" + index);
                            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<String> approvalIds = new java.util.ArrayList<>();
            for (var future : futures) {
                approvalIds.add(String.valueOf(future.get(10, TimeUnit.SECONDS)
                        .get("approvalRequestId")));
            }
            assertThat(approvalIds).containsOnly(String.valueOf(stored.get().getId()));
            assertThat(createdFlowInstances).hasValue(1);
        }
        finally {
            executor.shutdownNow();
        }
    }

    private Map<String, Object> input(String status) {
        return Map.of("recordId", "1001", "idempotencyKey", "order-confirm-1001",
                "arguments", Map.of("status", status));
    }

    private AiCapabilityPolicy policy() {
        AiCapabilityPolicy policy = new AiCapabilityPolicy();
        policy.setApprovalFlowModelKey("forge_capability_high_risk_approval");
        policy.setApprovalCandidateGroup("risk-approver");
        policy.setExpireSeconds(3600);
        return policy;
    }

    private AiCapabilityClient client() {
        AiCapabilityClient client = new AiCapabilityClient();
        client.setId(30L);
        client.setStatus("ENABLED");
        client.setCredentialVersion(2);
        client.setServiceUserId(20L);
        client.setActiveOrgId(40L);
        return client;
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(10L);
        user.setTenantId(1L);
        user.setActiveOrgId(40L);
        return new ExecutionIdentity(user, "USER", 10L, 20L, 30L,
                "client-a", "token-1", Set.of("capability:invoke:business.order.confirm"));
    }

    private SecureActionDescriptor descriptor() {
        ObjectMapper mapper = new ObjectMapper();
        return new SecureActionDescriptor(100L, "business.order.confirm", "确认订单",
                "确认订单", "1.0.0", "BUSINESS_ACTION", "purchase/order/confirm", "3",
                "ACTION", "HIGH", "purchase", "order", "confirm", 3,
                "purchase:order:confirm", Set.of("status"), Set.of("status"),
                mapper.createObjectNode(), mapper.createObjectNode(), mapper.createObjectNode());
    }
}
