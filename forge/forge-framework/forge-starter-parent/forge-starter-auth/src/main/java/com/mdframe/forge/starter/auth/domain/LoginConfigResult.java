package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录配置响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginConfigResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否启用验证码
     */
    private Boolean enableCaptcha;

    /**
     * 验证码类型：graphical(图形验证码), slider(滑块验证码), sms(短信验证码)
     */
    private String captchaType;

    /**
     * 是否启用记住我功能
     */
    private Boolean enableRememberMe;

    /**
     * 是否启用登录日志
     */
    private Boolean enableLoginLog;

    /**
     * 是否启用IP限制
     */
    private Boolean enableIpLimit;
}
