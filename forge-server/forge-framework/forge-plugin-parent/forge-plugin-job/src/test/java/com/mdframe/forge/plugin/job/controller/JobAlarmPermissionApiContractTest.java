package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobAlarmPermissionApiContractTest {

    @Test
    void shouldReplaceAdminIgnoreWithMethodPermissions() throws NoSuchMethodException {
        assertFalse(JobConfigController.class.isAnnotationPresent(ApiPermissionIgnore.class));
        assertFalse(JobLogController.class.isAnnotationPresent(ApiPermissionIgnore.class));
        assertFalse(JobMonitorController.class.isAnnotationPresent(ApiPermissionIgnore.class));

        assertPermission(JobConfigController.class.getDeclaredMethod("page",
                com.mdframe.forge.starter.core.domain.PageQuery.class,
                com.mdframe.forge.plugin.job.dto.JobConfigQuery.class), "system:jobConfig:list");
        assertPermission(JobConfigController.class.getDeclaredMethod("add", JobConfigSaveRequest.class),
                "system:jobConfig:add");
        assertPermission(JobConfigController.class.getDeclaredMethod("trigger", Long.class),
                "system:jobConfig:trigger");
        assertPermission(JobLogController.class.getDeclaredMethod("detail", Long.class),
                "system:jobLog:detail");
        assertPermission(JobLogController.class.getDeclaredMethod("clean", int.class),
                "system:jobLog:clean");
    }

    @Test
    void shouldAuditWritesWithoutRawRequestOrResponse() throws NoSuchMethodException {
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("add", JobConfigSaveRequest.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("update", JobConfigSaveRequest.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("delete", Long.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("start", Long.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("stop", Long.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("trigger", Long.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod("sync", Long.class));
        assertSafeAudit(JobConfigController.class.getDeclaredMethod(
                "updateCron", Long.class, String.class));
        assertSafeAudit(JobLogController.class.getDeclaredMethod("clean", int.class));
        assertSafeAudit(JobLogController.class.getDeclaredMethod(
                "export", Map.class, HttpServletResponse.class));
    }

    private void assertPermission(Method method, String expected) {
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(permission);
        assertTrue(java.util.Arrays.asList(permission.value()).contains(expected));
    }

    private void assertSafeAudit(Method method) {
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        assertNotNull(operationLog);
        assertFalse(operationLog.saveRequestParams());
        assertFalse(operationLog.saveResponseResult());
    }
}
