package com.mdframe.forge.plugin.capability.identity.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CapabilityOAuthClientConfigRequest(
        @NotNull Boolean enabled,
        @NotBlank String clientType,
        List<String> redirectUris) {
}
