package com.mdframe.forge.starter.social.factory;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.social.context.SocialProperties;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import com.mdframe.forge.starter.social.enums.SocialPlatform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWeChatOpenRequest;
import me.zhyd.oauth.request.AuthWeChatMpRequest;
import me.zhyd.oauth.request.AuthDingTalkRequest;
import me.zhyd.oauth.request.AuthWeChatEnterpriseQrcodeRequest;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthQqRequest;
import me.zhyd.oauth.request.AuthWeiboRequest;
import me.zhyd.oauth.request.AuthAlipayRequest;
import me.zhyd.oauth.request.AuthBaiduRequest;
import me.zhyd.oauth.request.AuthGoogleRequest;
import me.zhyd.oauth.request.AuthFacebookRequest;
import me.zhyd.oauth.request.AuthTwitterRequest;
import me.zhyd.oauth.request.AuthFeishuRequest;
import me.zhyd.oauth.request.AuthDingTalkAccountRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 三方登录请求工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialAuthRequestFactory {

    private final SocialProperties socialProperties;

    private final Map<String, AuthRequest> requestCache = new ConcurrentHashMap<>();

    /**
     * 根据配置创建AuthRequest
     */
    public AuthRequest createRequest(SysSocialConfig config) {
        String cacheKey = buildCacheKey(config);
        return requestCache.computeIfAbsent(cacheKey, key -> buildRequest(config));
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        requestCache.clear();
        log.info("三方登录请求缓存已清除");
    }

    /**
     * 清除指定配置的缓存
     */
    public void clearCache(SysSocialConfig config) {
        String cacheKey = buildCacheKey(config);
        requestCache.remove(cacheKey);
        log.info("三方登录请求缓存已清除: {}", cacheKey);
    }

    private String buildCacheKey(SysSocialConfig config) {
        return config.getPlatform() + ":" + config.getTenantId();
    }

    private AuthRequest buildRequest(SysSocialConfig config) {
        AuthConfig authConfig = buildAuthConfig(config);
        SocialPlatform platform = SocialPlatform.getByCode(config.getPlatform());

        if (platform == null) {
            throw new IllegalArgumentException("不支持的平台类型: " + config.getPlatform());
        }

        return switch (platform) {
            case WECHAT -> new AuthWeChatOpenRequest(authConfig);
            case WECHAT_MINI -> new AuthWeChatMpRequest(authConfig);
            case DINGTALK -> new AuthDingTalkRequest(authConfig);
            case WECHAT_ENTERPRISE -> new AuthWeChatEnterpriseQrcodeRequest(authConfig);
            case GITHUB -> new AuthGithubRequest(authConfig);
            case GITEE -> new AuthGiteeRequest(authConfig);
            case QQ -> new AuthQqRequest(authConfig);
            case WEIBO -> new AuthWeiboRequest(authConfig);
            case ALIPAY -> new AuthAlipayRequest(authConfig);
            case BAIDU -> new AuthBaiduRequest(authConfig);
            case GOOGLE -> new AuthGoogleRequest(authConfig);
            case FACEBOOK -> new AuthFacebookRequest(authConfig);
            case TWITTER -> new AuthTwitterRequest(authConfig);
            case FEISHU -> new AuthFeishuRequest(authConfig);
            case DINGTALK_ACCOUNT -> new AuthDingTalkAccountRequest(authConfig);
            default -> throw new IllegalArgumentException("不支持的平台类型: " + config.getPlatform());
        };
    }

    private AuthConfig buildAuthConfig(SysSocialConfig config) {
        AuthConfig.AuthConfigBuilder builder = AuthConfig.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret());

        if (StrUtil.isNotBlank(config.getRedirectUri())) {
            builder.redirectUri(config.getRedirectUri());
        } else if (StrUtil.isNotBlank(socialProperties.getCallbackPrefix())) {
            builder.redirectUri(socialProperties.getCallbackPrefix() + "/" + config.getPlatform().toLowerCase() + "/callback");
        }

        if (StrUtil.isNotBlank(config.getScope())) {
            builder.scopes(Arrays.asList(config.getScope().split(",")));
        }

        if (StrUtil.isNotBlank(config.getAgentId())) {
            builder.agentId(config.getAgentId());
        }

        return builder.build();
    }
}
