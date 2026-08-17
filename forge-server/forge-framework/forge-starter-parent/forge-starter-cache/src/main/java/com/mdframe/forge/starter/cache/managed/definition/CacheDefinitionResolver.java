package com.mdframe.forge.starter.cache.managed.definition;

import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.cache.managed.model.CacheDefinition;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.List;

public class CacheDefinitionResolver {

    private final ManagedCacheProperties properties;

    public CacheDefinitionResolver(ManagedCacheProperties properties) {
        this.properties = properties;
    }

    public CacheDefinition resolve(Class<?> targetClass, String cacheName) {
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        for (ForgeCacheConfig config : userClass.getAnnotationsByType(ForgeCacheConfig.class)) {
            if (config.name().equals(cacheName)) {
                return from(config, userClass);
            }
        }
        return new CacheDefinition(
                properties.getApplicationCode(),
                cacheName,
                "",
                CacheMode.REDIS,
                List.of(CacheMode.LOCAL, CacheMode.REDIS, CacheMode.MULTI),
                CacheScope.TENANT,
                60,
                1800,
                1000,
                false,
                30,
                userClass.getName());
    }

    private CacheDefinition from(ForgeCacheConfig config, Class<?> sourceClass) {
        return new CacheDefinition(
                properties.getApplicationCode(),
                config.name(),
                config.description(),
                config.mode(),
                Arrays.asList(config.allowedModes()),
                config.scope(),
                config.localTtlSeconds(),
                config.redisTtlSeconds(),
                config.localMaxSize(),
                config.cacheNull(),
                config.nullTtlSeconds(),
                sourceClass.getName());
    }
}
