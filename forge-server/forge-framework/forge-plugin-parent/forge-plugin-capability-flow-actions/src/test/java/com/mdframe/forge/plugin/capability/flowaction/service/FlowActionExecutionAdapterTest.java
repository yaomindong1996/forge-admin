package com.mdframe.forge.plugin.capability.flowaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.flowaction.source.FlowActionSourceService;
import com.mdframe.forge.plugin.capability.secureaction.catalog.SecureActionDescriptor;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowActionExecutionAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FlowActionSourceService sourceService = mock(FlowActionSourceService.class);
    private final BusinessFlowService flowService = mock(BusinessFlowService.class);
    private final FlowActionExecutionLogService logService = mock(FlowActionExecutionLogService.class);
    private final FlowActionExecutionAdapter adapter = new FlowActionExecutionAdapter(
            sourceService, flowService, logService);

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldIgnoreClientUserIdAndCompleteAsTrustedActor() {
        SecureActionDescriptor descriptor = descriptor("APPROVE");
        BusinessTaskFormContextVO context = taskContext();
        when(flowService.getActionableTaskFormContext(any())).thenReturn(context);
        when(flowService.completeBusinessTask(any())).thenReturn(runtime("已同意"));
        when(logService.requestDigest(eq(descriptor), anyMap())).thenReturn("sha256:digest");
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Map<String, Object>> action = invocation.getArgument(4);
            return action.get();
        }).when(logService).execute(eq(descriptor), any(), anyMap(), eq("req-1"), any());

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            Map<String, Object> result = adapter.execute(descriptor, input("task-1", "同意"), "req-1");
            assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
        }

        ArgumentCaptor<BusinessTaskActionDTO> captor = ArgumentCaptor.forClass(BusinessTaskActionDTO.class);
        verify(flowService).completeBusinessTask(captor.capture());
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getVariables()).isEmpty();
        assertThat(captor.getValue().getTaskId()).isEqualTo("task-1");
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("flow-action-key-1001");
        assertThat(captor.getValue().getRequestDigest()).isEqualTo("sha256:digest");
    }

    @Test
    void shouldStartThroughStableCapabilityBusinessKeyPath() {
        SecureActionDescriptor descriptor = descriptor("START");
        when(flowService.startDocumentFlowForCapability(any())).thenReturn(runtime("流程已发起"));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Map<String, Object>> action = invocation.getArgument(4);
            return action.get();
        }).when(logService).execute(eq(descriptor), any(), anyMap(), eq("req-1"), any());

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            Map<String, Object> result = adapter.execute(descriptor, Map.of(
                    "recordId", "1001",
                    "idempotencyKey", "flow-start-key-1001",
                    "arguments", Map.of()), "req-1");
            assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
        }

        ArgumentCaptor<BusinessFlowStartDTO> captor = ArgumentCaptor.forClass(BusinessFlowStartDTO.class);
        verify(flowService).startDocumentFlowForCapability(captor.capture());
        assertThat(captor.getValue().getObjectCode()).isEqualTo("order");
        assertThat(captor.getValue().getRecordId()).isEqualTo(1001L);
        assertThat(captor.getValue().getVariables()).isEmpty();
    }

    @Test
    void shouldUseDedicatedRecoveryPathOnlyWithMatchingLocalEvidence() {
        SecureActionDescriptor descriptor = descriptor("APPROVE");
        Map<String, Object> input = input("task-1", "同意");
        when(logService.isRecoverableRequest(eq(descriptor), any(), eq(input))).thenReturn(true);
        when(logService.requestDigest(eq(descriptor), eq(input))).thenReturn("sha256:digest");
        when(flowService.recoverCapabilityTaskAction(any())).thenReturn(runtime("已恢复"));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Map<String, Object>> action = invocation.getArgument(4);
            return action.get();
        }).when(logService).execute(eq(descriptor), any(), eq(input), eq("req-retry"), any());

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            Map<String, Object> result = adapter.execute(descriptor, input, "req-retry");
            assertThat(result.get("executeStatus")).isEqualTo("SUCCESS");
        }

        verify(flowService).recoverCapabilityTaskAction(any());
        verify(flowService, never()).completeBusinessTask(any());
        verify(flowService, never()).getActionableTaskFormContext(any());
    }

    @Test
    void shouldReuseCompletedResultWithoutRequiringTaskToRemainActionable() {
        SecureActionDescriptor descriptor = descriptor("APPROVE");
        Map<String, Object> input = input("task-1", "同意");
        when(logService.isRecoverableRequest(eq(descriptor), any(), eq(input))).thenReturn(true);
        when(logService.execute(eq(descriptor), any(), eq(input), eq("req-retry"), any()))
                .thenReturn(Map.of("executeStatus", "SUCCESS", "message", "已同意",
                        "correlationId", "req-1", "idempotentHit", true));

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            Map<String, Object> result = adapter.execute(descriptor, input, "req-retry");
            assertThat(result).containsEntry("idempotentHit", true);
        }

        verify(flowService, never()).getActionableTaskFormContext(any());
        verify(flowService, never()).completeBusinessTask(any());
        verify(flowService, never()).recoverCapabilityTaskAction(any());
    }

    @Test
    void shouldRejectTaskFromAnotherObjectBeforeExecution() {
        BusinessTaskFormContextVO context = taskContext();
        context.setObjectCode("other");
        when(flowService.getActionableTaskFormContext(any())).thenReturn(context);

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatThrownBy(() -> adapter.validate(descriptor("REJECT"), input("task-1", "不同意")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("FLOW_TASK_MISMATCH");
        }

        verify(flowService, never()).completeBusinessTask(any());
    }

    @Test
    void shouldRequireRejectComment() {
        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatThrownBy(() -> adapter.validate(descriptor("REJECT"), input("task-1", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("驳回意见");
        }
        verify(flowService, never()).getActionableTaskFormContext(any());
    }

    private Map<String, Object> input(String taskId, String comment) {
        java.util.LinkedHashMap<String, Object> arguments = new java.util.LinkedHashMap<>();
        arguments.put("taskId", taskId);
        if (comment != null) {
            arguments.put("comment", comment);
        }
        return Map.of("recordId", "1001", "idempotencyKey", "flow-action-key-1001",
                "arguments", arguments);
    }

    private SecureActionDescriptor descriptor(String operation) {
        try {
            var policy = objectMapper.readTree("""
                    {"bindingId":71,"flowModelKey":"order_approval","operation":"%s",
                     "publishedObjectVersion":3,"permission":"ai:businessFlow:view",
                     "confirmationMode":"MCP_ELICITATION","allowedOperations":["%s"]}
                    """.formatted(operation, operation));
            return new SecureActionDescriptor(
                    10L, "purchase.order.flow." + operation.toLowerCase(), operation + "采购单",
                    operation, "1.0.0", "FLOW_ACTION", "purchase/order/" + operation,
                    "3", "FLOW", "MEDIUM", "purchase", "order", operation, 3,
                    "ai:businessFlow:view", Set.of(), Set.of(), policy,
                    objectMapper.readTree("{\"type\":\"object\"}"),
                    objectMapper.readTree("{\"type\":\"object\"}"));
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private BusinessTaskFormContextVO taskContext() {
        BusinessTaskFormContextVO context = new BusinessTaskFormContextVO();
        context.setObjectCode("order");
        context.setRecordId(1001L);
        context.setProcessDefKey("order_approval:3:deployment");
        return context;
    }

    private BusinessFlowRuntimeVO runtime(String message) {
        BusinessFlowRuntimeVO runtime = new BusinessFlowRuntimeVO();
        runtime.setMessage(message);
        return runtime;
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setPermissions(Set.of("ai:capability:flow-action:invoke", "ai:businessFlow:view"));
        return new ExecutionIdentity(user, "USER", 101L, 999L,
                301L, "agent_client", "token-1", Set.of("capability:invoke"));
    }
}
