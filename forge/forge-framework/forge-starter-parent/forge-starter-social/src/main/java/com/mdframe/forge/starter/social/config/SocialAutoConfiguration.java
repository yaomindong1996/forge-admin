package com.mdframe.forge.starter.social.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 三方登录自动配置
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "forge.social", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocialAutoConfiguration {

    public SocialAutoConfiguration() {
        log.info("三方登录模块初始化完成");
    }
}
