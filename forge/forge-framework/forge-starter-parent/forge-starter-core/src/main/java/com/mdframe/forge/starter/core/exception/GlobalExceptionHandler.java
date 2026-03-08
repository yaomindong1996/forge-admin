package com.mdframe.forge.starter.core.exception;

import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，并返回规范的响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public RespInfo<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: URI={}, Code={}, Message={}", request.getRequestURI(), e.getCode(), e.getMessage());
        if (e.getData() != null) {
            return RespInfo.build(e.getCode(), e.getMessage(), e.getData());
        }
        return RespInfo.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常 (@Validated @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RespInfo<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public RespInfo<?> handleBindException(BindException e, HttpServletRequest request) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public RespInfo<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public RespInfo<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String errorMsg = String.format("缺少必需参数: %s", e.getParameterName());
        log.warn("缺少请求参数异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RespInfo<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String errorMsg = String.format("参数类型不匹配: %s", e.getName());
        log.warn("参数类型不匹配异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RespInfo<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String errorMsg = String.format("不支持的请求方法: %s", e.getMethod());
        log.warn("请求方法不支持异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        return RespInfo.error(405, errorMsg);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public RespInfo<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("404异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return RespInfo.error(404, "请求的资源不存在");
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public RespInfo<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问拒绝异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return RespInfo.error(403, "没有权限访问该资源");
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public RespInfo<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件上传大小超限: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return RespInfo.error(400, "上传文件大小超出限制");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public RespInfo<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: URI={}", request.getRequestURI(), e);
        return RespInfo.error(500, "系统内部错误");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RespInfo<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return RespInfo.error(400, e.getMessage() != null ? e.getMessage() : "参数错误");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public RespInfo<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("系统运行时错误: URI={}", request.getRequestURI(), e);
        return RespInfo.error(500, e.getMessage() != null ? e.getMessage().replace("java.lang.RuntimeException: ","") : e.getMessage());
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public RespInfo<?> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常: URI={}", request.getRequestURI(), e);
        return RespInfo.error(500, "系统异常，请联系管理员");
    }
}
