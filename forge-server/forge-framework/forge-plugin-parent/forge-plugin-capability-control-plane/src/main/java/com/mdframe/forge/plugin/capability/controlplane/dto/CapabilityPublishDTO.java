package com.mdframe.forge.plugin.capability.controlplane.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CapabilityPublishDTO(
        @NotBlank String capabilityCode,
        @NotBlank String protocolToolName,
        @NotBlank String capabilityName,
        @NotBlank String description,
        @NotBlank String sourceType,
        @NotBlank String sourceKey,
        @NotBlank String sourceVersion,
        @NotBlank String version,
        @NotBlank String behavior,
        @NotBlank String riskLevel,
        @NotBlank String visibility,
        @NotNull JsonNode inputSchema,
        @NotNull JsonNode outputSchema,
        JsonNode policySnapshot) {
}
