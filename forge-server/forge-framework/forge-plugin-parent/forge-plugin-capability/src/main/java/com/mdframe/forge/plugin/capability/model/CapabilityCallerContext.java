package com.mdframe.forge.plugin.capability.model;

import java.util.Objects;
import java.util.Set;

public record CapabilityCallerContext(
        String machineClientId,
        Long tenantId,
        Long userId,
        Long activeOrgId,
        Set<String> scopes) {

    public CapabilityCallerContext {
        if (machineClientId == null || machineClientId.isBlank()) {
            throw new IllegalArgumentException("machineClientId 不能为空");
        }
        machineClientId = machineClientId.trim();
        tenantId = Objects.requireNonNull(tenantId, "tenantId 不能为空");
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId 必须大于 0");
        }
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("userId 必须大于 0");
        }
        if (activeOrgId != null && activeOrgId <= 0) {
            throw new IllegalArgumentException("activeOrgId 必须大于 0");
        }
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
    }
}
