package com.mdframe.forge.plugin.capability.secureaction.catalog;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public record SecureActionDescriptor(
        Long capabilityId,
        String capabilityCode,
        String capabilityName,
        String description,
        String version,
        String sourceType,
        String sourceKey,
        String sourceVersion,
        String behavior,
        String riskLevel,
        String suiteCode,
        String objectCode,
        String actionCode,
        Integer publishedObjectVersion,
        String permission,
        Set<String> allowedFields,
        Set<String> requiredFields,
        JsonNode policySnapshot,
        JsonNode inputSchema,
        JsonNode outputSchema) {

    public SecureActionDescriptor(
            Long capabilityId,
            String capabilityCode,
            String capabilityName,
            String description,
            String version,
            String suiteCode,
            String objectCode,
            String actionCode,
            Integer publishedObjectVersion,
            String permission,
            Set<String> allowedFields,
            Set<String> requiredFields,
            JsonNode inputSchema,
            JsonNode outputSchema) {
        this(capabilityId, capabilityCode, capabilityName, description, version,
                "BUSINESS_ACTION", suiteCode + "/" + objectCode + "/" + actionCode,
                String.valueOf(publishedObjectVersion), "ACTION", "MEDIUM",
                suiteCode, objectCode, actionCode, publishedObjectVersion, permission,
                allowedFields, requiredFields, null, inputSchema, outputSchema);
    }
}
