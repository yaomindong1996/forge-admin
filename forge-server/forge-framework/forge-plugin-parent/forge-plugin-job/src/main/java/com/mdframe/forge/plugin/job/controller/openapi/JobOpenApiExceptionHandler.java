package com.mdframe.forge.plugin.job.controller.openapi;

import com.mdframe.forge.plugin.job.support.JobOpenApiException;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = JobOpenApiController.class)
public class JobOpenApiExceptionHandler {

    @ExceptionHandler(JobOpenApiException.class)
    public ResponseEntity<RespInfo<Void>> handleOpenApiException(
            JobOpenApiException exception,
            HttpServletRequest request) {
        log.warn("定时任务开放API调用拒绝: path={}, resultCode={}, errorCode={}",
                request.getRequestURI(), exception.getStatus(), exception.getErrorCode());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getStatus() == 401) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        }
        if (exception.getStatus() == 429) {
            response.header(HttpHeaders.RETRY_AFTER, "60");
        }
        return response.body(RespInfo.error(exception.getStatus(), exception.getErrorCode()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<RespInfo<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        log.warn("定时任务开放API参数无效: path={}, exceptionType={}",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return ResponseEntity.badRequest().body(RespInfo.error(400, "invalid_request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespInfo<Void>> handleUnknown(Exception exception, HttpServletRequest request) {
        log.error("定时任务开放API服务异常: path={}, exceptionType={}",
                request.getRequestURI(), exception.getClass().getSimpleName());
        return ResponseEntity.status(503).body(RespInfo.error(503, "service_unavailable"));
    }
}
