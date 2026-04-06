package com.mdframe.forge.starter.idempotent.config;

import com.mdframe.forge.starter.idempotent.aop.IdempotentAspect;
import com.mdframe.forge.starter.idempotent.generator.DefaultIdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.generator.IdempotentKeyGenerator;
import com.mdframe.forge.starter.idempotent.properties.IdempotentProperties;
import com.mdframe.forge.starter.idempotent.service.IdempotentStorageService;
import com.mdframe.forge.starter.idempotent.service.RedisIdempotentStorageService;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(IdempotentProperties.class)
@ConditionalOnProperty(prefix = "idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentKeyGenerator idempotentKeyGenerator() {
        return new DefaultIdempotentKeyGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentStorageService idempotentStorageService(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        return new RedisIdempotentStorageService(stringRedisTemplate, redissonClient);
    }

    @Bean
    public IdempotentAspect idempotentAspect(IdempotentKeyGenerator keyGenerator, IdempotentStorageService storageService, IdempotentProperties properties) {
        return new IdempotentAspect(keyGenerator, storageService, properties);
    }
}
