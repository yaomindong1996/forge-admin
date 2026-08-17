package com.mdframe.forge.starter.cache.managed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.cache.managed.ForgeManagedCacheManager;
import com.mdframe.forge.starter.cache.managed.aop.ForgeCacheAspect;
import com.mdframe.forge.starter.cache.managed.context.CacheIdentityProvider;
import com.mdframe.forge.starter.cache.managed.context.SessionCacheIdentityProvider;
import com.mdframe.forge.starter.cache.managed.definition.CacheDefinitionResolver;
import com.mdframe.forge.starter.cache.managed.definition.CacheDefinitionRegistrar;
import com.mdframe.forge.starter.cache.managed.key.ForgeCacheKeyResolver;
import com.mdframe.forge.starter.cache.managed.properties.ManagedCacheProperties;
import com.mdframe.forge.starter.cache.managed.transaction.CacheTransactionExecutor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(ManagedCacheProperties.class)
public class ManagedCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheIdentityProvider cacheIdentityProvider() {
        return new SessionCacheIdentityProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ForgeCacheKeyResolver forgeCacheKeyResolver(ObjectMapper objectMapper,
                                                       CacheIdentityProvider identityProvider) {
        return new ForgeCacheKeyResolver(objectMapper, identityProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheTransactionExecutor cacheTransactionExecutor() {
        return new CacheTransactionExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheDefinitionResolver cacheDefinitionResolver(ManagedCacheProperties properties) {
        return new CacheDefinitionResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheDefinitionRegistrar cacheDefinitionRegistrar(ConfigurableListableBeanFactory beanFactory,
                                                             CacheDefinitionResolver definitionResolver,
                                                             ForgeManagedCacheManager cacheManager) {
        return new CacheDefinitionRegistrar(beanFactory, definitionResolver, cacheManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ForgeManagedCacheManager forgeManagedCacheManager(ObjectProvider<RedissonClient> redissonProvider,
                                                             ManagedCacheProperties properties,
                                                             Environment environment) {
        if (!StringUtils.hasText(properties.getApplicationCode())) {
            properties.setApplicationCode(environment.getProperty("spring.application.name", "forge"));
        }
        return new ForgeManagedCacheManager(redissonProvider.getIfAvailable(), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ForgeCacheAspect forgeCacheAspect(ForgeManagedCacheManager cacheManager,
                                             CacheDefinitionResolver definitionResolver,
                                             ForgeCacheKeyResolver keyResolver,
                                             CacheTransactionExecutor transactionExecutor,
                                             ManagedCacheProperties properties) {
        return new ForgeCacheAspect(cacheManager, definitionResolver, keyResolver, transactionExecutor, properties);
    }
}
