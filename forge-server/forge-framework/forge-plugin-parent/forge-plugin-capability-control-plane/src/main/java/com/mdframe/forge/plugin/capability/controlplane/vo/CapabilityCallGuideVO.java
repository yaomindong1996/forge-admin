package com.mdframe.forge.plugin.capability.controlplane.vo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record CapabilityCallGuideVO(
        Long capabilityId,
        String capabilityCode,
        String capabilityName,
        String version,
        String currentVersion,
        Long grantId,
        String grantVersionStrategy,
        String grantFixedVersion,
        boolean versionUpgradeAvailable,
        Long clientId,
        String clientCode,
        String clientName,
        boolean ready,
        String behavior,
        String sourceType,
        String actionCode,
        String requiredActorType,
        boolean tokenExchangeRequired,
        List<String> availableAuthModes,
        String openapiResource,
        String invokeUrl,
        String tokenUrl,
        JsonNode requestExample,
        List<String> requestNotes,
        List<String> runtimePermissions,
        List<CapabilityCallGuideCheckVO> checks,
        String oauthExample,
        String hmacExample,
        String oauthJavaExample,
        String hmacJavaExample,
        boolean userAssertionEnabled,
        String userAssertionKeyId,
        String userAssertionIssuer,
        String userAssertionAudience,
        String userAssertionSubjectTokenType,
        long userAssertionMaxTtlSeconds,
        String userAssertionJavaExample) {
}
