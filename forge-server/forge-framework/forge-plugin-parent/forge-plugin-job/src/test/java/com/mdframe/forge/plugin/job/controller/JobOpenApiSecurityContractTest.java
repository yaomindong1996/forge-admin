package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.job.controller.openapi.JobExecutionOpenApiController;
import com.mdframe.forge.plugin.job.controller.openapi.JobOpenApiController;
import com.mdframe.forge.plugin.job.dto.JobApiTokenCreateRequest;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import com.mdframe.forge.plugin.job.entity.SysJobApiToken;
import com.mdframe.forge.plugin.job.vo.JobOpenApiExecutionVO;
import com.mdframe.forge.plugin.job.vo.JobOpenApiSummaryVO;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobOpenApiSecurityContractTest {

    @Test
    void shouldKeepOpenApiSeparateFromManagementPaths() {
        assertEquals("/openapi/v1/jobs",
                JobOpenApiController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals("/openapi/v1/executions",
                JobExecutionOpenApiController.class.getAnnotation(RequestMapping.class).value()[0]);
        assertEquals("/job/api-token",
                JobApiTokenController.class.getAnnotation(RequestMapping.class).value()[0]);
    }

    @Test
    void shouldNotPersistPlainCredentialsOrExposeInternalExecutionFields() {
        Set<String> tokenFields = fields(SysJobApiToken.class);
        Set<String> idempotencyFields = fields(SysJobApiIdempotency.class);
        Set<String> prohibitedResponses = Set.of(
                "executorBean", "executorHandler", "executorService", "jobParam",
                "result", "exceptionMsg", "fireInstanceId");

        assertFalse(tokenFields.contains("token"));
        assertFalse(tokenFields.contains("rawToken"));
        assertTrue(tokenFields.containsAll(Set.of("tokenKeyId", "tokenPrefix", "tokenHash")));
        assertFalse(idempotencyFields.contains("idempotencyKey"));
        assertTrue(idempotencyFields.contains("idempotencyKeyHash"));
        assertFalse(fields(JobOpenApiSummaryVO.class).stream().anyMatch(prohibitedResponses::contains));
        assertFalse(fields(JobOpenApiExecutionVO.class).stream().anyMatch(prohibitedResponses::contains));
    }

    @Test
    void shouldProtectAndSafelyAuditTokenManagement() throws NoSuchMethodException {
        assertPermission(JobApiTokenController.class.getDeclaredMethod(
                "create", JobApiTokenCreateRequest.class), "system:jobApiToken:add");
        assertPermission(JobApiTokenController.class.getDeclaredMethod(
                "revoke", Long.class), "system:jobApiToken:revoke");
        assertPermission(JobApiTokenController.class.getDeclaredMethod(
                "rotate", Long.class), "system:jobApiToken:rotate");
        assertSafeAudit(JobApiTokenController.class.getDeclaredMethod(
                "create", JobApiTokenCreateRequest.class));
        assertSafeAudit(JobApiTokenController.class.getDeclaredMethod(
                "rotate", Long.class));
    }

    private Set<String> fields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private void assertPermission(Method method, String expected) {
        SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
        assertNotNull(permission);
        assertTrue(Arrays.asList(permission.value()).contains(expected));
    }

    private void assertSafeAudit(Method method) {
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        assertNotNull(operationLog);
        assertFalse(operationLog.saveRequestParams());
        assertFalse(operationLog.saveResponseResult());
    }
}
