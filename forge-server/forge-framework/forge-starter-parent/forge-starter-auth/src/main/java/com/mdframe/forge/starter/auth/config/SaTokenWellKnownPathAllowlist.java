package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.fun.strategy.SaCheckRequestPathFunction;
import cn.dev33.satoken.strategy.SaStrategy;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 1.38 默认路径防火墙会拦截包含 {@code /.} 的 URI（防目录穿越），
 * 导致 RFC 合规的 {@code /.well-known/oauth-*} 元数据接口被写成「非法请求」。
 * 这里仅放行能力开放 OAuth 元数据路径，其它路径仍走原校验。
 */
@Configuration
public class SaTokenWellKnownPathAllowlist {

    @PostConstruct
    void allowOauthWellKnownMetadataPaths() {
        SaCheckRequestPathFunction original = SaStrategy.instance.checkRequestPath;
        SaStrategy.instance.checkRequestPath = (requestPath, request, response) -> {
            if (isOauthWellKnownMetadata(requestPath)) {
                return;
            }
            original.run(requestPath, request, response);
        };
    }

    static boolean isOauthWellKnownMetadata(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return false;
        }
        return "/.well-known/oauth-protected-resource".equals(requestPath)
                || "/.well-known/oauth-authorization-server".equals(requestPath);
    }
}
