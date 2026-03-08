package com.mdframe.forge.starter.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 短信验证码响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsCaptchaResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码key（用于验证时关联）
     */
    private String codeKey;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 发送状态：success-成功，fail-失败
     */
    private String status;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 验证码（开发环境可返回，生产环境不返回）
     */
    private String code;

    /**
     * 验证码类型
     */
    private String captchaType = "sms";

    /**
     * 发送间隔（秒），用于前端倒计时
     */
    private Integer interval;
}
