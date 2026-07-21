package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JobExecutorEndpointTest {

    private static final String TOKEN = "job-executor-token-32-characters-minimum";

    @Test
    void shouldBeDisabledByDefaultAndUseDedicatedAuthentication() throws NoSuchMethodException {
        ConditionalOnProperty condition = JobExecutorEndpoint.class.getAnnotation(ConditionalOnProperty.class);
        assertNotNull(condition);
        assertFalse(condition.matchIfMissing());

        Method method = JobExecutorEndpoint.class.getDeclaredMethod(
                "execute", String.class, JobExecutorEndpoint.ExecuteRequest.class);
        assertNotNull(method.getAnnotation(SaIgnore.class));
        assertNotNull(method.getAnnotation(ApiPermissionIgnore.class));
    }

    @Test
    void shouldRejectInvalidBearerBeforeResolvingHandler() {
        ApplicationContext context = mock(ApplicationContext.class);
        JobExecutorEndpoint endpoint = endpoint(context, TOKEN);

        ResponseEntity<RespInfo<String>> response = endpoint.execute(
                "Bearer invalid-token", request("sampleHandler", "{\"secret\":\"value\"}"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().getCode());
        verifyNoInteractions(context);
    }

    @Test
    void shouldReturnOnlyHandlerResultAfterAuthentication() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        IJobExecutor executor = mock(IJobExecutor.class);
        when(context.getBean("sampleHandler", IJobExecutor.class)).thenReturn(executor);
        when(executor.execute("{\"value\":1}")).thenReturn("completed");
        JobExecutorEndpoint endpoint = endpoint(context, TOKEN);

        ResponseEntity<RespInfo<String>> response = endpoint.execute(
                "Bearer " + TOKEN, request("sampleHandler", "{\"value\":1}"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("completed", response.getBody().getData());
    }

    @Test
    void shouldNotExposeHandlerException() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        IJobExecutor executor = mock(IJobExecutor.class);
        when(context.getBean("sampleHandler", IJobExecutor.class)).thenReturn(executor);
        when(executor.execute("payload")).thenThrow(new IllegalStateException("password=raw-secret"));
        JobExecutorEndpoint endpoint = endpoint(context, TOKEN);

        ResponseEntity<RespInfo<String>> response = endpoint.execute(
                "Bearer " + TOKEN, request("sampleHandler", "payload"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("任务执行失败", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("raw-secret"));
    }

    @Test
    void shouldFailClosedWhenExecutorTokenIsNotConfigured() {
        ApplicationContext context = mock(ApplicationContext.class);
        JobExecutorEndpoint endpoint = endpoint(context, "");

        ResponseEntity<RespInfo<String>> response = endpoint.execute(
                "Bearer any-token", request("sampleHandler", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        verifyNoInteractions(context);
    }

    private JobExecutorEndpoint endpoint(ApplicationContext context, String token) {
        JobProperties properties = new JobProperties();
        properties.setExecutorToken(token);
        return new JobExecutorEndpoint(context, properties);
    }

    private JobExecutorEndpoint.ExecuteRequest request(String handlerName, String param) {
        JobExecutorEndpoint.ExecuteRequest request = new JobExecutorEndpoint.ExecuteRequest();
        request.setHandlerName(handlerName);
        request.setParam(param);
        return request;
    }
}

