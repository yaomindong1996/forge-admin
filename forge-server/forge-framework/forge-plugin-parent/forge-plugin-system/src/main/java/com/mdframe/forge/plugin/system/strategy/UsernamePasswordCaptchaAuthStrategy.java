package com.mdframe.forge.plugin.system.strategy;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicy;
import com.mdframe.forge.plugin.system.auth.LoginCaptchaPolicyResolver;
import com.mdframe.forge.plugin.system.auth.LoginPasswordDecoder;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.enums.AuthType;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

/**
 * 用户名+密码+验证码认证策略
 * 支持多种验证码类型：图形验证码、滑块验证码、短信验证码、群二维码验证码
 * 验证码类型由全局登录配置和客户端覆盖配置统一解析
 */
@Slf4j
@Component
public class UsernamePasswordCaptchaAuthStrategy extends AbstractAuthStrategy {

    @Autowired
    private LoginPasswordDecoder loginPasswordDecoder;

    @Autowired
    private ICaptchaService captchaService;

    @Autowired
    private LoginCaptchaPolicyResolver captchaPolicyResolver;

    @Autowired
    private com.mdframe.forge.starter.config.service.ConfigManagerService configManagerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void validateRequest(LoginRequest request) {
        validateUsername(request.getUsername());
        validatePassword(request.getPassword());

        // 根据验证码类型校验不同参数
        LoginCaptchaPolicy captchaPolicy = captchaPolicyResolver.resolve(request.getUserClient());
        String captchaType = resolveEffectiveCaptchaType(request, captchaPolicy);
        if (!isCaptchaRequired(captchaPolicy, captchaType)) {
            return;
        }

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
        } else if ("group".equals(captchaType)) {
            // 群二维码验证码：只需要 code（codeKey 前端传固定值 "group_captcha"）
            if (StrUtil.isBlank(request.getCode())) {
                throw new RuntimeException("请输入群验证码");
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
        String effectiveType = resolveEffectiveCaptchaType(request, captchaPolicy);

        // 2. 根据验证码类型进行验证（群二维码引流验证码独立于验证码总开关生效）
        if (isCaptchaRequired(captchaPolicy, effectiveType)) {
            boolean captchaValid = validateCaptchaByType(request, effectiveType);
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
        applyPasswordExpiration(loginUser);
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
            case "group":
                // 群二维码验证码：比对配置中心固定码（不区分大小写，trim）
                return validateGroupCaptcha(request.getCode());
            case "graphical":
            default:
                // 图形验证码
                return userLoadService.validateCode(request.getCodeKey(), request.getCode());
        }
    }

    /**
     * 解析实际生效的验证码类型。
     * 当群二维码引流已启用且前端明确传了 captchaType="group" 时，以前端选择为准；
     * 否则使用后端配置的默认类型。
     */
    private String resolveEffectiveCaptchaType(LoginRequest request, LoginCaptchaPolicy captchaPolicy) {
        String configuredType = captchaPolicy.getCaptchaType();
        String requestType = request.getCaptchaType();
        if ("group".equals(requestType) && Boolean.TRUE.equals(captchaPolicy.getGroupQrcodeEnabled())) {
            return "group";
        }
        return configuredType;
    }

    /**
     * 判断本次登录是否需要校验验证码。
     * 验证码总开关开启时按解析出的类型校验；
     * 总开关关闭时，仅群二维码引流验证码独立生效。
     */
    private boolean isCaptchaRequired(LoginCaptchaPolicy captchaPolicy, String captchaType) {
        if (Boolean.TRUE.equals(captchaPolicy.getEnableCaptcha())) {
            return true;
        }
        return "group".equals(captchaType) && Boolean.TRUE.equals(captchaPolicy.getGroupQrcodeEnabled());
    }

    /**
     * 校验社区固定验证码：优先走官网 Website 配置（群/公众号共用），失败或未配置时回退本地 LoginConfig。
     */
    private boolean validateGroupCaptcha(String code) {
        if (StrUtil.isBlank(code)) {
            return false;
        }
        try {
            com.mdframe.forge.starter.config.config.LoginConfig loginConfig =
                    configManagerService.getLoginConfig();
            if (loginConfig == null) {
                log.warn("登录配置为空，拒绝群验证码登录");
                return false;
            }
            Boolean remote = verifyRemoteCommunityCaptcha(code, loginConfig.getGroupQrcodeBoardUrl());
            if (remote != null) {
                return remote;
            }
            if (StrUtil.isBlank(loginConfig.getGroupCaptchaCode())) {
                log.warn("群验证码未配置或为空，拒绝登录");
                return false;
            }
            return code.trim().equalsIgnoreCase(loginConfig.getGroupCaptchaCode().trim());
        } catch (Exception e) {
            log.error("校验群验证码失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 调用官网 /api/community/login-captcha/verify；无法推导或请求异常时返回 null（回退本地码）。
     */
    private Boolean verifyRemoteCommunityCaptcha(String code, String boardUrl) {
        String verifyUrl = resolveCaptchaVerifyUrl(boardUrl);
        if (StrUtil.isBlank(verifyUrl)) {
            return null;
        }
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(2000);
            factory.setReadTimeout(3000);
            RestClient client = RestClient.builder().requestFactory(factory).build();
            String body = client.post()
                    .uri(URI.create(verifyUrl))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("code", code.trim()))
                    .retrieve()
                    .body(String.class);
            if (StrUtil.isBlank(body)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data == null || data.isNull()) {
                return null;
            }
            if (data.has("configured") && !data.get("configured").asBoolean(true)) {
                return null;
            }
            if (data.has("valid")) {
                return data.get("valid").asBoolean(false);
            }
            return null;
        } catch (Exception ex) {
            log.warn("官网社区验证码校验失败，回退本地固定码: {}", ex.toString());
            return null;
        }
    }

    private String resolveCaptchaVerifyUrl(String boardUrl) {
        if (StrUtil.isBlank(boardUrl)) {
            return null;
        }
        String value = boardUrl.trim();
        int apiIdx = value.indexOf("/api/community/wechat-groups");
        if (apiIdx >= 0) {
            return value.substring(0, apiIdx) + "/api/community/login-captcha/verify";
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        int slash = value.lastIndexOf('/');
        if (slash > 0 && value.substring(slash + 1).contains("wechat-groups")) {
            return value.substring(0, slash) + "/login-captcha/verify";
        }
        return null;
    }

    @Override
    public String getAuthType() {
        return AuthType.PASSWORD_CAPTCHA.getCode();
    }
}
