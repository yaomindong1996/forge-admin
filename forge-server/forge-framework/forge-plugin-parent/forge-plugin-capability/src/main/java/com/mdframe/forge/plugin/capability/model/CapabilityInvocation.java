package com.mdframe.forge.plugin.capability.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record CapabilityInvocation(
        String requestId,
        String capabilityCode,
        String version,
        CapabilityCallerContext caller,
        JsonNode arguments) {

    public CapabilityInvocation {
        requestId = requireText(requestId, "requestId");
        capabilityCode = requireText(capabilityCode, "capabilityCode");
        version = requireText(version, "version");
        caller = Objects.requireNonNull(caller, "caller 不能为空");
        arguments = Objects.requireNonNull(arguments, "arguments 不能为空").deepCopy();
    }

    @Override
    public JsonNode arguments() {
        return arguments.deepCopy();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }
}
