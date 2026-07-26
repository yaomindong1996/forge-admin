package com.mdframe.forge.starter.auth.sms;

import java.time.Duration;

/**
 * 短信验证码发送适配接口。
 */
@FunctionalInterface
public interface SmsCaptchaSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 手机号
     * @param code 验证码
     * @param duration 验证码有效期
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String phone, String code, Duration duration);
}
