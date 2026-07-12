package com.mdframe.forge.plugin.capability.controlplane.service;

import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityInvocationAuditEvent;
import com.mdframe.forge.plugin.capability.controlplane.audit.CapabilityActorType;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityInvocationLog;
import com.mdframe.forge.plugin.capability.controlplane.mapper.AiCapabilityInvocationLogMapper;
import com.mdframe.forge.plugin.capability.model.CapabilityResultStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class CapabilityInvocationAuditServiceTest {

    @Test
    void shouldPersistOnlySafeMetadataThroughIdempotentInsert() {
        AiCapabilityInvocationLogMapper mapper = mock(AiCapabilityInvocationLogMapper.class);
        CapabilityInvocationAuditService service = new CapabilityInvocationAuditService(mapper);
        CapabilityInvocationAuditEvent event = new CapabilityInvocationAuditEvent(
                "request-1", 1L, "client_a", 2L, "system.user.search", "1.0.0",
                CapabilityActorType.SERVICE, 3L, 3L, 4L,
                CapabilityResultStatus.SUCCESS, "OK", null, null, "trace-1", -10L);

        service.record(1L, event);

        ArgumentCaptor<AiCapabilityInvocationLog> captor =
                ArgumentCaptor.forClass(AiCapabilityInvocationLog.class);
        verify(mapper).insertIdempotent(captor.capture());
        AiCapabilityInvocationLog log = captor.getValue();
        assertThat(log.getId()).isPositive();
        assertThat(log.getRequestId()).isEqualTo("request-1");
        assertThat(log.getActorType()).isEqualTo("SERVICE");
        assertThat(log.getActorUserId()).isEqualTo(3L);
        assertThat(log.getServiceUserId()).isEqualTo(3L);
        assertThat(log.getDurationMs()).isZero();
        Set<String> eventFields = Arrays.stream(CapabilityInvocationAuditEvent.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase())
                .collect(Collectors.toSet());
        assertThat(eventFields).doesNotContain(
                "arguments", "data", "header", "headers", "token", "secret", "throwable");
    }

    @Test
    void shouldRejectRawExceptionOrSecretInStableErrorCode() {
        AiCapabilityInvocationLogMapper mapper = mock(AiCapabilityInvocationLogMapper.class);
        CapabilityInvocationAuditService service = new CapabilityInvocationAuditService(mapper);
        CapabilityInvocationAuditEvent event = new CapabilityInvocationAuditEvent(
                "request-1", 1L, "client_a", 2L, "system.user.search", "1.0.0",
                CapabilityActorType.USER, 8L, 3L, 4L,
                CapabilityResultStatus.ERROR, "EXECUTION_FAILED",
                "secret=fcp_leaked_value", null, "trace-1", 10L);

        assertThatThrownBy(() -> service.record(1L, event))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class)
                .hasMessageContaining("错误码");
        verify(mapper, never()).insertIdempotent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldInsertReservationThenUpdateFinalResultByTrustedIdentity() {
        AiCapabilityInvocationLogMapper mapper = mock(AiCapabilityInvocationLogMapper.class);
        CapabilityInvocationAuditService service = new CapabilityInvocationAuditService(mapper);
        CapabilityInvocationAuditEvent event = new CapabilityInvocationAuditEvent(
                "request-1", 1L, "client_a", 2L, "system.user.search", "1.0.0",
                CapabilityActorType.USER, 8L, 3L, 4L,
                CapabilityResultStatus.ERROR, "EXECUTION_PENDING", null, null, null, 0L);
        when(mapper.updateResultByRequestIdentity(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(mapper.insertIdempotent(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.recordOrUpdate(1L, event);

        verify(mapper).updateResultByRequestIdentity(org.mockito.ArgumentMatchers.any());
        verify(mapper).insertIdempotent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectDuplicateRequestWithDifferentAuditIdentity() {
        AiCapabilityInvocationLogMapper mapper = mock(AiCapabilityInvocationLogMapper.class);
        CapabilityInvocationAuditService service = new CapabilityInvocationAuditService(mapper);
        CapabilityInvocationAuditEvent event = new CapabilityInvocationAuditEvent(
                "request-1", 1L, "client_a", 2L, "system.user.search", "1.0.0",
                CapabilityActorType.USER, 8L, 3L, 4L,
                CapabilityResultStatus.SUCCESS, "SUCCESS", null, null, null, 10L);
        when(mapper.updateResultByRequestIdentity(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(mapper.insertIdempotent(org.mockito.ArgumentMatchers.any())).thenReturn(0);

        assertThatThrownBy(() -> service.recordOrUpdate(1L, event))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class)
                .hasMessageContaining("身份冲突");
    }

    @Test
    void shouldAcceptIdempotentUpdateWhenDriverReportsNoChangedRows() {
        AiCapabilityInvocationLogMapper mapper = mock(AiCapabilityInvocationLogMapper.class);
        CapabilityInvocationAuditService service = new CapabilityInvocationAuditService(mapper);
        CapabilityInvocationAuditEvent event = new CapabilityInvocationAuditEvent(
                "request-1", 1L, "client_a", 2L, "system.user.search", "1.0.0",
                CapabilityActorType.USER, 8L, 3L, 4L,
                CapabilityResultStatus.SUCCESS, "SUCCESS", null, null, null, 10L);
        AiCapabilityInvocationLog existing = new AiCapabilityInvocationLog();
        existing.setTenantId(1L);
        existing.setClientId(1L);
        existing.setClientCode("client_a");
        existing.setCapabilityId(2L);
        existing.setCapabilityCode("system.user.search");
        existing.setActorType("USER");
        existing.setActorUserId(8L);
        existing.setServiceUserId(3L);
        existing.setActiveOrgId(4L);
        when(mapper.updateResultByRequestIdentity(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(mapper.insertIdempotent(org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(mapper.selectByRequestId(1L, "request-1")).thenReturn(existing);

        assertThatCode(() -> service.recordOrUpdate(1L, event)).doesNotThrowAnyException();
        verify(mapper, times(2)).updateResultByRequestIdentity(org.mockito.ArgumentMatchers.any());
    }
}
