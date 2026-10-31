package com.mdframe.forge.starter.message.sms;

import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmsRouterTenantTest {

    @Test
    void weightedRouteUsesChannelTenantWhenHintMissing() {
        AtomicLong seen = new AtomicLong(-1);
        SmsRouteSequence sequence = tenantId -> {
            seen.set(tenantId);
            return 0L;
        };
        SmsRouter router = new SmsRouter(providerWithTenant(9L), sequence);

        router.resolveAttemptOrder("NOTICE", null);

        assertEquals(9L, seen.get());
    }

    private static SmsConfigProvider providerWithTenant(Long tenantId) {
        return new SmsConfigProvider() {
            @Override
            public SmsConfig getSmsConfig() {
                return null;
            }

            @Override
            public List<SmsConfig> listEnabledSmsConfigs() {
                return List.of(channel("a", tenantId), channel("b", tenantId));
            }
        };
    }

    private static SmsConfig channel(String configId, Long tenantId) {
        SmsConfig config = new SmsConfig();
        config.setConfigId(configId);
        config.setTenantId(tenantId);
        config.setWeight(1);
        config.setChannelRole("SECONDARY");
        return config;
    }
}
