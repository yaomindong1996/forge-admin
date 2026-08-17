package com.mdframe.forge.starter.cache.managed;

import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeManagedCacheManagerTest {

    @Test
    void shouldReadWriteAndEvictLocalCache() {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));

        assertFalse(manager.get(definition, "key").hit());
        manager.put(definition, "key", "value");
        CacheLookup lookup = manager.get(definition, "key");
        assertTrue(lookup.hit());
        assertEquals("value", lookup.value());

        manager.evict(definition, "key");
        assertFalse(manager.get(definition, "key").hit());
    }

    @Test
    void shouldCacheNullOnlyWhenDefinitionAllowsIt() {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));

        manager.put(definition, "key", null);

        assertTrue(manager.get(definition, "key").hit());
        assertEquals(null, manager.get(definition, "key").value());
    }

    @Test
    void shouldRejectOverrideOutsideAllowedModes() {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        manager.register(definition);

        CachePolicyOverride override = new CachePolicyOverride(
                "forge-admin", "test:cache", true, CacheMode.MULTI,
                30, 300, 100, true, 10, 1L);

        assertThrows(IllegalArgumentException.class, () -> manager.applyOverride(override));
    }

    @Test
    void shouldRejectMultiPolicyWhenLocalTtlExceedsRedisTtl() {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.MULTI, List.of(CacheMode.MULTI));
        manager.register(definition);

        CachePolicyOverride override = new CachePolicyOverride(
                "forge-admin", "test:cache", true, CacheMode.MULTI,
                301, 300, 100, true, 10, 1L);

        assertThrows(IllegalArgumentException.class, () -> manager.applyOverride(override));
    }

    private ForgeManagedCacheManager manager() {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        return new ForgeManagedCacheManager(null, properties);
    }

    private CacheDefinition definition(CacheMode mode, List<CacheMode> allowedModes) {
        return new CacheDefinition(
                "forge-admin", "test:cache", "Test cache", mode, allowedModes,
                CacheScope.GLOBAL, 60, 300, 100, true, 10,
                getClass().getName());
    }
}
