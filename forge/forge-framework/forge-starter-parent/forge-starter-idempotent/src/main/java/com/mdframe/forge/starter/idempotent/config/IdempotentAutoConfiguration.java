package com.mdframe.forge.starter.idempotent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.idempotent.aop.IdempotentAspect;
import com.mdframe.forge.starter.idempotent.controller.IdempotentTokenController;
import com.mdframe.forge.starter.idempotent.enums.IdempotentStrategy;
import com.mdframe.forge.starter.idempotent.generator.DefaultIdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.generator.IdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.lock.LockManager;
import com.mdframe.forge.starter.idempotent.lock.RedissonLockManager;
import com.mdframe.forge.starter.idempotent.properties.CacheProperties;
import com.mdframe.forge.starter.idempotent.properties.IdempotentProperties;
import com.mdframe.forge.starter.idempotent.properties.LockProperties;
import com.mdframe.forge.starter.idempotent.properties.TokenProperties;
import com.mdframe.forge.starter.idempotent.service.*;
import com.mdframe.forge.starter.idempotent.strategy.IdempotentStrategyHandler;
import com.mdframe.forge.starter.idempotent.strategy.ReturnCacheStrategyHandler;
import com.mdframe.forge.starter.idempotent.strategy.StrictStrategyHandler;
import com.mdframe.forge.starter.idempotent.strategy.TokenRequiredStrategyHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties({
    IdempotentProperties.class,
    TokenProperties.class,
    CacheProperties.class,
    LockProperties.class
})
@ConditionalOnProperty(prefix = "forge.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public IdempotentKeyGenerator idempotentKeyGenerator() {
        return new DefaultIdempotentKeyGenerator();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(StringRedisTemplate redisTemplate, TokenProperties properties) {
        return new RedisTokenService(redisTemplate, properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ResultCacheService resultCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new RedisResultCacheService(redisTemplate, objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(RedissonClient.class)
    public LockManager lockManager(RedissonClient redissonClient) {
        return new RedissonLockManager(redissonClient);
    }
    
    @Bean
    public IdempotentStrategyHandler strictStrategyHandler(LockManager lockManager, LockProperties properties) {
        return new StrictStrategyHandler(lockManager, properties);
    }
    
    @Bean
    public IdempotentStrategyHandler returnCacheStrategyHandler(
            LockManager lockManager,
            ResultCacheService resultCacheService,
            LockProperties properties) {
        return new ReturnCacheStrategyHandler(lockManager, resultCacheService, properties);
    }
    
    @Bean
    public IdempotentStrategyHandler tokenRequiredStrategyHandler(
            TokenService tokenService,
            TokenProperties tokenProperties,
            IdempotentStrategyHandler returnCacheStrategyHandler) {
        return new TokenRequiredStrategyHandler(tokenService, tokenProperties, returnCacheStrategyHandler);
    }
    
    @Bean
    public Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers(
            IdempotentStrategyHandler strictStrategyHandler,
            IdempotentStrategyHandler returnCacheStrategyHandler,
            IdempotentStrategyHandler tokenRequiredStrategyHandler) {
        
        Map<IdempotentStrategy, IdempotentStrategyHandler> handlers = new HashMap<>();
        handlers.put(IdempotentStrategy.STRICT, strictStrategyHandler);
        handlers.put(IdempotentStrategy.RETURN_CACHE, returnCacheStrategyHandler);
        handlers.put(IdempotentStrategy.TOKEN_REQUIRED, tokenRequiredStrategyHandler);
        return handlers;
    }
    
    @Bean
    public IdempotentAspect idempotentAspect(
            IdempotentKeyGenerator keyGenerator,
            IdempotentProperties properties,
            Map<IdempotentStrategy, IdempotentStrategyHandler> strategyHandlers) {
        return new IdempotentAspect(keyGenerator, properties, strategyHandlers);
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "forge.idempotent.token", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IdempotentTokenController idempotentTokenController(TokenService tokenService, TokenProperties properties) {
        return new IdempotentTokenController(tokenService, properties);
    }
}