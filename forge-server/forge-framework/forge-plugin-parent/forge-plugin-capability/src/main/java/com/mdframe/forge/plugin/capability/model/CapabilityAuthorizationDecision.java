package com.mdframe.forge.plugin.capability.model;

import java.util.Objects;

public record CapabilityAuthorizationDecision(
        boolean allowed,
        CapabilityErrorCode errorCode) {

    public CapabilityAuthorizationDecision {
        if (allowed && errorCode != null) {
            throw new IllegalArgumentException("允许结果不能包含错误码");
        }
        if (!allowed) {
            errorCode = Objects.requireNonNull(errorCode, "拒绝结果必须包含错误码");
        }
    }

    public static CapabilityAuthorizationDecision allow() {
        return new CapabilityAuthorizationDecision(true, null);
    }

    public static CapabilityAuthorizationDecision deny(CapabilityErrorCode errorCode) {
        return new CapabilityAuthorizationDecision(false, errorCode);
    }
}
