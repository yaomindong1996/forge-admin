package com.mdframe.forge.starter.cache.managed.context;

public record CacheIdentity(Long tenantId, Long userId, Long activeOrgId) {
}
