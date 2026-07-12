package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowTask;

import java.util.Objects;

/**
 * Flow 服务最终任务动作授权规则。
 */
final class FlowTaskActionAuthorization {

    private FlowTaskActionAuthorization() {
    }

    /**
     * @return true 表示命中已完成的同请求幂等结果；false 表示允许首次执行。
     */
    static boolean authorize(FlowTask task, String userId, Long tenantId,
                             String actionType, String idempotencyKey,
                             String requestDigest, int completedStatus) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_TASK_TENANT_REQUIRED");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("FLOW_TASK_ASSIGNEE_REQUIRED");
        }
        if (task == null) {
            throw new IllegalStateException("FLOW_TASK_NOT_FOUND");
        }
        if (!tenantId.equals(task.getTenantId())) {
            throw new IllegalStateException("FLOW_TASK_TENANT_MISMATCH");
        }
        if (!userId.equals(task.getAssignee())) {
            throw new IllegalStateException("FLOW_TASK_ASSIGNEE_MISMATCH");
        }
        if (isIdempotentReplay(task, actionType, idempotencyKey, requestDigest, completedStatus)) {
            return true;
        }
        boolean governedAction = idempotencyKey != null || requestDigest != null;
        boolean actionableStatus = governedAction
                ? Objects.equals(task.getStatus(), 1)
                : Objects.equals(task.getStatus(), 0) || Objects.equals(task.getStatus(), 1);
        if (!actionableStatus) {
            throw new IllegalStateException("FLOW_TASK_NOT_ACTIONABLE");
        }
        if (task.getActionIdempotencyKey() != null) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_CONFLICT");
        }
        if ((idempotencyKey == null) != (requestDigest == null)) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_INVALID");
        }
        return false;
    }

    private static boolean isIdempotentReplay(FlowTask task, String actionType,
                                              String idempotencyKey, String requestDigest,
                                              int completedStatus) {
        if (idempotencyKey == null || requestDigest == null) {
            return false;
        }
        boolean same = idempotencyKey.equals(task.getActionIdempotencyKey())
                && requestDigest.equals(task.getActionRequestDigest())
                && actionType.equals(task.getActionType());
        if (same && Objects.equals(task.getStatus(), completedStatus)) {
            return true;
        }
        if (task.getActionIdempotencyKey() != null) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_CONFLICT");
        }
        return false;
    }
}
