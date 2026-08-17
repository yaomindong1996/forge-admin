package com.mdframe.forge.starter.cache.managed;

import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import com.mdframe.forge.starter.cache.managed.definition.CacheDefinitionResolver;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CacheDefinitionResolverTest {

    private final CacheDefinitionResolver resolver = new CacheDefinitionResolver(properties());

    @Test
    void shouldRejectUnmatchedNameWhenClassDeclaresManagedCaches() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> resolver.resolve(DeclaredCacheService.class, "declared:typo"));

        assertEquals("缓存名称未在类级配置中声明: declared:typo", exception.getMessage());
    }

    @Test
    void shouldKeepGlobalFallbackForClassWithoutManagedCacheDeclarations() {
        CacheDefinition definition = resolver.resolve(UndeclaredCacheService.class, "implicit:cache");

        assertEquals(CacheMode.REDIS, definition.defaultMode());
        assertEquals(CacheScope.TENANT, definition.scope());
    }

    private ManagedCacheProperties properties() {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        return properties;
    }

    @ForgeCacheConfig(name = "declared:cache", mode = CacheMode.LOCAL, scope = CacheScope.GLOBAL)
    private static class DeclaredCacheService {
    }

    private static class UndeclaredCacheService {
    }
}
