package com.mdframe.forge.plugin.capability.identity.token;

import com.mdframe.forge.plugin.capability.identity.config.CapabilityIdentityProperties;
import com.mdframe.forge.plugin.capability.identity.mapper.AiCapabilityAccessTokenMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CapabilityAccessTokenRetentionJobTest {

    @Test
    void shouldPhysicallyPurgeOnlyHistoryOlderThanRetentionCutoff() {
        AiCapabilityAccessTokenMapper mapper = mock(AiCapabilityAccessTokenMapper.class);
        CapabilityIdentityProperties properties = new CapabilityIdentityProperties();
        CapabilityAccessTokenRetentionJob job = new CapabilityAccessTokenRetentionJob(
                mapper, properties,
                Clock.fixed(Instant.parse("2026-07-12T01:00:00Z"), ZoneOffset.UTC));

        job.purgeExpiredHistory();

        verify(mapper).purgeExpiredHistoryBefore(
                LocalDateTime.of(2026, 6, 12, 1, 0));
    }
}
