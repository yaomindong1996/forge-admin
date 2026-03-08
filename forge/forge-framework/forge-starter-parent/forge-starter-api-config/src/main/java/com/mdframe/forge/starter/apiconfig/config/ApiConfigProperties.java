package com.mdframe.forge.starter.apiconfig.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API配置管理属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.api-config")
public class ApiConfigProperties {

    /**
     * 是否启用API配置管理
     */
    private boolean enabled = true;

    /**
     * 是否自动注册接口配置
     */
    private boolean autoRegister = true;

    /**
     * 是否预热缓存
     */
    private boolean cacheWarmUp = true;

    /**
     * 扫描的包路径
     */
    private String[] scanPackages = new String[0];

    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 本地缓存配置
         */
        private LocalConfig local = new LocalConfig();

        /**
         * Redis缓存配置
         */
        private RedisConfig redis = new RedisConfig();

        @Data
        public static class LocalConfig {
            /**
             * 最大缓存数量
             */
            private long maxSize = 1000;

            /**
             * 过期时间（分钟）
             */
            private long expireMinutes = 10;
        }

        @Data
        public static class RedisConfig {
            /**
             * 是否启用Redis缓存
             */
            private boolean enabled = true;

            /**
             * 过期时间（秒）
             */
            private long expireSeconds = 1800;

            /**
             * Key前缀
             */
            private String keyPrefix = "api:config:";
        }
    }
}
