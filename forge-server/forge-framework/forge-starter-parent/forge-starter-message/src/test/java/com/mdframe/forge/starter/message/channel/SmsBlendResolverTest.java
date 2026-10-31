package com.mdframe.forge.starter.message.channel;

import org.dromara.sms4j.api.SmsBlend;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SmsBlendResolverTest {

    @Test
    void missingBlendDoesNotFallBackToAnotherChannel() {
        SmsBlend otherTenant = mock(SmsBlend.class);
        SmsBlend registered = mock(SmsBlend.class);
        AtomicInteger ensures = new AtomicInteger();

        SmsBlend resolved = SmsBlendResolver.resolve("2:alibaba", runtimeId -> {
            if (!"2:alibaba".equals(runtimeId)) {
                return otherTenant;
            }
            return ensures.get() > 0 ? registered : null;
        }, ensures::incrementAndGet);

        assertSame(registered, resolved);
        assertEquals(1, ensures.get());
    }

    @Test
    void stillMissingAfterEnsureReturnsNull() {
        SmsBlend otherTenant = mock(SmsBlend.class);
        SmsBlend resolved = SmsBlendResolver.resolve("2:tencent", runtimeId -> {
            if ("1:alibaba".equals(runtimeId)) {
                return otherTenant;
            }
            return null;
        }, () -> { });

        assertNull(resolved);
    }
}
