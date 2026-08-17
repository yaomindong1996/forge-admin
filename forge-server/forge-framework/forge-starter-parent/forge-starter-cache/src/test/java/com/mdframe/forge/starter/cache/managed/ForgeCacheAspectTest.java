package com.mdframe.forge.starter.cache.managed;

import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheEvict;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCachePut;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheable;
import com.mdframe.forge.starter.cache.managed.aop.ForgeCacheAspect;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentity;
import com.mdframe.forge.starter.cache.managed.definition.CacheDefinitionResolver;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.key.ForgeCacheKeyResolver;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeCacheAspectTest {

    @Test
    void shouldApplyCacheablePutAndEvictAnnotations() {
        ManagedCacheProperties properties = new ManagedCacheProperties();
        properties.setApplicationCode("forge-admin");
        ForgeManagedCacheManager manager = new ForgeManagedCacheManager(null, properties);
        ForgeCacheKeyResolver keyResolver = new ForgeCacheKeyResolver(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                () -> new CacheIdentity(null, null, null));
        ForgeCacheAspect aspect = new ForgeCacheAspect(
                manager,
                new CacheDefinitionResolver(properties),
                keyResolver,
                new CacheTransactionExecutor(),
                properties);

        SampleService target = new SampleService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        SampleService proxy = factory.getProxy();

        assertEquals("value-1", proxy.load("a"));
        assertEquals("value-1", proxy.load("a"));
        assertEquals(1, target.loadCount);

        assertEquals("manual", proxy.put("a", "manual"));
        assertEquals("manual", proxy.load("a"));
        assertEquals(1, target.loadCount);

        proxy.evict("a");
        assertEquals("value-2", proxy.load("a"));
        assertEquals(2, target.loadCount);
    }

    @ForgeCacheConfig(
            name = "sample:cache",
            mode = CacheMode.LOCAL,
            allowedModes = CacheMode.LOCAL,
            scope = CacheScope.GLOBAL,
            localTtlSeconds = 60,
            redisTtlSeconds = 300,
            localMaxSize = 100)
    static class SampleService {

        private int loadCount;

        @ForgeCacheable(cacheName = "sample:cache", key = "#key")
        public String load(String key) {
            loadCount++;
            return "value-" + loadCount;
        }

        @ForgeCachePut(cacheName = "sample:cache", key = "#key")
        public String put(String key, String value) {
            return value;
        }

        @ForgeCacheEvict(cacheName = "sample:cache", key = "#key")
        public void evict(String key) {
        }
    }
}
