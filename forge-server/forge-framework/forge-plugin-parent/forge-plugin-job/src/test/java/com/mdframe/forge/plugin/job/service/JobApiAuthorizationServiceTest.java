package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.constant.JobApiScopes;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.model.JobApiPrincipal;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.plugin.job.vo.JobOpenApiSummaryVO;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobApiAuthorizationServiceTest {

    @Test
    void shouldRequireScopeAndPassTrustedResourceSetsToMapper() {
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        JobOpenApiSummaryVO expected = new JobOpenApiSummaryVO();
        expected.setId(7L);
        when(configMapper.selectOpenJobById(7L, Set.of(7L), Set.of("OPS"))).thenReturn(expected);
        JobApiAuthorizationService service = service(configMapper);
        JobApiPrincipal principal = principal();

        service.requireScope(principal, JobApiScopes.JOBS_READ);
        assertSame(expected, service.requireJob(principal, 7L));
        verify(configMapper).selectOpenJobById(7L, Set.of(7L), Set.of("OPS"));

        JobOpenApiException denied = assertThrows(JobOpenApiException.class,
                () -> service.requireScope(principal, JobApiScopes.JOBS_TRIGGER));
        assertEquals(403, denied.getStatus());
    }

    @Test
    void shouldDistinguishForbiddenFromMissingJob() {
        SysJobConfigMapper configMapper = mock(SysJobConfigMapper.class);
        JobApiAuthorizationService service = service(configMapper);
        when(configMapper.countOpenJobById(8L)).thenReturn(1);

        JobOpenApiException forbidden = assertThrows(JobOpenApiException.class,
                () -> service.requireJob(principal(), 8L));
        assertEquals(403, forbidden.getStatus());

        JobOpenApiException missing = assertThrows(JobOpenApiException.class,
                () -> service.requireJob(principal(), 9L));
        assertEquals(404, missing.getStatus());
    }

    private JobApiAuthorizationService service(SysJobConfigMapper configMapper) {
        return new JobApiAuthorizationService(configMapper, mock(SysJobLogMapper.class));
    }

    private JobApiPrincipal principal() {
        return new JobApiPrincipal(
                1L, 1L, "key-id", "caller",
                Set.of(JobApiScopes.JOBS_READ), Set.of(7L), Set.of("OPS"));
    }
}
