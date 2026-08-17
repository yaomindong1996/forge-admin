package com.mdframe.forge.starter.cache.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.model.CacheLookup;
import com.mdframe.forge.starter.cache.managed.model.CachePolicyOverride;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldFallBackToCodeDefaultsWhenRuntimeOverrideBecomesInvalid() throws Exception {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        manager.register(definition);
        CachePolicyOverride invalid = new CachePolicyOverride(
                "forge-admin", "test:cache", true, CacheMode.MULTI,
                30, 300, 100, true, 10, 1L);
        overrides(manager).set(Map.of(invalid.identity(), invalid));

        manager.put(definition, "key", "value");

        assertTrue(manager.get(definition, "key").hit());
        assertEquals(CacheMode.LOCAL, manager.listCaches().get(0).policy().cacheMode());
    }

    @Test
    void shouldRegisterDefinitionInRedisOnlyOnce() {
        RedisControlPlane controlPlane = controlPlane(Map.of());
        ForgeManagedCacheManager manager = manager(controlPlane.client());
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        CacheDefinition sharedDefinition = new CacheDefinition(
                definition.applicationCode(), definition.cacheName(), "Shared cache", definition.defaultMode(),
                definition.allowedModes(), definition.scope(), definition.localTtlSeconds(),
                definition.redisTtlSeconds(), definition.localMaxSize(), definition.cacheNull(),
                definition.nullTtlSeconds(), "another.Source");

        manager.register(definition);
        manager.register(sharedDefinition);

        verify(controlPlane.definitionMap(), times(1))
                .putIfAbsent(definition.identity(), definition);
    }

    @Test
    void shouldRejectIncompatibleLocalDefinition() {
        ForgeManagedCacheManager manager = manager();
        CacheDefinition definition = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        CacheDefinition incompatible = new CacheDefinition(
                definition.applicationCode(), definition.cacheName(), definition.description(),
                definition.defaultMode(), definition.allowedModes(), CacheScope.TENANT,
                definition.localTtlSeconds(), definition.redisTtlSeconds(), definition.localMaxSize(),
                definition.cacheNull(), definition.nullTtlSeconds(), "another.Source");
        manager.register(definition);

        assertThrows(IllegalStateException.class, () -> manager.register(incompatible));
    }

    @Test
    void shouldAcceptCompatibleRemoteDefinitionFromDifferentSource() {
        RedisControlPlane controlPlane = controlPlane(Map.of());
        CacheDefinition local = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        CacheDefinition remote = new CacheDefinition(
                local.applicationCode(), local.cacheName(), "Remote description", local.defaultMode(),
                local.allowedModes(), local.scope(), local.localTtlSeconds(), local.redisTtlSeconds(),
                local.localMaxSize(), local.cacheNull(), local.nullTtlSeconds(), "remote.Source");
        when(controlPlane.definitionMap().putIfAbsent(local.identity(), local)).thenReturn(remote);

        assertDoesNotThrow(() -> manager(controlPlane.client()).register(local));
    }

    @Test
    void shouldRejectIncompatibleRemoteDefinitionWithoutOverwritingIt() {
        RedisControlPlane controlPlane = controlPlane(Map.of());
        CacheDefinition local = definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL));
        CacheDefinition remote = new CacheDefinition(
                local.applicationCode(), local.cacheName(), local.description(), local.defaultMode(),
                local.allowedModes(), CacheScope.TENANT, local.localTtlSeconds(), local.redisTtlSeconds(),
                local.localMaxSize(), local.cacheNull(), local.nullTtlSeconds(), "remote.Source");
        when(controlPlane.definitionMap().putIfAbsent(local.identity(), local)).thenReturn(remote);

        ForgeManagedCacheManager manager = manager(controlPlane.client());

        assertThrows(IllegalStateException.class, () -> manager.register(local));
        verify(controlPlane.definitionMap(), times(1)).putIfAbsent(local.identity(), local);
    }

    @Test
    void shouldReplacePolicyOverridesAsOneImmutableSnapshot() throws Exception {
        CachePolicyOverride initial = override(false, 1L);
        CachePolicyOverride refreshed = override(true, 2L);
        RedisControlPlane controlPlane = controlPlane(Map.of(initial.identity(), initial));
        when(controlPlane.policyMap().readAllMap())
                .thenReturn(Map.of(initial.identity(), initial))
                .thenReturn(Map.of(refreshed.identity(), refreshed));
        ForgeManagedCacheManager manager = manager(controlPlane.client());
        Map<String, CachePolicyOverride> previous = overrides(manager).get();

        manager.get(definition(CacheMode.LOCAL, List.of(CacheMode.LOCAL)), "key");

        Map<String, CachePolicyOverride> current = overrides(manager).get();
        assertNotSame(previous, current);
        assertEquals(initial, previous.get(initial.identity()));
        assertEquals(refreshed, current.get(refreshed.identity()));
        assertThrows(UnsupportedOperationException.class,
                () -> current.put(initial.identity(), initial));
    }

    @SuppressWarnings("unchecked")
    private AtomicReference<Map<String, CachePolicyOverride>> overrides(
            ForgeManagedCacheManager manager) throws Exception {
        var field = ForgeManagedCacheManager.class.getDeclaredField("overrides");
        field.setAccessible(true);
        return (AtomicReference<Map<String, CachePolicyOverride>>) field.get(manager);
    }

    private ForgeManagedCacheManager manager() {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        return new ForgeManagedCacheManager(null, properties);
    }

    private ForgeManagedCacheManager manager(RedissonClient client) {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        return new ForgeManagedCacheManager(client, properties, new ObjectMapper().findAndRegisterModules());
    }

    private CacheDefinition definition(CacheMode mode, List<CacheMode> allowedModes) {
        return new CacheDefinition(
                "forge-admin", "test:cache", "Test cache", mode, allowedModes,
                CacheScope.GLOBAL, 60, 300, 100, true, 10,
                getClass().getName());
    }

    private CachePolicyOverride override(boolean enabled, long version) {
        return new CachePolicyOverride(
                "forge-admin", "test:cache", enabled, CacheMode.LOCAL,
                60, 300, 100, true, 10, version);
    }

    @SuppressWarnings("unchecked")
    private RedisControlPlane controlPlane(Map<String, CachePolicyOverride> initialOverrides) {
        RedissonClient client = mock(RedissonClient.class);
        RMap<String, CacheDefinition> definitionMap = mock(RMap.class);
        RMap<String, CachePolicyOverride> policyMap = mock(RMap.class);
        RTopic topic = mock(RTopic.class);
        doReturn(definitionMap).when(client).getMap(anyString(), any(Codec.class));
        doReturn(policyMap).when(client).getMap(
                org.mockito.ArgumentMatchers.endsWith(":control:policies"), any(Codec.class));
        doReturn(topic).when(client).getTopic(anyString(), any(Codec.class));
        when(policyMap.readAllMap()).thenReturn(initialOverrides);
        return new RedisControlPlane(client, definitionMap, policyMap);
    }

    private record RedisControlPlane(
            RedissonClient client,
            RMap<String, CacheDefinition> definitionMap,
            RMap<String, CachePolicyOverride> policyMap) {
    }
}
