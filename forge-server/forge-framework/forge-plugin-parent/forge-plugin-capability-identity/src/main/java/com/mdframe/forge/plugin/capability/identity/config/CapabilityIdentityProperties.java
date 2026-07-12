package com.mdframe.forge.plugin.capability.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(prefix = "forge.capability.identity")
public class CapabilityIdentityProperties {

    private boolean enabled = true;
    private String issuer = "http://localhost:8580";
    private String resource = "http://localhost:8580/mcp";
    private String tokenPepper;
    private String authorizationCodePepper;
    private Duration accessTokenTtl = Duration.ofMinutes(10);
    private Duration authorizationCodeTtl = Duration.ofMinutes(2);
    private Duration accessTokenRetention = Duration.ofDays(30);
    private Duration lastUsedTouchInterval = Duration.ofMinutes(1);
    private Set<String> allowedOrigins = new LinkedHashSet<>();

    public Duration validatedAccessTokenTtl() {
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()
                || accessTokenTtl.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalStateException("MCP access token TTL 必须大于 0 且不超过 15 分钟");
        }
        return accessTokenTtl;
    }

    public Duration validatedAuthorizationCodeTtl() {
        if (authorizationCodeTtl == null || authorizationCodeTtl.isZero()
                || authorizationCodeTtl.isNegative()
                || authorizationCodeTtl.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalStateException("MCP authorization code TTL 必须大于 0 且不超过 120 秒");
        }
        return authorizationCodeTtl;
    }

    public String validatedIssuer() {
        return validateExternalUri(issuer, false, "issuer");
    }

    public Duration validatedAccessTokenRetention() {
        if (accessTokenRetention == null
                || accessTokenRetention.compareTo(Duration.ofDays(1)) < 0
                || accessTokenRetention.compareTo(Duration.ofDays(365)) > 0) {
            throw new IllegalStateException("MCP access token 留存期必须为 1 到 365 天");
        }
        return accessTokenRetention;
    }

    public Duration validatedLastUsedTouchInterval() {
        if (lastUsedTouchInterval == null || lastUsedTouchInterval.isNegative()
                || lastUsedTouchInterval.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalStateException("MCP last_used_at 写入节流间隔必须为 0 到 5 分钟");
        }
        return lastUsedTouchInterval;
    }

    public String validatedResource() {
        return validateExternalUri(resource, true, "resource");
    }

    private String validateExternalUri(String value, boolean allowPath, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("MCP " + name + " 未配置");
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MCP " + name + " 不是合法 URI", exception);
        }
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost())
                || "127.0.0.1".equals(uri.getHost())
                || "::1".equals(uri.getHost()));
        if (!("https".equalsIgnoreCase(uri.getScheme()) || localHttp)
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (!allowPath && uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
            throw new IllegalStateException("MCP " + name + " 必须是 HTTPS；仅本地开发允许 localhost HTTP");
        }
        String normalized = value.endsWith("/") && !allowPath
                ? value.substring(0, value.length() - 1)
                : value;
        return normalized;
    }
}
