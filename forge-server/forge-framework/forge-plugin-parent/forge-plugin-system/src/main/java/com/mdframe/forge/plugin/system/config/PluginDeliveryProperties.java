package com.mdframe.forge.plugin.system.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Instant;
import java.util.List;
@ConfigurationProperties("forge.plugin-delivery")
@Data
public class PluginDeliveryProperties {
    private boolean enabled;
    private Worker worker = new Worker();
    private List<Target> targets = List.of();
    @Data public static class Worker {
        private String id;
        private Long tenantId;
        private String tokenSha256;
        private Instant expiresAt;
    }
    @Data public static class Target {
        private String id;
        private String name;
        private String repositoryId;
    }
    public boolean usable(Instant now) {
        return enabled && worker != null && targets != null && validTargets()
                && worker.getId() != null && worker.getId().matches("[a-z0-9][a-z0-9_-]{0,63}")
                && worker.getTenantId() != null && worker.getTenantId() > 0
                && worker.getTokenSha256() != null && worker.getTokenSha256().matches("[a-f0-9]{64}")
                && worker.getExpiresAt() != null && worker.getExpiresAt().isAfter(now);
    }
    private boolean validTargets() {
        return !targets.isEmpty() && targets.stream().allMatch(value -> value != null
                && value.getId() != null && value.getId().matches("[a-z][a-z0-9-]{0,63}")
                && value.getRepositoryId() != null && value.getRepositoryId().matches("[a-z][a-z0-9-]{0,63}")
                && value.getName() != null && !value.getName().isBlank() && value.getName().length() <= 100)
                && targets.stream().map(Target::getId).distinct().count() == targets.size();
    }
    public Target target(String id, Long tenantId) {
        if (!usable(Instant.now()) || !worker.getTenantId().equals(tenantId)) {
            throw new com.mdframe.forge.starter.core.exception.BusinessException(503, "交付执行器未启用或已到期");
        }
        return targets.stream().filter(value -> value.getId() != null && value.getId().equals(id)
                && value.getId().matches("[a-z][a-z0-9-]{0,63}") && value.getRepositoryId() != null
                && value.getRepositoryId().matches("[a-z][a-z0-9-]{0,63}"))
                .findFirst().orElseThrow(() -> new com.mdframe.forge.starter.core.exception.BusinessException(
                        403, "目标未在部署配置中授权"));
    }
}
