package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;

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
                             String requestDigest, FlowTaskStatus completedStatus) {
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
        if (isIdempotentReplay(task, actionType, idempotencyKey, requestDigest, completedStatus)) {
            // 转办后 assignee 已经切换为目标用户，原操作者通过 owner 重试同一请求时
            // 仍应命中幂等结果；普通审批任务不会设置 owner，因此不扩大正常操作范围。
            if (!userId.equals(task.getAssignee()) && !userId.equals(task.getOwner())) {
                throw new IllegalStateException("FLOW_TASK_ASSIGNEE_MISMATCH");
            }
            return true;
        }
        if (!userId.equals(task.getAssignee())) {
            throw new IllegalStateException("FLOW_TASK_ASSIGNEE_MISMATCH");
        }
        boolean governedAction = idempotencyKey != null || requestDigest != null;
        boolean actionableStatus = governedAction
                ? FlowTaskStatus.CLAIMED.matches(task.getStatus())
                : FlowTaskStatus.isActionable(task.getStatus());
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
                                              FlowTaskStatus completedStatus) {
        if (idempotencyKey == null || requestDigest == null) {
            return false;
        }
        boolean same = idempotencyKey.equals(task.getActionIdempotencyKey())
                && requestDigest.equals(task.getActionRequestDigest())
                && actionType.equals(task.getActionType());
        if (same && completedStatus != null && completedStatus.matches(task.getStatus())) {
            return true;
        }
        if (task.getActionIdempotencyKey() != null) {
            throw new IllegalStateException("FLOW_TASK_IDEMPOTENCY_CONFLICT");
        }
        return false;
    }
}
