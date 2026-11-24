package com.mdframe.forge.starter.social.factory;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.social.context.SocialProperties;
import com.mdframe.forge.starter.social.domain.entity.SysSocialAppConfig;
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
import java.util.List;
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
     * 根据配置创建AuthRequest（兼容期：使用连接旧明文 Secret，结果缓存）
     */
    public AuthRequest createRequest(SysSocialConfig config) {
        return createRequest(config, (SysSocialAppConfig) null);
    }

    /**
     * 使用 LOGIN 应用的非敏感 OAuth 参数创建AuthRequest（Secret 仅兼容连接旧字段）。
     * <p>
     * 应用维度参数优先，连接维度作为兼容期回退，结果按连接+应用维度缓存。
     * 应用 Secret 存储在密文字段时，调用方必须先解密并使用三参数重载，不能把密文传入本方法。
     */
    public AuthRequest createRequest(SysSocialConfig config, SysSocialAppConfig app) {
        String cacheKey = buildCacheKey(config, app);
        return requestCache.computeIfAbsent(cacheKey, key -> buildRequest(config, app, null));
    }

    /**
     * 使用显式 Secret 创建AuthRequest（LOGIN 应用解密凭据）。
     * <p>
     * 不进缓存，避免明文 Secret 长期驻留与轮换后使用旧值。
     */
    public AuthRequest createRequest(SysSocialConfig config, char[] explicitSecret) {
        return createRequest(config, null, explicitSecret);
    }

    /**
     * 使用 LOGIN 应用参数与显式 Secret 创建AuthRequest。
     * <p>
     * clientId/redirectUri/scope/agentId 均按「应用优先、连接回退」解析，
     * 使纯 OAuth 登录平台（Gitee/GitHub 等）与企业微信共用同一套连接+应用配置模型。
     */
    public AuthRequest createRequest(SysSocialConfig config, SysSocialAppConfig app, char[] explicitSecret) {
        if (explicitSecret == null || explicitSecret.length == 0) {
            return createRequest(config, app);
        }
        return buildRequest(config, app, new String(explicitSecret));
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        requestCache.clear();
        log.info("三方登录请求缓存已清除");
    }

    /**
     * 清除指定配置的缓存（含该连接下所有应用维度的缓存项）
     */
    public void clearCache(SysSocialConfig config) {
        String prefix = buildCacheKeyPrefix(config);
        requestCache.keySet().removeIf(key -> key.startsWith(prefix));
        log.info("三方登录请求缓存已清除: {}*", prefix);
    }

    private String buildCacheKeyPrefix(SysSocialConfig config) {
        return config.getPlatform() + ":" + config.getTenantId() + ":" + config.getId() + ":";
    }

    private String buildCacheKey(SysSocialConfig config, SysSocialAppConfig app) {
        // 连接ID 维度隔离缓存，避免同平台多连接互相覆盖；应用维度参与 key，避免换绑 LOGIN 应用后命中旧参数
        String appPart = app != null && app.getId() != null ? String.valueOf(app.getId()) : "-";
        return buildCacheKeyPrefix(config) + appPart;
    }

    private AuthRequest buildRequest(SysSocialConfig config, SysSocialAppConfig app, String secretOverride) {
        AuthConfig authConfig = buildAuthConfig(config, app, secretOverride);
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

    private AuthConfig buildAuthConfig(SysSocialConfig config, SysSocialAppConfig app, String secretOverride) {
        String clientId = firstNotBlank(app == null ? null : app.getClientId(), config.getClientId());
        String redirectUri = firstNotBlank(app == null ? null : app.getRedirectUri(), config.getRedirectUri());
        String scope = firstNotBlank(app == null ? null : app.getScope(), config.getScope());
        String agentId = firstNotBlank(app == null ? null : app.getAgentId(), config.getAgentId());

        AuthConfig.AuthConfigBuilder builder = AuthConfig.builder()
                .clientId(clientId)
                .clientSecret(StrUtil.isNotBlank(secretOverride) ? secretOverride : config.getClientSecret());

        if (StrUtil.isNotBlank(redirectUri)) {
            builder.redirectUri(redirectUri);
        } else if (StrUtil.isNotBlank(socialProperties.getCallbackPrefix())) {
            builder.redirectUri(socialProperties.getCallbackPrefix() + "/" + config.getPlatform().toLowerCase() + "/callback");
        }

        if (StrUtil.isNotBlank(scope)) {
            // Gitee 社区登录需 user_info,projects；允许配置里带空格
            List<String> scopes = Arrays.stream(scope.split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .toList();
            if (!scopes.isEmpty()) {
                builder.scopes(scopes);
            }
        }

        if (StrUtil.isNotBlank(agentId)) {
            builder.agentId(agentId);
        }

        return builder.build();
    }

    /**
     * 应用维度参数优先，为空时回退连接维度旧字段
     */
    private String firstNotBlank(String preferred, String fallback) {
        return StrUtil.isNotBlank(preferred) ? preferred : fallback;
    }
}
