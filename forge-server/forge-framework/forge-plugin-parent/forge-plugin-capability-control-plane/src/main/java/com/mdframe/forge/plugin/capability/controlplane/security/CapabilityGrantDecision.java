package com.mdframe.forge.plugin.capability.controlplane.security;

public record CapabilityGrantDecision(
        boolean allowed,
        String resolvedVersion,
        String errorCode) {

    public static CapabilityGrantDecision allow(String version) {
        return new CapabilityGrantDecision(true, version, null);
    }

    public static CapabilityGrantDecision deny(String errorCode) {
        return new CapabilityGrantDecision(false, null, errorCode);
    }
}
