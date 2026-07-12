package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityAccessTokenMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 短期令牌是运行时安全材料，不属于用户可恢复主数据。仅在撤销/过期且超过留存期后物理清理，
 * 降低 HMAC 元数据长期堆积风险；业务审计由独立调用审计表保留。
 */
@Component
@ConditionalOnProperty(prefix = "forge.capability.identity", name = "enabled", havingValue = "true")
public class CapabilityAccessTokenRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(CapabilityAccessTokenRetentionJob.class);

    private final AiCapabilityAccessTokenMapper tokenMapper;
    private final CapabilityIdentityProperties properties;
    private final Clock clock;

    public CapabilityAccessTokenRetentionJob(
            AiCapabilityAccessTokenMapper tokenMapper,
            CapabilityIdentityProperties properties,
            @Qualifier("capabilityClock") Clock clock) {
        this.tokenMapper = tokenMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${forge.capability.identity.token-cleanup-interval-ms:21600000}",
            initialDelayString = "${forge.capability.identity.token-cleanup-initial-delay-ms:300000}")
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredHistory() {
        LocalDateTime cutoff = LocalDateTime.now(clock)
                .minus(properties.validatedAccessTokenRetention());
        int purged = tokenMapper.purgeExpiredHistoryBefore(cutoff);
        if (purged > 0) {
            log.info("[MCP令牌留存] resultCode=PURGED, purgedCount={}", purged);
        }
    }
}
