package com.mdframe.forge.starter.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 */
@Slf4j
@Configuration
public class RedissonConfig {

    /**
     * 自定义Redisson配置
     * 配置Jackson序列化器，支持Java 8时间类型
     */
    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return config -> {
            // 配置Jackson序列化
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            config.setCodec(new JsonJacksonCodec(objectMapper));
            
            log.info("Redisson配置初始化完成");
        };
    }
}
