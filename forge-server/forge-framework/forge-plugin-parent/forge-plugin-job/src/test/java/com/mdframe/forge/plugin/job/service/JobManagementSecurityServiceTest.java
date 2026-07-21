package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.starter.core.context.ExecutionIdentity;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobManagementSecurityServiceTest {

    @AfterEach
    void tearDown() {
        ExecutionIdentityContextHolder.clear();
    }

    @Test
    void shouldRequireDangerousPermissionForBeanAndRpcTargets() {
        JobManagementSecurityService service = new JobManagementSecurityService(
                new JobExecutorCatalogService());
        JobConfigSaveRequest request = new JobConfigSaveRequest();
        request.setExecuteMode("BEAN");

        try (var ignored = ExecutionIdentityContextHolder.open(identity(2, Set.of()))) {
            assertThrows(BusinessException.class, () -> service.assertCanManageTarget(request));
        }
        try (var ignored = ExecutionIdentityContextHolder.open(
                identity(2, Set.of(JobPermissions.CONFIG_DANGEROUS)))) {
            assertDoesNotThrow(() -> service.assertCanManageTarget(request));
        }
    }

    @Test
    void shouldProtectCodeRegisteredScheduledTasks() {
        JobExecutorCatalogService catalog = new JobExecutorCatalogService();
        ScheduledJob annotation = mock(ScheduledJob.class);
        when(annotation.enabled()).thenReturn(true);
        when(annotation.name()).thenReturn("protectedJob");
        when(annotation.description()).thenReturn("受保护任务");
        when(annotation.group()).thenReturn("SYSTEM");
        catalog.registerScheduledJob("protectedBean", "run", annotation);
        JobManagementSecurityService service = new JobManagementSecurityService(catalog);
        SysJobConfig config = new SysJobConfig();
        config.setExecuteMode("BEAN");
        config.setExecutorBean("protectedBean");
        config.setExecutorMethod("run");

        try (var ignored = ExecutionIdentityContextHolder.open(identity(2, Set.of()))) {
            assertThrows(BusinessException.class,
                    () -> service.assertCanManageProtectedTask(config));
        }
        try (var ignored = ExecutionIdentityContextHolder.open(identity(0, Set.of()))) {
            assertDoesNotThrow(() -> service.assertCanManageProtectedTask(config));
        }
    }

    @Test
    void shouldRequireSensitiveDetailPermission() {
        JobManagementSecurityService service = new JobManagementSecurityService(
                new JobExecutorCatalogService());

        try (var ignored = ExecutionIdentityContextHolder.open(identity(2, Set.of()))) {
            assertThrows(BusinessException.class, service::assertSensitiveLogAccess);
        }
        try (var ignored = ExecutionIdentityContextHolder.open(
                identity(2, Set.of(JobPermissions.LOG_DETAIL)))) {
            assertDoesNotThrow(service::assertSensitiveLogAccess);
        }
    }

    private ExecutionIdentity identity(int userType, Set<String> permissions) {
        LoginUser user = new LoginUser();
        user.setUserId(10L);
        user.setTenantId(1L);
        user.setUsername("job-operator");
        user.setUserType(userType);
        user.setPermissions(permissions);
        return new ExecutionIdentity(user, "USER", 10L, 10L,
                1L, "pc", "test-token", Set.of());
    }
}
