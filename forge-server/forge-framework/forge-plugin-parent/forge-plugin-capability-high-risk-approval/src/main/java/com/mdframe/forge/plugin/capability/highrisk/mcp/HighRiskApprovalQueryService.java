package com.mdframe.forge.plugin.capability.highrisk.mcp;

import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityApproval;
import com.mdframe.forge.plugin.capability.highrisk.mapper.CapabilityApprovalMapper;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class HighRiskApprovalQueryService {

    private static final String QUERY_PERMISSION = "ai:capability:approval:query";
    private final CapabilityApprovalMapper approvalMapper;

    public Map<String, Object> get(Long approvalId) {
        ExecutionIdentity identity = ExecutionIdentityContextHolder.current()
                .orElseThrow(() -> new BusinessException(401, "UNAUTHENTICATED"));
        if (!"USER".equals(identity.actorType())) {
            throw new BusinessException(403, "USER_DELEGATION_REQUIRED");
        }
        requirePermission(identity);
        AiCapabilityApproval approval = approvalMapper.selectOwned(
                identity.loginUser().getTenantId(), approvalId, identity.clientId(),
                identity.actorUserId(), identity.serviceUserId(),
                identity.loginUser().getActiveOrgId());
        if (approval == null) {
            throw new BusinessException(404, "APPROVAL_NOT_FOUND");
        }
        String status = approval.getExecuteStatus();
        if ("PENDING_APPROVAL".equals(status)
                && approval.getExpiresAt() != null
                && !approval.getExpiresAt().isAfter(LocalDateTime.now())) {
            status = "EXPIRED";
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("approvalRequestId", String.valueOf(approval.getId()));
        result.put("capabilityCode", approval.getCapabilityCode());
        result.put("version", approval.getCapabilityVersion());
        result.put("status", status);
        result.put("resultCode", "EXPIRED".equals(status) ? "EXPIRED" : approval.getResultCode());
        putIfNotNull(result, "submittedAt", text(approval.getCreateTime()));
        putIfNotNull(result, "expiresAt", text(approval.getExpiresAt()));
        putIfNotNull(result, "completedAt", text(approval.getCompletedAt()));
        putIfNotNull(result, "correlationId", approval.getRequestId());
        result.put("message", message(status));
        return result;
    }

    private void requirePermission(ExecutionIdentity identity) {
        Set<String> permissions = identity.loginUser().getPermissions();
        if (!hasPermission(permissions, QUERY_PERMISSION)) {
            throw new BusinessException(403, "FORBIDDEN");
        }
    }

    private boolean hasPermission(Set<String> permissions, String required) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        if (permissions.contains("*:*:*") || permissions.contains(required)) {
            return true;
        }
        int splitIndex = required.lastIndexOf(':');
        while (splitIndex > 0) {
            if (permissions.contains(required.substring(0, splitIndex) + ":*")) {
                return true;
            }
            splitIndex = required.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private String text(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private String message(String status) {
        return switch (status) {
            case "RESERVED" -> "审批请求正在创建";
            case "PENDING_APPROVAL" -> "高风险动作等待人工审批";
            case "SUCCESS" -> "审批通过且业务动作已执行";
            case "REJECTED" -> "审批已驳回，业务动作未执行";
            case "CANCELLED" -> "审批已取消，业务动作未执行";
            case "EXPIRED" -> "审批已过期，业务动作未执行";
            case "FAILED" -> "审批后置校验或业务执行失败";
            default -> "审批状态不可用";
        };
    }
}
