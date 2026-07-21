package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.constant.JobApiScopes;
import com.mdframe.forge.plugin.job.constant.JobApiTokenStatus;
import com.mdframe.forge.plugin.job.dto.JobApiTokenCreateRequest;
import com.mdframe.forge.plugin.job.entity.SysJobApiToken;
import com.mdframe.forge.plugin.job.mapper.SysJobApiTokenMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.support.JobApiTokenCodec;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobApiTokenCreatedVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobApiTokenServiceTest {

    @Test
    void shouldStoreOnlyHashAndAuthenticateIssuedToken() {
        SysJobApiTokenMapper tokenMapper = mock(SysJobApiTokenMapper.class);
        AtomicReference<SysJobApiToken> stored = new AtomicReference<>();
        AtomicLong ids = new AtomicLong(10L);
        when(tokenMapper.insert(any(SysJobApiToken.class))).thenAnswer(invocation -> {
            SysJobApiToken token = invocation.getArgument(0);
            token.setId(ids.incrementAndGet());
            stored.set(token);
            return 1;
        });
        when(tokenMapper.selectActiveByTokenKeyId(any())).thenAnswer(invocation -> {
            SysJobApiToken token = stored.get();
            return token != null && token.getTokenKeyId().equals(invocation.getArgument(0)) ? token : null;
        });
        when(tokenMapper.touchLastUsed(eq(1L), eq(11L), any())).thenReturn(1);
        JobApiTokenService service = service(tokenMapper);

        JobApiTokenCreatedVO issued = service.create(1L, request());
        SysJobApiToken entity = stored.get();

        assertTrue(issued.token().matches("^fja_[A-Za-z0-9_-]{22}_[A-Za-z0-9_-]{43}$"));
        assertEquals(64, entity.getTokenHash().length());
        assertNotEquals(issued.token(), entity.getTokenHash());
        assertFalse(entity.getResourceJobIds().contains(issued.token()));
        assertFalse(Arrays.stream(SysJobApiToken.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> Set.of("token", "rawToken", "plainToken").contains(name)));

        JobApiPrincipal principal = service.authenticate("Bearer " + issued.token());

        assertEquals(11L, principal.tokenId());
        assertEquals(Set.of(7L), principal.jobIds());
        assertEquals(Set.of("OPS"), principal.jobGroups());
        assertTrue(principal.scopes().contains(JobApiScopes.JOBS_TRIGGER));
        verify(tokenMapper).touchLastUsed(eq(1L), eq(11L), any());
    }

    @Test
    void shouldRejectExpiredOrWrongToken() {
        SysJobApiTokenMapper tokenMapper = mock(SysJobApiTokenMapper.class);
        AtomicReference<SysJobApiToken> stored = new AtomicReference<>();
        when(tokenMapper.insert(any(SysJobApiToken.class))).thenAnswer(invocation -> {
            SysJobApiToken token = invocation.getArgument(0);
            token.setId(12L);
            stored.set(token);
            return 1;
        });
        when(tokenMapper.selectActiveByTokenKeyId(any())).thenReturn(stored.get());
        JobApiTokenService service = service(tokenMapper);
        JobApiTokenCreatedVO issued = service.create(1L, request());
        when(tokenMapper.selectActiveByTokenKeyId(any())).thenReturn(stored.get());

        stored.get().setExpiresAt(LocalDateTime.now().minusMinutes(1));
        JobOpenApiException expired = assertThrows(JobOpenApiException.class,
                () -> service.authenticate("Bearer " + issued.token()));
        assertEquals(401, expired.getStatus());

        stored.get().setExpiresAt(LocalDateTime.now().plusDays(1));
        char replacement = issued.token().endsWith("A") ? 'B' : 'A';
        String changed = issued.token().substring(0, issued.token().length() - 1) + replacement;
        JobOpenApiException wrong = assertThrows(JobOpenApiException.class,
                () -> service.authenticate("Bearer " + changed));
        assertEquals(401, wrong.getStatus());

        stored.get().setStatus(JobApiTokenStatus.REVOKED);
        JobOpenApiException revoked = assertThrows(JobOpenApiException.class,
                () -> service.authenticate("Bearer " + issued.token()));
        assertEquals(401, revoked.getStatus());
    }

    @Test
    void shouldRejectTokenHashedWithDifferentPepper() {
        SysJobApiTokenMapper tokenMapper = mock(SysJobApiTokenMapper.class);
        AtomicReference<SysJobApiToken> stored = new AtomicReference<>();
        when(tokenMapper.insert(any(SysJobApiToken.class))).thenAnswer(invocation -> {
            SysJobApiToken token = invocation.getArgument(0);
            token.setId(13L);
            stored.set(token);
            return 1;
        });
        JobApiTokenCreatedVO issued = service(tokenMapper).create(1L, request());
        when(tokenMapper.selectActiveByTokenKeyId(any())).thenAnswer(invocation -> stored.get());
        JobApiTokenService wrongPepperService = service(
                tokenMapper, "different-job-open-api-pepper-32-characters-minimum");

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> wrongPepperService.authenticate("Bearer " + issued.token()));

        assertEquals(401, exception.getStatus());
    }

    @Test
    void shouldAbortRotationWhenRevokeCasLosesRace() {
        SysJobApiTokenMapper tokenMapper = mock(SysJobApiTokenMapper.class);
        SysJobApiToken current = new SysJobApiToken();
        current.setId(12L);
        current.setTenantId(1L);
        current.setCallerName("integration-service");
        current.setStatus(JobApiTokenStatus.ACTIVE);
        current.setScopes(JobApiScopes.JOBS_READ);
        current.setResourceJobIds("[7]");
        current.setResourceJobGroups("[]");
        current.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(tokenMapper.selectTenantById(1L, 12L)).thenReturn(current);
        when(tokenMapper.insert(any(SysJobApiToken.class))).thenAnswer(invocation -> {
            SysJobApiToken replacement = invocation.getArgument(0);
            replacement.setId(14L);
            return 1;
        });
        when(tokenMapper.revoke(eq(1L), eq(12L), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service(tokenMapper).rotate(1L, 12L));

        assertEquals("Token状态已变化，轮换已取消", exception.getMessage());
        verify(tokenMapper).insert(any(SysJobApiToken.class));
        verify(tokenMapper).revoke(eq(1L), eq(12L), any());
    }

    @Test
    void shouldRequireConfiguredPepper() {
        JobProperties properties = new JobProperties();
        JobApiTokenService service = new JobApiTokenService(
                mock(SysJobApiTokenMapper.class), mock(SysJobConfigMapper.class),
                new JobApiTokenCodec(properties), properties, new ObjectMapper());

        assertThrows(com.mdframe.forge.starter.core.exception.BusinessException.class,
                () -> service.create(1L, request()));
        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> service.authenticate("Bearer invalid"));
        assertEquals(503, exception.getStatus());
    }

    private JobApiTokenService service(SysJobApiTokenMapper tokenMapper) {
        return service(tokenMapper, "job-open-api-test-pepper-32-characters-minimum");
    }

    private JobApiTokenService service(SysJobApiTokenMapper tokenMapper, String pepper) {
        JobProperties properties = new JobProperties();
        properties.getOpenApi().setTokenPepper(pepper);
        return new JobApiTokenService(
                tokenMapper, mock(SysJobConfigMapper.class),
                new JobApiTokenCodec(properties), properties, new ObjectMapper());
    }

    private JobApiTokenCreateRequest request() {
        return new JobApiTokenCreateRequest(
                "integration-service",
                "test caller",
                Set.of(JobApiScopes.JOBS_READ, JobApiScopes.JOBS_TRIGGER),
                Set.of(7L),
                Set.of("OPS"),
                LocalDateTime.now().plusDays(30));
    }
}
