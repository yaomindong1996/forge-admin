package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobExecutionRecoveryServiceTest {

    @Test
    void shouldFailOnlyExecutionsOlderThanConfiguredTimeout() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.failStaleExecutions(
                org.mockito.ArgumentMatchers.any(), eq(JobExecutionRecoveryService.RECOVERY_REASON)))
                .thenReturn(3);
        JobProperties properties = new JobProperties();
        properties.setExecutionRecoveryTimeout(Duration.ofMinutes(15));
        JobExecutionRecoveryService service = new JobExecutionRecoveryService(mapper, properties);
        LocalDateTime before = LocalDateTime.now().minusMinutes(15).minusSeconds(1);

        int recovered = service.recoverStaleExecutions();

        LocalDateTime after = LocalDateTime.now().minusMinutes(15).plusSeconds(1);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).failStaleExecutions(cutoff.capture(), eq(JobExecutionRecoveryService.RECOVERY_REASON));
        assertEquals(3, recovered);
        assertTrue(cutoff.getValue().isAfter(before));
        assertTrue(cutoff.getValue().isBefore(after));
    }
}

