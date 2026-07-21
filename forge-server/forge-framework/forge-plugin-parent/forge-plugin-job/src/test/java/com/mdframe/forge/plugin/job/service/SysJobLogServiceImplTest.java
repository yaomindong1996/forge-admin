package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.dto.JobLogQuery;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.service.impl.SysJobLogServiceImpl;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysJobLogServiceImplTest {

    @Test
    void shouldValidateRetentionDaysBeforeCleaning() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobLogServiceImpl service = service(mapper);

        assertThrows(BusinessException.class, () -> service.cleanLog(-1));
        assertThrows(BusinessException.class, () -> service.cleanLog(3651));
    }

    @Test
    void shouldCleanTerminalLogsBeforeRetentionBoundary() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.cleanPhysicalBefore(org.mockito.ArgumentMatchers.any())).thenReturn(7);
        SysJobLogServiceImpl service = service(mapper);

        int cleaned = service.cleanLog(30);

        assertEquals(7, cleaned);
        verify(mapper).cleanPhysicalBefore(org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void shouldConvertDynamicExportParametersIntoTypedQuery() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        SysJobLogServiceImpl service = service(mapper);

        service.selectExportList(Map.of(
                "jobConfigId", "7",
                "jobName", " inventoryClose ",
                "status", 0,
                "triggerType", "MANUAL",
                "startTime", "2026-07-20 08:30:00",
                "endTime", "2026-07-20T09:45:30"));

        ArgumentCaptor<JobLogQuery> captor = ArgumentCaptor.forClass(JobLogQuery.class);
        verify(mapper).selectExportList(captor.capture());
        JobLogQuery query = captor.getValue();
        assertEquals(7L, query.getJobConfigId());
        assertEquals("inventoryClose", query.getJobName());
        assertEquals(0, query.getStatus());
        assertEquals("MANUAL", query.getTriggerType());
        assertEquals(LocalDateTime.of(2026, 7, 20, 8, 30), query.getStartTime());
        assertEquals(LocalDateTime.of(2026, 7, 20, 9, 45, 30), query.getEndTime());
    }

    @Test
    void shouldSanitizeLegacyDetailSummariesBeforeReturning() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        JobLogDetailVO detail = new JobLogDetailVO();
        detail.setResultSummary("Authorization: Bearer raw-token");
        detail.setExceptionSummary("password=raw-password");
        when(mapper.selectLogDetail(9L)).thenReturn(detail);

        JobLogDetailVO result = service(mapper).selectLogDetail(9L);

        assertFalse(result.getResultSummary().contains("raw-token"));
        assertFalse(result.getExceptionSummary().contains("raw-password"));
    }

    private SysJobLogServiceImpl service(SysJobLogMapper mapper) {
        SysJobLogServiceImpl service = new SysJobLogServiceImpl(
                new JobLogSanitizer(new ObjectMapper()), mock(JobManagementSecurityService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }
}
