package com.mdframe.forge.starter.social.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.social.domain.dto.SocialAuthUrl;
import com.mdframe.forge.starter.social.domain.dto.SocialLoginRequest;
import com.mdframe.forge.starter.social.domain.dto.SocialPlatformInfo;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.enums.SocialPlatform;
import com.mdframe.forge.starter.social.factory.SocialAuthRequestFactory;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 三方登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final ISocialConfigService socialConfigService;
    private final SocialAuthRequestFactory authRequestFactory;

    /**
     * 获取已启用的三方登录平台列表
     */
    @GetMapping("/platforms")
    @IgnoreTenant
    @SaIgnore
    public RespInfo<List<SocialPlatformInfo>> getPlatforms(@RequestParam(required = false) Long tenantId) {
        List<SocialPlatformInfo> platforms = socialConfigService.selectEnabledPlatforms(tenantId);
        return RespInfo.success(platforms);
    }

    /**
     * 获取三方登录授权链接
     */
    @GetMapping("/authUrl/{platform}")
    @IgnoreTenant
    @SaIgnore
    public RespInfo<SocialAuthUrl> getAuthUrl(@PathVariable String platform,
                                               @RequestParam(required = false) Long tenantId) {
        SysSocialConfig config = socialConfigService.selectByPlatformAndTenant(platform, tenantId);
        if (config == null || config.getStatus() != 1) {
            return RespInfo.error("该平台登录未启用");
        }

        AuthRequest authRequest = authRequestFactory.createRequest(config);
        // 将 platform 编码到 state 中，格式：platform_randomUUID
        String state = platform + "_" + IdUtil.fastSimpleUUID();
        String authUrl = authRequest.authorize(state);

        SocialAuthUrl result = SocialAuthUrl.builder()
                .platform(platform)
                .platformName(config.getPlatformName())
                .authUrl(authUrl)
                .state(state)
                .build();

        return RespInfo.success(result);
    }

    /**
     * 三方登录回调（直接返回AuthUser供前端处理）
     */
    @PostMapping("/callback")
    @IgnoreTenant
    @SaIgnore
    public RespInfo<AuthUser> callback(@RequestBody SocialLoginRequest request) {
        log.info("三方登录回调参数:{}",request);
        SysSocialConfig config = socialConfigService.selectByPlatformAndTenant(request.getPlatform(), request.getTenantId());
        if (config == null || config.getStatus() != 1) {
            return RespInfo.error("该平台登录未启用");
        }

        AuthRequest authRequest = authRequestFactory.createRequest(config);
        AuthCallback callback = AuthCallback.builder()
                .code(request.getCode())
                .state(request.getState())
                .build();

        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (!response.ok()) {
            log.error("三方登录失败: platform={}, msg={}", request.getPlatform(), response.getMsg());
            return RespInfo.error(response.getMsg());
        }
        
        log.info("获取三方用户信息成功:{}", JSONObject.toJSONString(response.getData()));

        return RespInfo.success(response.getData());
    }
}
