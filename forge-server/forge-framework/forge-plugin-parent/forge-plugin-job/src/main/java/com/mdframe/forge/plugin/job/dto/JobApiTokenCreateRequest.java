package com.mdframe.forge.plugin.job.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

public record JobApiTokenCreateRequest(
        @NotBlank @Size(max = 100) String callerName,
        @Size(max = 500) String callerDescription,
        @NotEmpty Set<String> scopes,
        Set<Long> jobIds,
        Set<@Size(max = 200) String> jobGroups,
        @NotNull @Future LocalDateTime expiresAt) {
}
