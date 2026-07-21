package com.mdframe.forge.plugin.job.manager;

import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.model.JobApiTriggerTarget;
import com.mdframe.forge.plugin.job.service.JobApiReservationService;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobApiIdempotencyManagerTest {

    @Test
    void shouldFailBeforeReservationWhenRedisIsMissing() {
        JobApiReservationService reservationService = mock(JobApiReservationService.class);
        JobApiIdempotencyManager manager = manager(provider(null), reservationService);

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> manager.reserve(principal(), target(), "request-key-001"));

        assertEquals(503, exception.getStatus());
        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldSerializeSameKeyAndReturnReservation() throws Exception {
        RLock lock = mock(RLock.class);
        when(lock.tryLock(2000L, 30000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getLock(anyString())).thenReturn(lock);
        JobApiReservationService reservationService = mock(JobApiReservationService.class);
        when(reservationService.reserve(eq(principal()), eq(target()), any(), any()))
                .thenReturn(new JobApiReservationService.Reservation(88L, false));
        JobApiIdempotencyManager manager = manager(provider(client), reservationService);

        JobApiReservationService.Reservation result = manager.reserve(
                principal(), target(), "request-key-001");

        assertEquals(88L, result.executionId());
        assertFalse(result.reused());
    }

    @Test
    void shouldRejectUnsafeKeyWithoutCallingRedis() {
        ObjectProvider<RedissonClient> provider = provider(mock(RedissonClient.class));
        JobApiIdempotencyManager manager = manager(provider, mock(JobApiReservationService.class));

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> manager.reserve(principal(), target(), "bad key"));

        assertEquals(400, exception.getStatus());
        verifyNoInteractions(provider);
    }

    @Test
    void shouldReturnConflictWhenSameKeyLockIsBusy() throws Exception {
        RLock lock = mock(RLock.class);
        when(lock.tryLock(2000L, 30000L, TimeUnit.MILLISECONDS)).thenReturn(false);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getLock(anyString())).thenReturn(lock);
        JobApiReservationService reservationService = mock(JobApiReservationService.class);

        JobOpenApiException exception = assertThrows(JobOpenApiException.class,
                () -> manager(provider(client), reservationService)
                        .reserve(principal(), target(), "request-key-001"));

        assertEquals(409, exception.getStatus());
        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldReadWinningReservationAfterUniqueKeyRace() throws Exception {
        RLock lock = mock(RLock.class);
        when(lock.tryLock(2000L, 30000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        RedissonClient client = mock(RedissonClient.class);
        when(client.getLock(anyString())).thenReturn(lock);
        JobApiReservationService reservationService = mock(JobApiReservationService.class);
        when(reservationService.reserve(eq(principal()), eq(target()), any(), any()))
                .thenThrow(new DuplicateKeyException("concurrent reservation"));
        SysJobApiIdempotency winning = new SysJobApiIdempotency();
        winning.setExecutionId(99L);
        when(reservationService.findEffective(eq(principal()), eq(target()), any(), any()))
                .thenReturn(winning);

        JobApiReservationService.Reservation result = manager(provider(client), reservationService)
                .reserve(principal(), target(), "request-key-001");

        assertEquals(99L, result.executionId());
        assertTrue(result.reused());
        verify(reservationService).findEffective(eq(principal()), eq(target()), any(), any());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedissonClient> provider(RedissonClient client) {
        ObjectProvider<RedissonClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    private JobApiIdempotencyManager manager(
            ObjectProvider<RedissonClient> provider,
            JobApiReservationService reservationService) {
        return new JobApiIdempotencyManager(provider, reservationService, new JobProperties());
    }

    private JobApiPrincipal principal() {
        return new JobApiPrincipal(1L, 1L, "trusted-key", "caller",
                Set.of("jobs:trigger"), Set.of(7L), Set.of());
    }

    private JobApiTriggerTarget target() {
        JobApiTriggerTarget target = new JobApiTriggerTarget();
        target.setId(7L);
        target.setJobName("sampleJob");
        target.setJobGroup("DEFAULT");
        target.setStatus(1);
        target.setSyncStatus(JobScheduleCoordinator.SYNCED);
        return target;
    }
}
