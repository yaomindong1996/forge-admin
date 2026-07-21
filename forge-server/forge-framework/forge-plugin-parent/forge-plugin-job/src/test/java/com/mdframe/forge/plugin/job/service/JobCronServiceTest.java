package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.dto.JobCronPreviewRequest;
import com.mdframe.forge.plugin.job.vo.JobCronPreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobCronServiceTest {

    private final JobCronService service = new JobCronService(new JobScheduleDomainService());

    @Test
    void shouldPreviewFiveDailyFireTimesWithFixedClock() {
        JobCronPreviewRequest request = new JobCronPreviewRequest();
        request.setCronExpression("0 0 2 * * ?");
        request.setTimezone("UTC");
        Clock clock = Clock.fixed(Instant.parse("2026-07-19T00:00:00Z"), ZoneId.of("UTC"));

        JobCronPreviewVO result = service.preview(request, clock);

        assertEquals("每天 02:00 执行", result.getDescription());
        assertEquals("UTC", result.getTimezone());
        assertEquals(5, result.getNextFireTimes().size());
        assertEquals(LocalDateTime.of(2026, 7, 19, 2, 0), result.getNextFireTimes().get(0));
        assertEquals(LocalDateTime.of(2026, 7, 23, 2, 0), result.getNextFireTimes().get(4));
    }

    @Test
    void shouldPreviewInRequestedShanghaiTimezone() {
        JobCronPreviewRequest request = new JobCronPreviewRequest();
        request.setCronExpression("0 0 2 * * ?");
        request.setTimezone("Asia/Shanghai");
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T17:00:00Z"), ZoneId.of("UTC"));

        JobCronPreviewVO result = service.preview(request, clock);

        assertEquals(LocalDateTime.of(2026, 7, 19, 2, 0), result.getNextFireTimes().get(0));
        assertEquals("Asia/Shanghai", result.getTimezone());
    }

    @Test
    void shouldDescribeSupportedSimpleSchedules() {
        assertEquals("每 10 分钟执行", service.describe("0 0/10 * * * ?"));
        assertEquals("每小时第 15 分钟执行", service.describe("0 15 * * * ?"));
        assertEquals("每周一 09:30 执行", service.describe("0 30 9 ? * MON"));
        assertEquals("每月 5 日 18:00 执行", service.describe("0 0 18 5 * ?"));
        assertEquals("自定义执行计划", service.describe("0 0 9-18 ? * MON-FRI"));
    }

    @Test
    void shouldReturnBusinessMessageForInvalidCron() {
        JobCronPreviewRequest request = new JobCronPreviewRequest();
        request.setCronExpression("invalid cron");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.preview(request, Clock.systemUTC()));

        assertEquals("Cron表达式格式不正确，请检查后重试", exception.getMessage());
    }
}
