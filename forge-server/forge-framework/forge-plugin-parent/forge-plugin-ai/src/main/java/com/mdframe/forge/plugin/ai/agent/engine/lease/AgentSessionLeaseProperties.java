package com.mdframe.forge.plugin.ai.agent.engine.lease;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "forge.agent.session-lease")
public class AgentSessionLeaseProperties {
    private Duration ttl = Duration.ofSeconds(60);
    private Duration heartbeatInterval = Duration.ofSeconds(10);

    public void validate() {
        if (ttl == null || heartbeatInterval == null || ttl.toMillis() < 10000 || ttl.toMillis() > 300000
                || heartbeatInterval.toMillis() < 1000 || heartbeatInterval.toMillis() > ttl.toMillis() / 3) {
            throw new IllegalArgumentException("Agent 租约需为 10–300 秒，心跳需为 1 秒至 TTL 的三分之一");
        }
    }
}
