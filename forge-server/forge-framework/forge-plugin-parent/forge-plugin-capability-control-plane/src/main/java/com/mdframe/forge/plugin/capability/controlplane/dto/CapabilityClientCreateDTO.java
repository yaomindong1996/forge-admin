package com.mdframe.forge.plugin.capability.controlplane.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record CapabilityClientCreateDTO(
        @NotBlank String clientCode,
        @NotBlank String clientName,
        @NotNull @Positive Long serviceUserId,
        @NotNull @Positive Long activeOrgId,
        LocalDateTime expiresAt,
        String remark) {
}
