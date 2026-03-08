package com.mdframe.forge.starter.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证方式枚举
 */
@Getter
@AllArgsConstructor
public enum AuthType {

    /**
     * 用户名+密码
     */
    PASSWORD("password", "用户名密码认证"),

    /**
     * 用户名+密码+验证码
     */
    PASSWORD_CAPTCHA("password_captcha", "用户名密码验证码认证"),

    /**
     * 手机号+验证码
     */
    PHONE_CAPTCHA("phone_captcha", "手机号验证码认证"),

    /**
     * 微信授权登录
     */
    WECHAT("wechat", "微信授权登录"),

    /**
     * 邮箱+验证码
     */
    EMAIL_CAPTCHA("email_captcha", "邮箱验证码认证"),

    /**
     * 第三方OAuth2
     */
    OAUTH2("oauth2", "OAuth2认证");

    /**
     * 认证方式代码
     */
    private final String code;

    /**
     * 认证方式描述
     */
    private final String description;

    /**
     * 根据代码获取认证方式
     */
    public static AuthType getByCode(String code) {
        for (AuthType authType : values()) {
            if (authType.getCode().equals(code)) {
                return authType;
            }
        }
        return null;
    }

    /**
     * 验证认证方式是否有效
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }
}
