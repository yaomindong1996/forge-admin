package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowTaskActionAuthorizationTest {

    @Test
    void shouldAuthorizeClaimedTaskForSameTenantAndAssignee() {
        assertThat(FlowTaskActionAuthorization.authorize(
                task(1L, "101", FlowTaskStatus.CLAIMED), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", FlowTaskStatus.APPROVED)).isFalse();
    }

    @Test
    void shouldKeepDirectUiCompatibilityForAutoAssignedPendingTask() {
        assertThat(FlowTaskActionAuthorization.authorize(
                task(1L, "101", FlowTaskStatus.PENDING), "101", 1L,
                "APPROVE", null, null, FlowTaskStatus.APPROVED)).isFalse();
    }

    @Test
    void shouldRejectCrossTenantTask() {
        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task(2L, "101", FlowTaskStatus.CLAIMED), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", FlowTaskStatus.APPROVED))
                .hasMessage("任务不属于当前租户");
    }

    @Test
    void shouldRejectTaskTransferredAfterConfirmation() {
        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task(1L, "202", FlowTaskStatus.CLAIMED), "101", 1L,
                "APPROVE", "key-1", "sha256:digest", FlowTaskStatus.APPROVED))
                .hasMessage("当前用户不是任务办理人");
    }

    @Test
    void shouldRejectCompletedTaskWithDifferentRequest() {
        FlowTask task = task(1L, "101", FlowTaskStatus.APPROVED);
        task.setActionIdempotencyKey("key-old");
        task.setActionRequestDigest("sha256:old");
        task.setActionType("APPROVE");

        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task, "101", 1L, "APPROVE", "key-new", "sha256:new", FlowTaskStatus.APPROVED))
                .hasMessage("任务动作幂等冲突，请刷新后重试");
    }

    @Test
    void shouldReturnIdempotentSuccessForSameCompletedRequest() {
        FlowTask task = task(1L, "101", FlowTaskStatus.APPROVED);
        task.setActionIdempotencyKey("key-1");
        task.setActionRequestDigest("sha256:digest");
        task.setActionType("APPROVE");

        assertThat(FlowTaskActionAuthorization.authorize(
                task, "101", 1L, "APPROVE", "key-1", "sha256:digest", FlowTaskStatus.APPROVED)).isTrue();
    }

    @Test
    void shouldNotExposeIdempotentSuccessToAnotherActor() {
        FlowTask task = task(1L, "101", FlowTaskStatus.APPROVED);
        task.setActionIdempotencyKey("key-1");
        task.setActionRequestDigest("sha256:digest");
        task.setActionType("APPROVE");

        assertThatThrownBy(() -> FlowTaskActionAuthorization.authorize(
                task, "202", 1L, "APPROVE", "key-1", "sha256:digest", FlowTaskStatus.APPROVED))
                .hasMessage("当前用户不是任务办理人");
    }

    private FlowTask task(Long tenantId, String assignee, FlowTaskStatus status) {
        FlowTask task = new FlowTask();
        task.setTenantId(tenantId);
        task.setAssignee(assignee);
        task.setStatus(status.getCode());
        return task;
    }
}
