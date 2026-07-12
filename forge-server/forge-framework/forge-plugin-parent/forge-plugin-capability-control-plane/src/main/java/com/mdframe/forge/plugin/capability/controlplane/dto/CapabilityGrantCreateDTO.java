package com.mdframe.forge.plugin.capability.controlplane.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CapabilityGrantCreateDTO(
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long capabilityId,
        @NotBlank String versionStrategy,
        @NotBlank String fixedVersion,
        JsonNode fieldPolicy,
        LocalDateTime expiresAt) {
}
