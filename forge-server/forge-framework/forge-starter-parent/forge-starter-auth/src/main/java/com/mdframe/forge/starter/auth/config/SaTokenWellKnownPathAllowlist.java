package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.strategy.hooks.SaFirewallCheckHookForWhitePath;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 1.38 曾用 {@code SaStrategy.checkRequestPath} 拦截含 {@code /.} 的 URI，
 * 导致 RFC 合规的 {@code /.well-known/oauth-*} 被写成「非法请求」。
 * 1.45+ 改为 {@code SaFirewallStrategy} 钩子链；这里把能力开放 OAuth 元数据路径
 * 写入官方 {@link SaFirewallCheckHookForWhitePath}（默认链首），命中后跳过后续校验。
 */
@Configuration
public class SaTokenWellKnownPathAllowlist {

    private static final String[] OAUTH_WELL_KNOWN_PATHS = {
            "/.well-known/oauth-protected-resource",
            "/.well-known/oauth-authorization-server"
    };

    @PostConstruct
    void allowOauthWellKnownMetadataPaths() {
        SaFirewallCheckHookForWhitePath.instance.resetConfig(OAUTH_WELL_KNOWN_PATHS);
    }

    static boolean isOauthWellKnownMetadata(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return false;
        }
        for (String path : OAUTH_WELL_KNOWN_PATHS) {
            if (path.equals(requestPath)) {
                return true;
            }
        }
        return false;
    }
}
