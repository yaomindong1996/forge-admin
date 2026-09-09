package com.mdframe.forge.plugin.system.strategy;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.auth.LoginPasswordDecoder;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.enums.AuthType;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户名+密码+验证码认证策略
 * 支持多种验证码类型：图形验证码、滑块验证码、短信验证码
 * 验证码类型由全局登录配置和客户端覆盖配置统一解析
 */
@Component
public class UsernamePasswordCaptchaAuthStrategy extends AbstractAuthStrategy {

    @Autowired
    private LoginPasswordDecoder loginPasswordDecoder;

    @Autowired
    private ICaptchaService captchaService;

    @Autowired
    private LoginCaptchaPolicyResolver captchaPolicyResolver;

    @Override
    protected void validateRequest(LoginRequest request) {
        validateUsername(request.getUsername());
        validatePassword(request.getPassword());

        // 根据验证码类型校验不同参数
        LoginCaptchaPolicy captchaPolicy = captchaPolicyResolver.resolve(request.getUserClient());
        if (!Boolean.TRUE.equals(captchaPolicy.getEnableCaptcha())) {
            return;
        }

        String captchaType = captchaPolicy.getCaptchaType();

        if ("sms".equals(captchaType)) {
            // 短信验证码需要手机号和验证码
            if (StrUtil.isBlank(request.getPhone())) {
                throw new RuntimeException("手机号不能为空");
            }
            if (StrUtil.isBlank(request.getCode())) {
                throw new RuntimeException("验证码不能为空");
            }
        } else if ("slider".equals(captchaType)) {
            if (StrUtil.isBlank(request.getCode()) || StrUtil.isBlank(request.getCodeKey())) {
                throw new RuntimeException("请完成滑块验证");
            }
        } else {
            // 图形验证码需要codeKey和code
            validateCaptcha(request.getCode(), request.getCodeKey());
        }
    }

    @Override
    protected LoginUser doAuthenticate(LoginRequest request) {
        String username = request.getUsername();

        // 1. 获取登录配置，确定验证码类型
        LoginCaptchaPolicy captchaPolicy = captchaPolicyResolver.resolve(request.getUserClient());

        // 2. 根据验证码类型进行验证
        if (Boolean.TRUE.equals(captchaPolicy.getEnableCaptcha())) {
            boolean captchaValid = validateCaptchaByType(request, captchaPolicy.getCaptchaType());
            if (!captchaValid) {
                throw new BusinessException("验证码错误或已过期");
            }
        }

        String rawPassword = loginPasswordDecoder.decode(request.getPassword());
        LoginUser loginUser = userLoadService.authenticateByUsernamePassword(
                username, rawPassword, request.getTenantId());
        checkAccountLocked(loginUser);
        if (loginUser == null) {
            recordLoginFailure(null, "用户名或密码错误");
        }
        return loginUser;
    }

    /**
     * 根据验证码类型进行验证
     *
     * @param request     登录请求
     * @param captchaType 验证码类型
     * @return 是否验证通过
     */
    private boolean validateCaptchaByType(LoginRequest request, String captchaType) {
        if (captchaType == null) {
            captchaType = "graphical"; // 默认图形验证码
        }

        switch (captchaType) {
            case "slider":
                try {
                    return captchaService.validateAndDeleteSliderCaptcha(
                            request.getCodeKey(), Integer.valueOf(request.getCode()));
                } catch (NumberFormatException exception) {
                    return false;
                }
            case "sms":
                // 短信验证码：使用手机号验证
                return captchaService.validateAndDeleteSmsCaptcha(request.getPhone(), request.getCode());
            case "graphical":
            default:
                // 图形验证码
                return userLoadService.validateCode(request.getCodeKey(), request.getCode());
        }
    }

    @Override
    public String getAuthType() {
        return AuthType.PASSWORD_CAPTCHA.getCode();
    }
}
