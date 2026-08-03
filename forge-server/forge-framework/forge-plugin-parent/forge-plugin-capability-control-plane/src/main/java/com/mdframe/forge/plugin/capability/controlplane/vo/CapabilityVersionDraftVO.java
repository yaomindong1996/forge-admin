package com.mdframe.forge.plugin.capability.controlplane.vo;

import com.fasterxml.jackson.databind.JsonNode;

public record CapabilityVersionDraftVO(
        Long capabilityId,
        String capabilityCode,
        String sourceType,
        String sourceKey,
        String sourceVersion,
        String currentVersion,
        String suggestedVersion,
        String description,
        JsonNode inputSchema,
        JsonNode policySnapshot) {
}
