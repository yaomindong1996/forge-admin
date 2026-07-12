package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.auth.config.FlowDelegationSessionVerifier;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowOverdueReminderService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowDelegatedIdentityControllerTest {

    @AfterEach
    void clearIdentity() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldStartAsTrustedSessionUserAndIgnoreBodyIdentity() {
        FlowInstanceService flowInstanceService = mock(FlowInstanceService.class);
        when(flowInstanceService.startProcess(
                any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("process-1");
        FlowDelegationSessionVerifier delegationVerifier = mock(FlowDelegationSessionVerifier.class);
        FlowInstanceController controller = new FlowInstanceController(
                flowInstanceService, mock(FlowBusinessMapper.class), mock(FlowOrgIntegrationService.class),
                delegationVerifier);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("businessKey", "order:1001");
        request.put("businessType", "order");
        request.put("title", "采购审批");
        request.put("variables", Map.of("amount", 100));
        request.put("userId", "999999");
        request.put("userName", "伪造用户");
        request.put("deptId", "888888");

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            controller.startDelegated("order_approval", request);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(flowInstanceService).startProcess(
                eq("order_approval"), eq("order:1001"), eq("order"), eq("采购审批"),
                variables.capture(), eq("101"), eq("用户A"), eq("201"), eq("研发一部"));
        verify(delegationVerifier).requireTrustedDelegation();
        assertThat(variables.getValue()).containsEntry("amount", 100)
                .doesNotContainKeys("userId", "userName", "deptId", "deptName");
    }

    @Test
    void shouldRejectTaskBodyIdentityThatDiffersFromTrustedSession() {
        FlowTaskService flowTaskService = mock(FlowTaskService.class);
        FlowTaskController controller = new FlowTaskController(
                flowTaskService, mock(FlowOverdueReminderService.class));

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            assertThatThrownBy(() -> controller.approve(Map.of(
                    "taskId", "task-1", "userId", "202", "tenantId", 1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("FLOW_TASK_ASSIGNEE_MISMATCH");
        }

        verify(flowTaskService, never()).approve(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldPassTrustedTenantAndUserToTaskService() {
        FlowTaskService flowTaskService = mock(FlowTaskService.class);
        FlowTaskController controller = new FlowTaskController(
                flowTaskService, mock(FlowOverdueReminderService.class));

        try (var ignored = ExecutionIdentityContextHolder.open(identity())) {
            controller.reject(Map.of(
                    "taskId", "task-1", "userId", "101", "tenantId", 1L,
                    "comment", "不同意", "idempotencyKey", "flow-action-key-1001",
                    "requestDigest", "sha256:digest"));
        }

        verify(flowTaskService).reject(
                "task-1", "101", "不同意", null, 1L,
                "flow-action-key-1001", "sha256:digest");
    }

    private ExecutionIdentity identity() {
        LoginUser user = new LoginUser();
        user.setUserId(101L);
        user.setRealName("用户A");
        user.setTenantId(1L);
        user.setActiveOrgId(201L);
        user.setActiveOrgName("研发一部");
        user.setPermissions(Set.of("flow:task:approve", "flow:task:reject"));
        return new ExecutionIdentity(user, "USER", 101L, 999L,
                301L, "agent-client", "token-1", Set.of("capability:invoke"));
    }

}
