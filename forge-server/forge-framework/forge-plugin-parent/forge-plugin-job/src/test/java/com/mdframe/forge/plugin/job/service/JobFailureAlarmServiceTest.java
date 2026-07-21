package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import com.mdframe.forge.plugin.job.vo.JobFailureAlarmContextVO;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.message.channel.MessageChannel;
import com.mdframe.forge.starter.message.sdk.MessageClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobFailureAlarmServiceTest {

    @Test
    void shouldSendWebAndEmailWithoutSensitiveExecutionData() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.selectFailureAlarmContext(88L)).thenReturn(context("WEB,EMAIL"));
        MessageService messageService = mock(MessageService.class);
        MessageClient messageClient = mock(MessageClient.class);
        when(messageClient.send(any())).thenReturn(MessageChannel.SendResult.ok("mail-1"));

        JobFailureAlarmService service = service(mapper,
                provider(messageService), provider(messageClient), provider(null));

        service.notifyFinalFailure(88L);

        ArgumentCaptor<MessageSendRequestDTO> webRequest =
                ArgumentCaptor.forClass(MessageSendRequestDTO.class);
        verify(messageService).sendIfAbsent(webRequest.capture(), eq("JOB_FAILURE"), eq("88:WEB"));
        assertEquals(2, webRequest.getValue().getUserIds().size());
        assertTrue(webRequest.getValue().getContent().contains("执行ID：88"));
        assertFalse(webRequest.getValue().getContent().contains("raw-password"));
        assertFalse(webRequest.getValue().getContent().contains("ops@example.com"));

        ArgumentCaptor<MessageChannel.SendRequest> emailRequest =
                ArgumentCaptor.forClass(MessageChannel.SendRequest.class);
        verify(messageClient).send(emailRequest.capture());
        assertEquals(2, emailRequest.getValue().getEmailList().size());
        assertFalse(emailRequest.getValue().getContent().contains("raw-password"));
    }

    @Test
    void shouldSwallowUnavailableChannelAndIncrementFailureMetric() {
        SysJobLogMapper mapper = mock(SysJobLogMapper.class);
        when(mapper.selectFailureAlarmContext(88L)).thenReturn(context("WEB"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JobFailureAlarmService service = service(mapper,
                provider(null), provider(null), provider(registry));

        service.notifyFinalFailure(88L);

        assertEquals(1.0, registry.counter(
                "forge.job.alarm.send.failures", "channel", "WEB").count());
    }

    private JobFailureAlarmService service(SysJobLogMapper mapper,
                                           ObjectProvider<MessageService> messageServiceProvider,
                                           ObjectProvider<MessageClient> messageClientProvider,
                                           ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterProvider) {
        return new JobFailureAlarmService(mapper, new JobLogSanitizer(new ObjectMapper()),
                messageServiceProvider, messageClientProvider, meterProvider);
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private JobFailureAlarmContextVO context(String channels) {
        JobFailureAlarmContextVO context = new JobFailureAlarmContextVO();
        context.setExecutionId(88L);
        context.setJobConfigId(7L);
        context.setJobName("inventoryClose");
        context.setJobGroup("BUSINESS");
        context.setFailureTime(LocalDateTime.of(2026, 7, 20, 10, 30));
        context.setExceptionSummary("java.lang.IllegalStateException: password=raw-password\n\tat internal.Stack");
        context.setAlarmEnabled(1);
        context.setAlarmChannels(channels);
        context.setAlarmRecipientUserIds("12,13");
        context.setAlarmEmail("ops@example.com,owner@example.com");
        return context;
    }
}
