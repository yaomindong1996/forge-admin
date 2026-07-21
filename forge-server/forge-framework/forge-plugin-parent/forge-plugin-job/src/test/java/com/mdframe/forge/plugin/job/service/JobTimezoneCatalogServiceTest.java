package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.vo.JobTimezoneOptionVO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobTimezoneCatalogServiceTest {

    private final JobTimezoneCatalogService service = new JobTimezoneCatalogService();

    @Test
    void shouldListJdkTimezonesWithPreferredDefaultFirst() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-19T00:00:00Z"), ZoneId.of("UTC"));

        List<JobTimezoneOptionVO> options = service.listTimezones(clock);

        assertEquals("Asia/Shanghai", options.get(0).getValue());
        assertEquals("UTC+08:00", options.get(0).getOffset());
        assertEquals(ZoneId.getAvailableZoneIds().size(), options.size());
        assertTrue(options.stream().anyMatch(option -> "UTC".equals(option.getValue())
                && "UTC+00:00".equals(option.getOffset())));
        assertTrue(options.stream().anyMatch(option -> "America/New_York".equals(option.getValue())
                && "UTC-05:00".equals(option.getOffset())));
    }
}
