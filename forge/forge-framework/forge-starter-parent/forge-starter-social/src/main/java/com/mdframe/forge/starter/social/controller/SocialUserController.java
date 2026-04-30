package com.mdframe.forge.starter.social.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.social.domain.dto.SocialBindRequest;
import com.mdframe.forge.starter.social.domain.dto.SocialPlatformInfo;
import com.mdframe.forge.starter.social.domain.dto.UserSocialBinding;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import com.mdframe.forge.starter.social.factory.SocialAuthRequestFactory;
import com.mdframe.forge.starter.social.service.ISocialConfigService;
import com.mdframe.forge.starter.social.service.ISocialUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialUserController {

    private final ISocialUserService socialUserService;
    private final ISocialConfigService socialConfigService;
    private final SocialAuthRequestFactory authRequestFactory;

    @GetMapping("/user/bindings")
    public RespInfo<List<UserSocialBinding>> getUserBindings(@RequestParam(required = false) Long tenantId) {
        Long userId = SessionHelper.getUserId();
        Long currentTenantId = tenantId != null ? tenantId : SessionHelper.getTenantId();

        List<SocialPlatformInfo> platforms = socialConfigService.selectEnabledPlatforms(currentTenantId);
        List<SysUserSocial> bindings = socialUserService.selectByUserId(userId);

        List<UserSocialBinding> result = platforms.stream().map(p -> {
            SysUserSocial binding = bindings.stream()
                    .filter(b -> b.getPlatform().equalsIgnoreCase(p.getPlatform()))
                    .findFirst().orElse(null);

            return UserSocialBinding.builder()
                    .platform(p.getPlatform())
                    .platformName(p.getPlatformName())
                    .platformLogo(p.getPlatformLogo())
                    .bound(binding != null)
                    .nickname(binding != null ? binding.getNickname() : null)
                    .email(binding != null ? binding.getEmail() : null)
                    .avatar(binding != null ? binding.getAvatar() : null)
                    .bindTime(binding != null ? binding.getBindTime() : null)
                    .build();
        }).collect(Collectors.toList());

        return RespInfo.success(result);
    }

    @PostMapping("/bind")
    public RespInfo<Void> bind(@RequestBody SocialBindRequest request) {
        Long userId = SessionHelper.getUserId();
        Long tenantId = SessionHelper.getTenantId();

        String platform = request.getPlatform();
        SysSocialConfig config = socialConfigService.selectByPlatformAndTenant(platform, tenantId);
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
            log.error("绑定三方账号失败: platform={}, msg={}", platform, response.getMsg());
            return RespInfo.error(response.getMsg());
        }

        boolean bound = socialUserService.bindSocialUser(userId, response.getData(), platform, tenantId);
        if (!bound) {
            return RespInfo.error("该平台已绑定，请勿重复操作");
        }

        log.info("绑定三方账号成功: userId={}, platform={}", userId, platform);
        return RespInfo.success();
    }

    @DeleteMapping("/unbind/{platform}")
    public RespInfo<Void> unbind(@PathVariable String platform) {
        Long userId = SessionHelper.getUserId();
        socialUserService.unbindSocialUser(userId, platform);
        log.info("解绑三方账号成功: userId={}, platform={}", userId, platform);
        return RespInfo.success();
    }
}
