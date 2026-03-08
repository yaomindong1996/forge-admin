package com.mdframe.forge.starter.config.config;

import lombok.Data;

/**
 * 安全配置
 */
@Data
public class SecurityConfig {

    /**
     * Sa-Token配置
     */
    private SaTokenConfig saToken = new SaTokenConfig();

    /**
     * 密码策略配置
     */
    private PasswordPolicyConfig passwordPolicy = new PasswordPolicyConfig();

    /**
     * Sa-Token配置
     */
    @Data
    public static class SaTokenConfig {
        /**
         * token有效期（秒）
         */
        private Long timeout = 2592000L;

        /**
         * 第二次无操作后token过期时间（秒）
         */
        private Long activityTimeout = -1L;

        /**
         * 是否允许同一账号并发登录
         */
        private Boolean isConcurrent = false;

        /**
         * 在多人登录同一账号时，是否共用一个token
         */
        private Boolean isShare = false;

        /**
         * 是否尝试从请求中读取token
         */
        private Boolean isReadBody = true;

        /**
         * 是否尝试从header中读取token
         */
        private Boolean isReadHeader = true;

        /**
         * 是否尝试从cookie中读取token
         */
        private Boolean isReadCookie = false;

        /**
         * token前缀
         */
        private String tokenPrefix = "Bearer";

        /**
         * token名称
         */
        private String tokenName = "Authorization";
    }

    /**
     * 密码策略配置
     */
    @Data
    public static class PasswordPolicyConfig {
        /**
         * 最小密码长度
         */
        private Integer minLength = 8;

        /**
         * 是否包含大写字母
         */
        private Boolean requireUppercase = true;

        /**
         * 是否包含小写字母
         */
        private Boolean requireLowercase = true;

        /**
         * 是否包含数字
         */
        private Boolean requireNumbers = true;

        /**
         * 是否包含特殊字符
         */
        private Boolean requireSpecialChars = false;

        /**
         * 密码过期天数
         */
        private Integer expireDays = 90;

        /**
         * 密码历史记录数量
         */
        private Integer historyCount = 5;
    }
}
