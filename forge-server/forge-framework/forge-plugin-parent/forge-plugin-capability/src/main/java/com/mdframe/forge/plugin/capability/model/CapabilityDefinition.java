package com.mdframe.forge.plugin.capability.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public record CapabilityDefinition(
        String capabilityCode,
        String protocolToolName,
        String version,
        CapabilityBehavior behavior,
        CapabilityRiskLevel riskLevel,
        String description,
        JsonNode inputSchema,
        JsonNode outputSchema) {

    public CapabilityDefinition {
        capabilityCode = requireText(capabilityCode, "capabilityCode");
        protocolToolName = requireText(protocolToolName, "protocolToolName");
        version = requireText(version, "version");
        behavior = Objects.requireNonNull(behavior, "behavior 不能为空");
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel 不能为空");
        description = requireText(description, "description");
        inputSchema = Objects.requireNonNull(inputSchema, "inputSchema 不能为空").deepCopy();
        outputSchema = Objects.requireNonNull(outputSchema, "outputSchema 不能为空").deepCopy();
    }

    @Override
    public JsonNode inputSchema() {
        return inputSchema.deepCopy();
    }

    @Override
    public JsonNode outputSchema() {
        return outputSchema.deepCopy();
    }

    public String key() {
        return capabilityCode + "@" + version;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }
}
