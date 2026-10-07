package com.mdframe.forge.plugin.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;

/** 配置仅由部署方提供；不接受请求租户，也不保存明文机器凭证。 */
@Data
@ConfigurationProperties("forge.plugin-build.worker")
public class PluginWorkerProperties {
    private boolean enabled;
    private String id;
    private Long tenantId;
    private String tokenSha256;
    private Instant expiresAt;

    public boolean usable(Instant now) {
        return enabled && id != null && id.matches("[a-z0-9][a-z0-9_-]{0,63}")
                && tenantId != null && tenantId > 0 && tokenSha256 != null
                && tokenSha256.matches("[a-f0-9]{64}") && expiresAt != null && expiresAt.isAfter(now);
    }
}
