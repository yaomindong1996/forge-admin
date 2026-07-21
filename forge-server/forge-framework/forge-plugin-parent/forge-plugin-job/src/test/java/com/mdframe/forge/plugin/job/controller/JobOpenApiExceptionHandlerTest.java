package com.mdframe.forge.plugin.job.controller;

import com.mdframe.forge.plugin.job.controller.openapi.JobOpenApiExceptionHandler;
import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobOpenApiExceptionHandlerTest {

    private final JobOpenApiExceptionHandler handler = new JobOpenApiExceptionHandler();

    @Test
    void shouldReturnRealHttpStatusAndForgeErrorBody() {
        HttpServletRequest request = request();

        for (int status : List.of(400, 401, 403, 404, 409, 429, 503)) {
            ResponseEntity<RespInfo<Void>> response = handler.handleOpenApiException(
                    new JobOpenApiException(status, "safe_error_code"), request);

            assertEquals(status, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(status, response.getBody().getCode());
            assertEquals("safe_error_code", response.getBody().getMessage());
        }
    }

    @Test
    void shouldAddAuthenticationAndRateLimitHeaders() {
        HttpServletRequest request = request();

        ResponseEntity<RespInfo<Void>> unauthorized = handler.handleOpenApiException(
                JobOpenApiException.unauthorized(), request);
        ResponseEntity<RespInfo<Void>> limited = handler.handleOpenApiException(
                JobOpenApiException.tooManyRequests(), request);

        assertEquals("Bearer error=\"invalid_token\"",
                unauthorized.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE));
        assertEquals("60", limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void shouldHideUnknownExceptionDetails() {
        ResponseEntity<RespInfo<Void>> response = handler.handleUnknown(
                new IllegalStateException("database password must stay hidden"), request());

        assertEquals(503, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(503, response.getBody().getCode());
        assertEquals("service_unavailable", response.getBody().getMessage());
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/openapi/v1/jobs/7");
        return request;
    }
}
