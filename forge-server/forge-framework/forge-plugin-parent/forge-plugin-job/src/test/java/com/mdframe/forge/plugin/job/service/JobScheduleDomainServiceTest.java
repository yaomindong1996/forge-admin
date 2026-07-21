package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobScheduleDomainServiceTest {

    private final JobScheduleDomainService service = new JobScheduleDomainService();

    @Test
    void shouldResolveShanghaiAndUtcLocalTimes() {
        LocalDateTime localTime = LocalDateTime.of(2026, 7, 19, 10, 0);

        assertEquals(Instant.parse("2026-07-19T02:00:00Z"),
                service.resolveOnceInstant(localTime, service.requireZoneId("Asia/Shanghai")));
        assertEquals(Instant.parse("2026-07-19T10:00:00Z"),
                service.resolveOnceInstant(localTime, service.requireZoneId("UTC")));
    }

    @Test
    void shouldRejectNonIanaTimezoneAndDstGap() {
        assertThrows(BusinessException.class, () -> service.requireZoneId("+08:00"));

        ZoneId newYork = service.requireZoneId("America/New_York");
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.resolveOnceInstant(LocalDateTime.of(2026, 3, 8, 2, 30), newYork));

        assertEquals("所选时间在该时区不存在，请避开夏令时切换时段", exception.getMessage());
    }

    @Test
    void shouldUseEarlierOffsetDuringDstOverlap() {
        ZoneId newYork = service.requireZoneId("America/New_York");

        Instant resolved = service.resolveOnceInstant(
                LocalDateTime.of(2026, 11, 1, 1, 30), newYork);

        assertEquals(Instant.parse("2026-11-01T05:30:00Z"), resolved);
    }
}
