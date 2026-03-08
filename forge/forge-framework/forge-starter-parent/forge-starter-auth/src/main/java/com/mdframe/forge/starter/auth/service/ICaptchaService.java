package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.auth.domain.CaptchaResult;
import com.mdframe.forge.starter.auth.domain.SliderCaptchaResult;
import com.mdframe.forge.starter.auth.domain.SmsCaptchaResult;

import java.time.Duration;

/**
 * 验证码服务接口
 */
public interface ICaptchaService {

    /**
     * 验证码缓存key前缀
     */
    String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 滑块验证码缓存key前缀
     */
    String SLIDER_CAPTCHA_KEY_PREFIX = "captcha:slider:";

    /**
     * 短信验证码缓存key前缀
     */
    String SMS_CAPTCHA_KEY_PREFIX = "captcha:sms:";

    /**
     * 生成验证码
     *
     * @return 验证码key
     */
    String generateCaptcha();

    /**
     * 生成验证码（指定长度）
     *
     * @param length 验证码长度
     * @return 验证码key
     */
    String generateCaptcha(int length);

    /**
     * 生成验证码（指定长度和过期时间）
     *
     * @param length   验证码长度
     * @param duration 过期时间
     * @return 验证码key
     */
    String generateCaptcha(int length, Duration duration);

    /**
     * 获取验证码
     *
     * @param key 验证码key
     * @return 验证码
     */
    String getCaptcha(String key);

    /**
     * 验证验证码
     *
     * @param key  验证码key
     * @param code 验证码
     * @return 是否正确
     */
    boolean validate(String key, String code);

    /**
     * 验证验证码（验证后删除）
     *
     * @param key  验证码key
     * @param code 验证码
     * @return 是否正确
     */
    boolean validateAndDelete(String key, String code);

    /**
     * 删除验证码
     *
     * @param key 验证码key
     */
    void deleteCaptcha(String key);

    /**
     * 生成图形验证码
     *
     * @return 验证码结果，包含key、code（开发环境）和base64图片
     */
    CaptchaResult generateGraphicCaptcha();

    /**
     * 生成图形验证码
     *
     * @param length   验证码长度
     * @param duration 过期时间
     * @return 验证码结果，包含key、code（开发环境）和base64图片
     */
    CaptchaResult generateGraphicCaptcha(int length, Duration duration);

    // ==================== 滑块验证码 ====================

    /**
     * 生成滑块验证码
     *
     * @return 滑块验证码结果
     */
    SliderCaptchaResult generateSliderCaptcha();

    /**
     * 生成滑块验证码
     *
     * @param duration 过期时间
     * @return 滑块验证码结果
     */
    SliderCaptchaResult generateSliderCaptcha(Duration duration);

    /**
     * 验证滑块验证码
     *
     * @param key   验证码key
     * @param moveX 滑块移动距离
     * @return 是否正确
     */
    boolean validateSliderCaptcha(String key, Integer moveX);

    /**
     * 验证滑块验证码（验证后删除）
     *
     * @param key   验证码key
     * @param moveX 滑块移动距离
     * @return 是否正确
     */
    boolean validateAndDeleteSliderCaptcha(String key, Integer moveX);

    // ==================== 短信验证码 ====================

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 发送结果
     */
    SmsCaptchaResult sendSmsCaptcha(String phone);

    /**
     * 发送短信验证码
     *
     * @param phone    手机号
     * @param duration 过期时间
     * @return 发送结果
     */
    SmsCaptchaResult sendSmsCaptcha(String phone, Duration duration);

    /**
     * 验证短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否正确
     */
    boolean validateSmsCaptcha(String phone, String code);

    /**
     * 验证短信验证码（验证后删除）
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否正确
     */
    boolean validateAndDeleteSmsCaptcha(String phone, String code);
}
