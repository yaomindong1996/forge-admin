package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowTask;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowTaskActionAuthorizationTest {

    @Test
    void shouldAuthorizeClaimedTaskForSameTenantAndAssignee() {
        assertThat(FlowTaskActionAuthorization.authorize(
                task(1L, "101", 1), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", 2)).isFalse();
    }

    @Test
    void shouldKeepDirectUiCompatibilityForAutoAssignedPendingTask() {
        assertThat(FlowTaskActionAuthorization.authorize(
                task(1L, "101", 0), "101", 1L,
                "APPROVE", null, null, 2)).isFalse();
    }

    @Test
    void shouldRejectCrossTenantTask() {
        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task(2L, "101", 1), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", 2))
                .hasMessage("FLOW_TASK_TENANT_MISMATCH");
    }

    @Test
    void shouldRejectTaskTransferredAfterConfirmation() {
        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task(1L, "202", 1), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", 2))
                .hasMessage("FLOW_TASK_ASSIGNEE_MISMATCH");
    }

    @Test
    void shouldRejectCompletedTaskWithDifferentRequest() {
        FlowTask task = task(1L, "101", 2);
        task.setActionIdempotencyKey("key-old");
        task.setActionRequestDigest("sha256:old");
        task.setActionType("APPROVE");

        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task, "101", 1L, "APPROVE", "key-new", "sha256:new", 2))
                .hasMessage("FLOW_TASK_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void shouldReturnIdempotentSuccessForSameCompletedRequest() {
        FlowTask task = task(1L, "101", 2);
        task.setActionIdempotencyKey("key-1");
        task.setActionRequestDigest("sha256:digest");
        task.setActionType("APPROVE");

        assertThat(FlowTaskActionAuthorization.authorize(
                task, "101", 1L, "APPROVE", "key-1", "sha256:digest", 2)).isTrue();
    }

    @Test
    void shouldNotExposeIdempotentSuccessToAnotherActor() {
        FlowTask task = task(1L, "101", 2);
        task.setActionIdempotencyKey("key-1");
        task.setActionRequestDigest("sha256:digest");
        task.setActionType("APPROVE");

        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task, "202", 1L, "APPROVE", "key-1", "sha256:digest", 2))
                .hasMessage("FLOW_TASK_ASSIGNEE_MISMATCH");
    }

    private FlowTask task(Long tenantId, String assignee, Integer status) {
        FlowTask task = new FlowTask();
        task.setTenantId(tenantId);
        task.setAssignee(assignee);
        task.setStatus(status);
        return task;
    }
}
