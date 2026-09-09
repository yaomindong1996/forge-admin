package com.mdframe.forge.starter.core.exception;

import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，并返回规范的响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SYSTEM_ERROR_MESSAGE = "系统异常，请联系管理员";

    private static final String DATABASE_ERROR_MESSAGE = "数据访问异常，请联系管理员";

    private static final Pattern SQL_STATEMENT_PATTERN = Pattern.compile(
            "(?is)\\b(select|insert|update|delete|replace|merge|alter|drop|create|truncate)\\b[\\s\\S]{0,800}\\b(from|into|set|values|table|database)\\b"
    );

    private static final Pattern DATABASE_OBJECT_ERROR_PATTERN = Pattern.compile(
            "(?is)\\b(unknown\\s+(column|table|database)|table\\s+.+?\\s+doesn't\\s+exist|column\\s+.+?\\s+cannot\\s+be\\s+null|data\\s+too\\s+long\\s+for\\s+column)\\b"
    );

    private static final Pattern EXCEPTION_MESSAGE_PREFIX_PATTERN = Pattern.compile(
            "^(?:(?:[\\w$]+\\.)*[\\w$]*(?:Exception|Error):\\s*)+"
    );

    private static final String[] DATABASE_EXCEPTION_CLASS_PREFIXES = {
            "java.sql.",
            "com.mysql.",
            "org.postgresql.",
            "oracle.jdbc.",
            "dm.jdbc.",
            "com.microsoft.sqlserver.jdbc.",
            "org.springframework.dao.",
            "org.springframework.jdbc.",
            "org.mybatis.",
            "org.apache.ibatis.",
            "com.baomidou.mybatisplus.core.exceptions.",
            "net.sf.jsqlparser."
    };

    private static final String[] DATABASE_MESSAGE_MARKERS = {
            "### sql:",
            "### cause:",
            "bad sql grammar",
            "error querying database",
            "error updating database",
            "sqlexception",
            "sqlsyntaxerrorexception",
            "sqlintegrityconstraintviolationexception",
            "sql integrity constraint violation",
            "you have an error in your sql syntax",
            "unknown column",
            "unknown table",
            "unknown database",
            "table doesn't exist",
            "data truncation",
            "duplicate entry",
            "foreign key constraint fails",
            "communications link failure",
            "access denied for user",
            "public key retrieval is not allowed",
            "lock wait timeout",
            "deadlock found",
            "preparedstatementcallback",
            "statementcallback"
    };

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public RespInfo<?> handleBusinessException(BusinessException e, HttpServletRequest request,
                                               HttpServletResponse response) {
        setHttpStatus(response, e.getCode());
        if (containsSensitiveDatabaseDetail(e)) {
            return handleDatabaseError(e, request, response, "业务异常包装数据库异常");
        }
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
    public RespInfo<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request,
                                                             HttpServletResponse response) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 400);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public RespInfo<?> handleBindException(BindException e, HttpServletRequest request, HttpServletResponse response) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 400);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public RespInfo<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request,
                                                          HttpServletResponse response) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 400);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public RespInfo<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,
                                                                      HttpServletRequest request,
                                                                      HttpServletResponse response) {
        String errorMsg = String.format("缺少必需参数: %s", e.getParameterName());
        log.warn("缺少请求参数异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 400);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RespInfo<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e,
                                                                  HttpServletRequest request,
                                                                  HttpServletResponse response) {
        String errorMsg = String.format("参数类型不匹配: %s", e.getName());
        log.warn("参数类型不匹配异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 400);
        return RespInfo.error(400, errorMsg);
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RespInfo<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e,
                                                                     HttpServletRequest request,
                                                                     HttpServletResponse response) {
        String errorMsg = String.format("不支持的请求方法: %s", e.getMethod());
        log.warn("请求方法不支持异常: URI={}, Message={}", request.getRequestURI(), errorMsg);
        setHttpStatus(response, 405);
        return RespInfo.error(405, errorMsg);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public RespInfo<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request,
                                                     HttpServletResponse response) {
        log.warn("404异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        setHttpStatus(response, 404);
        return RespInfo.error(404, "请求的资源不存在");
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public RespInfo<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request,
                                                   HttpServletResponse response) {
        log.warn("访问拒绝异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        setHttpStatus(response, 403);
        return RespInfo.error(403, "没有权限访问该资源");
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public RespInfo<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request,
                                                            HttpServletResponse response) {
        log.warn("文件上传大小超限: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        setHttpStatus(response, 413);
        return RespInfo.error(413, "上传文件大小超出限制");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public RespInfo<?> handleNullPointerException(NullPointerException e, HttpServletRequest request,
                                                  HttpServletResponse response) {
        log.error("空指针异常: URI={}", request.getRequestURI(), e);
        setHttpStatus(response, 500);
        return RespInfo.error(500, SYSTEM_ERROR_MESSAGE);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RespInfo<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request,
                                                      HttpServletResponse response) {
        if (containsSensitiveDatabaseDetail(e)) {
            return handleDatabaseError(e, request, response, "非法参数包装数据库异常");
        }
        log.warn("非法参数异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        setHttpStatus(response, 400);
        return RespInfo.error(400, e.getMessage() != null ? e.getMessage() : "参数错误");
    }

    /**
     * 处理数据库异常
     */
    @ExceptionHandler(SQLException.class)
    public RespInfo<?> handleSQLException(SQLException e, HttpServletRequest request,
                                          HttpServletResponse response) {
        return handleDatabaseError(e, request, response, "数据库异常");
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public RespInfo<?> handleRuntimeException(RuntimeException e, HttpServletRequest request,
                                              HttpServletResponse response) {
        if (containsSensitiveDatabaseDetail(e)) {
            return handleDatabaseError(e, request, response, "系统运行时数据库异常");
        }
        BusinessException businessCause = findCause(e, BusinessException.class);
        if (businessCause != null) {
            log.warn("运行时异常包装业务异常: URI={}, Code={}, Message={}", request.getRequestURI(), businessCause.getCode(), businessCause.getMessage(), e);
            setHttpStatus(response, businessCause.getCode());
            if (businessCause.getData() != null) {
                return RespInfo.build(businessCause.getCode(), sanitizeClientMessage(businessCause.getMessage()), businessCause.getData());
            }
            return RespInfo.error(businessCause.getCode(), sanitizeClientMessage(businessCause.getMessage()));
        }
        log.error("系统运行时错误: URI={}", request.getRequestURI(), e);
        setHttpStatus(response, 500);
        return RespInfo.error(500, buildRuntimeClientMessage(e));
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public RespInfo<?> handleException(Exception e, HttpServletRequest request,
                                       HttpServletResponse response) {
        if (containsSensitiveDatabaseDetail(e)) {
            return handleDatabaseError(e, request, response, "未知数据库异常");
        }
        log.error("未知异常: URI={}", request.getRequestURI(), e);
        setHttpStatus(response, 500);
        return RespInfo.error(500, SYSTEM_ERROR_MESSAGE);
    }

    private RespInfo<?> handleDatabaseError(Throwable e, HttpServletRequest request,
                                            HttpServletResponse response, String logMessage) {
        log.error("{}: URI={}", logMessage, request.getRequestURI(), e);
        setHttpStatus(response, 500);
        return RespInfo.error(500, DATABASE_ERROR_MESSAGE);
    }

    private void setHttpStatus(HttpServletResponse response, Integer code) {
        if (response == null || code == null) {
            return;
        }
        int status = code >= 400 && code <= 599 ? code : 500;
        response.setStatus(status);
        if (status == 429 && !response.containsHeader("Retry-After")) {
            response.setHeader("Retry-After", "60");
        }
    }

    private boolean containsSensitiveDatabaseDetail(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (isDatabaseExceptionClass(current) || containsSqlDetail(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDatabaseExceptionClass(Throwable throwable) {
        String className = throwable.getClass().getName();
        for (String prefix : DATABASE_EXCEPTION_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSqlDetail(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        for (String marker : DATABASE_MESSAGE_MARKERS) {
            if (lowerMessage.contains(marker)) {
                return true;
            }
        }
        return SQL_STATEMENT_PATTERN.matcher(message).find()
                || DATABASE_OBJECT_ERROR_PATTERN.matcher(message).find();
    }

    private String buildRuntimeClientMessage(RuntimeException e) {
        Throwable current = e;
        while (current != null) {
            String message = sanitizeClientMessage(current.getMessage());
            if (message != null && !message.isBlank()) {
                return message;
            }
            current = current.getCause();
        }
        return SYSTEM_ERROR_MESSAGE;
    }

    private String sanitizeClientMessage(String message) {
        if (message == null) {
            return null;
        }
        String sanitized = EXCEPTION_MESSAGE_PREFIX_PATTERN.matcher(message.trim()).replaceFirst("").trim();
        return sanitized;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
