package com.mdframe.forge.plugin.ai.resource.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record McpConnectionSaveDTO(
        Long id,
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{2,63}") String connectionCode,
        @NotBlank @Size(max = 80) String connectionName,
        @NotBlank @Size(max = 500) String endpoint,
        @Size(max = 4096) String bearerToken,
        boolean clearSecret,
        @Min(1) @Max(30) int timeoutSeconds,
        @Min(0) @Max(1) int enabled,
        @Min(0) int expectedRowVersion) {
}
