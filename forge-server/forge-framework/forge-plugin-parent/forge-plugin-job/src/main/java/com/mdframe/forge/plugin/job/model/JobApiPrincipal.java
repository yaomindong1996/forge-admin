package com.mdframe.forge.plugin.job.model;

import java.util.Set;

public record JobApiPrincipal(
        Long tokenId,
        Long tenantId,
        String tokenKeyId,
        String callerName,
        Set<String> scopes,
        Set<Long> jobIds,
        Set<String> jobGroups) {

    public JobApiPrincipal {
        scopes = Set.copyOf(scopes);
        jobIds = Set.copyOf(jobIds);
        jobGroups = Set.copyOf(jobGroups);
    }
}
