package com.mdframe.forge.plugin.capability.identity.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthorizationDecisionRequest(
        @NotBlank String clientId,
        @NotBlank String responseType,
        @NotBlank String redirectUri,
        @NotBlank String resource,
        @NotBlank String scope,
        @NotBlank String codeChallenge,
        @NotBlank String codeChallengeMethod,
        String state,
        @NotNull Boolean approved) {
}
