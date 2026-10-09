package com.mdframe.forge.plugin.system.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Instant;
@Data @ConfigurationProperties("forge.plugin-runtime-probe")
public class PluginRuntimeProbeProperties {
    private boolean enabled;
    private boolean allowLoopback;
    private String tokenSha256;
    private Instant expiresAt;
    private String jarPath = "/opt/forge/admin.jar";
    public boolean usable() {
        return enabled && tokenSha256 != null && tokenSha256.matches("[a-f0-9]{64}")
                && expiresAt != null && expiresAt.isAfter(Instant.now());
    }
}
