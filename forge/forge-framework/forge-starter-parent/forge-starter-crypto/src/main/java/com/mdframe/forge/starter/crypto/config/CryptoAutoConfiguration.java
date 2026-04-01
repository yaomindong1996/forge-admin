package com.mdframe.forge.starter.crypto.config;

import cn.dev33.satoken.config.SaTokenConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.crypto.advice.DecryptRequestBodyAdvice;
import com.mdframe.forge.starter.crypto.advice.EncryptResponseBodyAdvice;
import com.mdframe.forge.starter.crypto.cache.ReplayTokenCache;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import com.mdframe.forge.starter.crypto.crypto.impl.AESEncryptor;
import com.mdframe.forge.starter.crypto.crypto.impl.SM4Encryptor;
import com.mdframe.forge.starter.crypto.filter.ReplayAttackFilter;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.keyexchange.KeyExchangeController;
import com.mdframe.forge.starter.crypto.keyexchange.KeyExchangeService;
import com.mdframe.forge.starter.crypto.keyexchange.RsaKeyPairHolder;
import com.mdframe.forge.starter.crypto.keyexchange.SessionKeyStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * 加密模块自动配置
 */
@Slf4j
@AutoConfiguration
public class CryptoAutoConfiguration {

    
    @Bean
    public KeyExchangeController keyExchangeController(KeyExchangeService keyExchangeService) {
        return new KeyExchangeController(keyExchangeService);
    }
    
    // ==================== 密钥交换相关 Bean ====================

    @Bean
    public RsaKeyPairHolder rsaKeyPairHolder(CryptoProperties properties) {
        if (StringUtils.hasText(properties.getRsaPublicKey())
            && StringUtils.hasText(properties.getRsaPrivateKey())) {
            log.info("使用配置的 RSA 密钥对");
            return new RsaKeyPairHolder(properties.getRsaPublicKey(), properties.getRsaPrivateKey());
        }
        log.info("自动生成 RSA 密钥对");
        return new RsaKeyPairHolder();
    }

    @Bean
    @ConditionalOnBean(ICacheService.class)
    public SessionKeyStore sessionKeyStore(ICacheService cacheService,SaTokenConfig saTokenConfig) {
        log.info("会话密钥存储初始化完成, 过期时间: {}秒", saTokenConfig.getTimeout());
        return new SessionKeyStore(cacheService, saTokenConfig.getTimeout());
    }

    @Bean
    public KeyExchangeService keyExchangeService(RsaKeyPairHolder rsaKeyPairHolder, SessionKeyStore sessionKeyStore) {
        log.info("密钥交换服务初始化完成");
        return new KeyExchangeService(rsaKeyPairHolder, sessionKeyStore);
    }

    // ==================== 加密器相关 Bean ====================

    @Bean
    public SM4Encryptor sm4Encryptor(CryptoProperties properties) {
        return new SM4Encryptor(properties);
    }

    @Bean
    public AESEncryptor aesEncryptor(CryptoProperties properties) {
        return new AESEncryptor(properties);
    }

    // ==================== 脱敏相关 Bean ====================

    @Bean
    public DesensitizeStrategyFactory desensitizeStrategyFactory() {
        log.info("脱敏策略工厂初始化完成");
        return new DesensitizeStrategyFactory();
    }

    @Bean
    public EncryptorFactory encryptorFactory(CryptoProperties properties,
                                             SM4Encryptor sm4Encryptor,
                                             AESEncryptor aesEncryptor) {
        EncryptorFactory factory = new EncryptorFactory(properties);
        factory.register(sm4Encryptor);
        factory.register(aesEncryptor);
        log.info("加密器工厂初始化完成, 已注册算法: SM4, AES");
        return factory;
    }

    @Bean
    public EncryptResponseBodyAdvice encryptResponseBodyAdvice(CryptoProperties properties,
                                                               EncryptorFactory encryptorFactory,
                                                               ObjectMapper objectMapper,
                                                               @Autowired(required = false) SessionKeyStore sessionKeyStore,
            IApiConfigManager apiConfigManager) {
        log.info("响应加密处理器初始化完成, 动态密钥: {}", sessionKeyStore != null);
        return new EncryptResponseBodyAdvice(properties, encryptorFactory, objectMapper, sessionKeyStore,apiConfigManager);
    }

    @Bean
    public DecryptRequestBodyAdvice decryptRequestBodyAdvice(CryptoProperties properties,
                                                             EncryptorFactory encryptorFactory,
                                                             ObjectMapper objectMapper,
                                                             @Autowired(required = false) SessionKeyStore sessionKeyStore,
            IApiConfigManager apiConfigManager) {
        log.info("请求解密处理器初始化完成, 动态密钥: {}", sessionKeyStore != null);
        return new DecryptRequestBodyAdvice(properties, encryptorFactory, objectMapper, sessionKeyStore,apiConfigManager);
    }

    // ==================== 防重放相关 Bean ====================

    @Bean
    @ConditionalOnBean(ICacheService.class)
    public ReplayTokenCache replayTokenCache(ICacheService cacheService) {
        log.info("防重放令牌缓存初始化完成");
        return new ReplayTokenCache(cacheService);
    }

    @Bean
    @ConditionalOnBean(ReplayTokenCache.class)
    public FilterRegistrationBean<ReplayAttackFilter> replayAttackFilter(
            CryptoProperties properties,
            ReplayTokenCache tokenCache,
            ObjectMapper objectMapper) {

        FilterRegistrationBean<ReplayAttackFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ReplayAttackFilter(properties, tokenCache, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        log.info("防重放攻击过滤器初始化完成");
        return registration;
    }
}
