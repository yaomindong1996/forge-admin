package com.mdframe.forge.plugin.capability.secureaction.catalog;

import lombok.Data;

@Data
public class SecureActionCatalogRow {

    private Long capabilityId;
    private String capabilityCode;
    private String capabilityName;
    private String description;
    private String sourceType;
    private String sourceKey;
    private String sourceVersion;
    private String behavior;
    private String version;
    private String inputSchema;
    private String outputSchema;
    private String policySnapshot;
    private String fieldPolicy;
    private String riskLevel;
}
