package com.mdframe.forge.starter.message.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SmsBlendIdsTest {

    @Test
    void sameBusinessIdIsIsolatedByTenant() {
        assertEquals("1:alibaba", SmsBlendIds.runtimeId(1L, "alibaba"));
        assertEquals("2:alibaba", SmsBlendIds.runtimeId(2L, " alibaba "));
        assertNotEquals(SmsBlendIds.runtimeId(1L, "alibaba"), SmsBlendIds.runtimeId(2L, "alibaba"));
    }

    @Test
    void missingTenantDoesNotShareAnotherTenantsKey() {
        assertEquals("0:main", SmsBlendIds.runtimeId(null, "main"));
        assertNotEquals("1:main", SmsBlendIds.runtimeId(null, "main"));
    }
}
