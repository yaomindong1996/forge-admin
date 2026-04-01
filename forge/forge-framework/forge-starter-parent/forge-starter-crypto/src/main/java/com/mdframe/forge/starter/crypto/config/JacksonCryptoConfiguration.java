package com.mdframe.forge.starter.crypto.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson加密序列化配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.crypto", name = "enabled", havingValue = "true",matchIfMissing = true)
@ConditionalOnBean(EncryptorFactory.class)
public class JacksonCryptoConfiguration {

    private final EncryptorFactory encryptorFactory;
    private final DesensitizeStrategyFactory desensitizeStrategyFactory;

    @Bean
    @ConditionalOnProperty(prefix = "forge.crypto", name = "enable-field-crypto", havingValue = "true", matchIfMissing = true)
    public Jackson2ObjectMapperBuilderCustomizer cryptoJacksonCustomizer() {
        return builder -> {
            builder.postConfigurer(objectMapper -> {
                // 设置加密器工厂到ObjectMapper的属性中，供序列化器使用
                objectMapper.setConfig(
                        objectMapper.getSerializationConfig()
                                .withAttribute(EncryptorFactory.class, encryptorFactory)
                                .withAttribute(DesensitizeStrategyFactory.class, desensitizeStrategyFactory)
                );
                objectMapper.setConfig(
                        objectMapper.getDeserializationConfig()
                                .withAttribute(EncryptorFactory.class, encryptorFactory)
                );
                log.info("Jackson字段级加解密配置完成");
            });
        };
    }
}
