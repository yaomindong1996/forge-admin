package com.mdframe.forge.plugin.ai.invocation.job;

import com.mdframe.forge.plugin.ai.invocation.mapper.AiModelInvocationLogMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInvocationLogRetentionJobTest {

    @Mock
    private AiModelInvocationLogMapper mapper;

    @Test
    void defaultRetentionShouldDeleteRecordsOlderThanNinetyDays() {
        AiInvocationLogRetentionJob job = new AiInvocationLogRetentionJob(mapper);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        when(mapper.deleteBefore(org.mockito.ArgumentMatchers.any())).thenReturn(7);

        String result = job.cleanExpiredInvocationLogs(null);

        assertEquals("deleted=7", result);
        verify(mapper).deleteBefore(cutoff.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(90);
        assertTrue(Math.abs(Duration.between(expected, cutoff.getValue()).toSeconds()) <= 2);
    }

    @Test
    void explicitRetentionShouldPassExactCutoffWindowToMapper() {
        AiInvocationLogRetentionJob job = new AiInvocationLogRetentionJob(mapper);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        job.cleanExpiredInvocationLogs("30");

        verify(mapper).deleteBefore(cutoff.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(30);
        assertTrue(Math.abs(Duration.between(expected, cutoff.getValue()).toSeconds()) <= 2);
    }

    @Test
    void invalidRetentionShouldFailWithoutDeleting() {
        AiInvocationLogRetentionJob job = new AiInvocationLogRetentionJob(mapper);

        assertThrows(BusinessException.class, () -> job.cleanExpiredInvocationLogs("0"));
        assertThrows(BusinessException.class, () -> job.cleanExpiredInvocationLogs("-1"));
        assertThrows(BusinessException.class, () -> job.cleanExpiredInvocationLogs("abc"));
        verify(mapper, never()).deleteBefore(org.mockito.ArgumentMatchers.any());
    }
}
