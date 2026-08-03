package com.mdframe.forge.plugin.capability.controlplane.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record CapabilityGrantUpdateDTO(
        @NotBlank(message = "请选择版本策略") String versionStrategy,
        @NotBlank(message = "请输入固定版本") String fixedVersion,
        JsonNode fieldPolicy,
        LocalDateTime expiresAt) {
}
