package com.mdframe.forge.plugin.capability.controlplane.vo;

public record CapabilityClientSecretVO(
        Long clientId,
        String clientCode,
        String keyPrefix,
        String clientSecret,
        Integer credentialVersion) {
}
