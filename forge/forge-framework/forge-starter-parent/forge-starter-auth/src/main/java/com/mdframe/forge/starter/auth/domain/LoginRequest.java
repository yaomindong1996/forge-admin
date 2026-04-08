package com.mdframe.forge.starter.auth.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 验证码key（用于从缓存中获取验证码）
     */
    private String codeKey;

    /**
     * 租户ID（可选，用于多租户登录）
     */
    private Long tenantId;

    /**
     * 认证方式（password-用户名密码，password_captcha-用户名密码验证码，phone_captcha-手机号验证码）
     * 默认为password
     */
    private String authType = "password";

    /**
     * 用户客户端类型（pc, app, h5, wechat）
     * 用于区分不同客户端的认证策略
     */
    private String userClient;

    /**
     * 客户端AppId（可选，用于客户端验证）
     */
    private String appId;
    
    /**
     * 客户端秘钥
     */
    private String appSecret;

    /**
     * 手机号（手机号登录时使用）
     */
    private String phone;

    /**
     * 邮箱（邮箱登录时使用）
     */
    private String email;

    /**
     * 第三方授权码（OAuth2、微信登录时使用）
     */
    private String authCode;

    /**
     * 三方平台类型
     */
    private String socialPlatform;

    /**
     * 三方用户唯一标识
     */
    private String socialUuid;

    /**
     * 三方用户昵称
     */
    private String socialNickname;

    /**
     * 三方用户头像
     */
    private String socialAvatar;

    /**
     * 三方用户邮箱
     */
    private String socialEmail;

    /**
     * 认证方式常量
     */
    public static final String AUTH_TYPE_PASSWORD = "password";
    public static final String AUTH_TYPE_PASSWORD_CAPTCHA = "password_captcha";
    public static final String AUTH_TYPE_PHONE_CAPTCHA = "phone_captcha";
    public static final String AUTH_TYPE_WECHAT = "wechat";
    public static final String AUTH_TYPE_EMAIL_CAPTCHA = "email_captcha";
    public static final String AUTH_TYPE_OAUTH2 = "oauth2";
    public static final String AUTH_TYPE_SOCIAL = "social";
}
