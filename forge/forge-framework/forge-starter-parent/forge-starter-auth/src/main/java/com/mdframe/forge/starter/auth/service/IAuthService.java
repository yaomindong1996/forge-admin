package com.mdframe.forge.starter.auth.service;

import com.mdframe.forge.starter.auth.domain.*;
import com.mdframe.forge.starter.core.session.LoginUser;

/**
 * 认证服务接口
 */
public interface IAuthService {

    /**
     * 统一登录入口
     * 根据authType和userClient自动选择认证方式
     *
     * @param request 登录请求
     * @return 登录结果
     */
    LoginResult login(LoginRequest request);

    /**
     * 登出
     */
    void logout();

    /**
     * 根据用户名加载用户信息
     * 此方法由业务层实现，从数据库加载用户及其角色权限
     *
     * @param username 用户名
     * @param tenantId 租户ID（可选）
     * @return 登录用户信息
     */
    LoginUser loadUserByUsername(String username, Long tenantId);

    /**
     * 根据手机号加载用户信息
     *
     * @param phone    手机号
     * @param tenantId 租户ID（可选）
     * @return 登录用户信息
     */
    default LoginUser loadUserByPhone(String phone, Long tenantId) {
        throw new UnsupportedOperationException("不支持手机号登录");
    }

    /**
     * 根据邮箱加载用户信息
     *
     * @param email    邮箱
     * @param tenantId 租户ID（可选）
     * @return 登录用户信息
     */
    default LoginUser loadUserByEmail(String email, Long tenantId) {
        throw new UnsupportedOperationException("不支持邮箱登录");
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册后的用户信息
     */
    LoginUser register(RegisterRequest request);

    /**
     * 修改密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean changePassword(String oldPassword, String newPassword);

    /**
     * 重置密码（通过验证码）
     *
     * @param username    用户名
     * @param newPassword 新密码
     * @param code        验证码
     * @param codeKey     验证码key
     * @return 是否成功
     */
    boolean resetPassword(String username, String newPassword, String code, String codeKey);

    /**
     * 获取验证码
     *
     * @return 验证码信息
     */
    CaptchaResult getCaptcha();

    /**
     * 刷新Token
     *
     * @return 新的登录结果
     */
    LoginResult refreshToken();

    /**
     * 验证密码
     *
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean matchPassword(String rawPassword, String encodedPassword);

    /**
     * 验证验证码
     *
     * @param codeKey 验证码key
     * @param code 验证码
     * @return 是否正确
     */
    boolean validateCode(String codeKey, String code);

    /**
     * 验证手机验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否正确
     */
    default boolean validatePhoneCode(String phone, String code) {
        throw new UnsupportedOperationException("不支持手机验证码验证");
    }

    /**
     * 获取滑块验证码
     *
     * @return 滑块验证码信息
     */
    default SliderCaptchaResult getSliderCaptcha() {
        throw new UnsupportedOperationException("不支持滑块验证码");
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 发送结果
     */
    default SmsCaptchaResult sendSmsCaptcha(String phone) {
        throw new UnsupportedOperationException("不支持短信验证码");
    }

    /**
     * 获取登录配置
     *
     * @return 登录配置
     */
    default LoginConfigResult getLoginConfig() {
        throw new UnsupportedOperationException("不支持获取登录配置");
    }
}
