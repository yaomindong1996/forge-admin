package com.mdframe.forge.plugin.capability.controlplane.vo;

import java.util.List;

public record CapabilityGrantCapabilityVO(
        Long id,
        String capabilityCode,
        String capabilityName,
        String currentVersion,
        String sourceType,
        String behavior,
        String riskLevel,
        String requiredActorType,
        String publishStatus,
        Integer enabled,
        List<String> allowedFields,
        List<String> requiredFields,
        List<String> allowedOperations) {
}
