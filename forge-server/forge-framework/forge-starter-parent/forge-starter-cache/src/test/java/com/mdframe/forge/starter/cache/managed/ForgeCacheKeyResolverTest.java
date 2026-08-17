package com.mdframe.forge.starter.cache.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentity;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentityProvider;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.key.ForgeCacheKeyResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeCacheKeyResolverTest {

    private final Method method = method();

    @Test
    void shouldResolveSpelAndKeepRawValueOutOfKey() {
        ForgeCacheKeyResolver resolver = resolver(new CacheIdentity(1L, 10L, 100L));

        String key = resolver.resolve(method, this, new Object[]{"phone-13800138000"},
                "#value", CacheScope.TENANT, null).orElseThrow();

        assertEquals(64, key.length());
        assertNotEquals("phone-13800138000", key);
    }

    @Test
    void shouldProduceDifferentKeysForDifferentIdentityScopes() {
        String tenantKey = resolver(new CacheIdentity(1L, 10L, 100L))
                .resolve(method, this, new Object[]{"same"}, "#value", CacheScope.TENANT, null)
                .orElseThrow();
        String userKey = resolver(new CacheIdentity(1L, 11L, 100L))
                .resolve(method, this, new Object[]{"same"}, "#value", CacheScope.TENANT_USER, null)
                .orElseThrow();
        String orgKey = resolver(new CacheIdentity(1L, 11L, 101L))
                .resolve(method, this, new Object[]{"same"}, "#value", CacheScope.TENANT_USER_ORG, null)
                .orElseThrow();

        assertNotEquals(tenantKey, userKey);
        assertNotEquals(userKey, orgKey);
    }

    @Test
    void shouldBypassWhenRequiredTenantContextIsMissing() {
        Optional<String> key = resolver(new CacheIdentity(null, null, null))
                .resolve(method, this, new Object[]{"same"}, "", CacheScope.TENANT, null);

        assertTrue(key.isEmpty());
    }

    private ForgeCacheKeyResolver resolver(CacheIdentity identity) {
        CacheIdentityProvider provider = () -> identity;
        return new ForgeCacheKeyResolver(new ObjectMapper(), provider);
    }

    private Method method() {
        try {
            return getClass().getDeclaredMethod("sample", String.class);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unused")
    private String sample(String value) {
        return value;
    }
}
