package com.mdframe.forge.starter.config.config;

import lombok.Data;

/**
 * 登录配置
 */
@Data
public class LoginConfig {

    /**
     * 是否启用验证码
     */
    private Boolean enableCaptcha = true;

    /**
     * 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
     */
    private String captchaType = "graphical";

    /**
     * 是否启用记住我功能
     */
    private Boolean enableRememberMe = true;

    /**
     * 记住我有效天数
     */
    private Integer rememberMeDays = 30;

    /**
     * 是否启用登录日志
     */
    private Boolean enableLoginLog = true;

    /**
     * 是否启用IP限制
     */
    private Boolean enableIpLimit = false;

    /**
     * 允许的IP白名单（逗号分隔）
     */
    private String ipWhitelist = "";
}
