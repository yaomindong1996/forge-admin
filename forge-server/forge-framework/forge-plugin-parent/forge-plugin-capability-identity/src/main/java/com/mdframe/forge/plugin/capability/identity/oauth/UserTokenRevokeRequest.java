package com.mdframe.forge.plugin.capability.identity.oauth;

import jakarta.validation.constraints.NotBlank;

public record UserTokenRevokeRequest(
        @NotBlank String clientId,
        @NotBlank String token) {
}
