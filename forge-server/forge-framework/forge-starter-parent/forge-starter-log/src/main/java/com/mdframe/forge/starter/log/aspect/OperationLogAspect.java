package com.mdframe.forge.starter.log.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.apiconfig.domain.dto.ApiConfigInfo;
import com.mdframe.forge.starter.apiconfig.service.IApiConfigManager;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.context.LogProperties;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.log.service.ILogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 操作日志切面
 * 拦截@OperationLog注解，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ILogService.class)
public class OperationLogAspect {

    private final ILogService logService;
    private final LogProperties logProperties;
    @Qualifier("logTaskExecutor")
    private final Executor logTaskExecutor;
    private final IApiConfigManager apiConfigManager;

    private static final DateTimeFormatter TRACE_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String TRACE_ID_KEY = "traceId";
    private static final String PAGE_PATH_HEADER = "X-Page-Path";
    private static final String PAGE_TITLE_HEADER = "X-Page-Title";
    private static final String DECRYPTED_REQUEST_BODY_OMITTED = "[DECRYPTED_REQUEST_BODY_OMITTED]";

    /**
     * 定义切点：拦截所有标注@OperationLog的方法
     */
    //@Pointcut("@annotation(com.mdframe.forge.starter.core.annotation.log.OperationLog)")
    @Pointcut("@within(org.springframework.stereotype.Controller) || @within(org.springframework.web.bind.annotation.RestController)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 检查是否启用操作日志
        if (logProperties.getEnableOperationLog() == null || !logProperties.getEnableOperationLog()) {
            return joinPoint.proceed();
        }

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String requestUrl = request.getRequestURI();

        // 检查是否在排除路径中
        if (isExcludePath(requestUrl)) {
            return joinPoint.proceed();
        }

        // 构建日志对象
        OperationLogInfo logInfo = new OperationLogInfo();
        logInfo.setOperationTime(LocalDateTime.now());
        logInfo.setRequestUrl(requestUrl);
        logInfo.setRequestMethod(request.getMethod());
        logInfo.setOperationIp(getClientIp(request));
        logInfo.setUserAgent(request.getHeader("User-Agent"));
        logInfo.setOperationPage(truncate(request.getHeader(PAGE_PATH_HEADER), 500));
        logInfo.setOperationPageTitle(truncate(decodeHeader(request.getHeader(PAGE_TITLE_HEADER)), 200));

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        boolean decryptEndpoint = isApiDecryptEndpoint(joinPoint, method);

        String requestParams = "";
        ApiConfigInfo apiConfig = null;
        if (annotation != null) {
            logInfo.setOperationModule(annotation.module());
            logInfo.setOperationType(annotation.type().name());
            logInfo.setOperationDesc(annotation.desc());
        } else {
            apiConfig = apiConfigManager.getApiConfig(request.getRequestURI(), request.getMethod());
            if (apiConfig != null) {
                logInfo.setOperationModule(apiConfig.getModuleCode());
                logInfo.setOperationType(resolveDefaultOperationType(apiConfig.getReqMethod(), requestUrl));
                logInfo.setOperationDesc(apiConfig.getApiName());
            } else {
                logInfo.setOperationType(resolveDefaultOperationType(request.getMethod(), requestUrl));
            }
        }
        normalizeAuditMetadata(logInfo, requestUrl);

        if (shouldSkipOperationLog(logInfo.getOperationType(), request.getMethod())) {
            return proceedWithoutOperationLog(joinPoint);
        }

        // 生成traceId并放入MDC
        String traceId = generateTraceId();
        MDC.put(TRACE_ID_KEY, traceId);

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 保存请求参数
        if (annotation != null) {
            if (annotation.saveRequestParams()) {
                requestParams = getRequestParams(joinPoint, request, decryptEndpoint);
                logInfo.setRequestParams(truncate(requestParams, logProperties.getRequestParamsMaxLength()));
            }
        } else {
            requestParams = getRequestParams(joinPoint, request, decryptEndpoint);
            logInfo.setRequestParams(truncate(requestParams, logProperties.getRequestParamsMaxLength()));
        }

        logInfo.setOperationContent(buildOperationContent(logInfo, requestUrl));
        // 获取当前登录用户信息
        try {
            Long userId = SessionHelper.getUserId();
            if (userId != null) {
                logInfo.setUserId(userId);
            } else if (StpUtil.isLogin()) {
                Object loginId = StpUtil.getLoginId();
                logInfo.setUserId(Long.valueOf(loginId.toString()));
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败", e);
        }

        // 打印入参日志
        if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
            printRequestLog(methodName, requestUrl,
                    annotation != null ? annotation.module() : "",
                    annotation != null ? annotation.desc() : "",
                    requestParams);
        }

        // 执行目标方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            logInfo.setOperationStatus(1);  // 成功

            // 保存响应结果
            String responseResult = "";
            if (annotation != null && annotation.saveResponseResult() && result != null) {
                responseResult = JSONObject.toJSONString(result);
                logInfo.setResponseResult(truncate(responseResult, logProperties.getResponseResultMaxLength()));
            }
            fillAuditSnapshot(logInfo, requestParams, responseResult);

            // 计算执行时长
            long executeTime = System.currentTimeMillis() - startTime;
            logInfo.setExecuteTime(executeTime);

            // 打印出参日志
            if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
                printResponseLog(methodName, requestUrl,
                        annotation != null ? annotation.module() : "",
                        annotation != null ? annotation.desc() : "",
                        responseResult, executeTime);
            }

        } catch (Throwable e) {
            logInfo.setOperationStatus(0);  // 失败
            logInfo.setErrorMsg(truncate(e.getMessage(), 500));
            fillAuditSnapshot(logInfo, requestParams, null);

            // 计算执行时长
            long executeTime = System.currentTimeMillis() - startTime;
            logInfo.setExecuteTime(executeTime);

            // 打印异常日志
            if (logProperties.getPrintOperationLog() != null && logProperties.getPrintOperationLog()) {
                printErrorLog(methodName, requestUrl,
                        annotation != null ? annotation.module() : "",
                        annotation != null ? annotation.desc() : "",
                        e, executeTime);
            }

            throw e;
        } finally {
            // 异步保存日志
            saveLogAsync(logInfo);
            OperationAuditContext.clear();
            // 清除MDC
            MDC.remove(TRACE_ID_KEY);
        }

        return result;
    }

    private Object proceedWithoutOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } finally {
            OperationAuditContext.clear();
        }
    }

    /**
     * 填充页面操作审计快照。
     */
    private void fillAuditSnapshot(OperationLogInfo logInfo, String requestParams, String responseResult) {
        OperationAuditContext.Snapshot snapshot = OperationAuditContext.snapshot();
        if (snapshot.getOperationContent() != null) {
            logInfo.setOperationContent(truncate(toJsonString(snapshot.getOperationContent()), 1000));
        }
        if (snapshot.getBeforeData() != null) {
            logInfo.setBeforeData(truncate(toJsonString(snapshot.getBeforeData()), logProperties.getResponseResultMaxLength()));
        }
        if (snapshot.getAfterData() != null) {
            logInfo.setAfterData(truncate(toJsonString(snapshot.getAfterData()), logProperties.getResponseResultMaxLength()));
        }
        if (snapshot.getDiffData() != null) {
            logInfo.setDiffData(truncate(toJsonString(snapshot.getDiffData()), logProperties.getResponseResultMaxLength()));
        }
        if (StrUtil.isBlank(logInfo.getAfterData()) && isMutatingOperation(logInfo.getOperationType())) {
            String fallbackAfterData = resolveFallbackAfterData(requestParams, responseResult);
            if (StrUtil.isNotBlank(fallbackAfterData)) {
                logInfo.setAfterData(truncate(fallbackAfterData, logProperties.getResponseResultMaxLength()));
            }
        }
    }

    private String resolveFallbackAfterData(String requestParams, String responseResult) {
        if (StrUtil.isNotBlank(requestParams) && !containsOmittedRequestBody(requestParams)) {
            return requestParams;
        }
        if (StrUtil.isNotBlank(responseResult) && !containsOmittedRequestBody(responseResult)) {
            return responseResult;
        }
        return "";
    }

    private boolean containsOmittedRequestBody(String value) {
        return StrUtil.isNotBlank(value) && value.contains(DECRYPTED_REQUEST_BODY_OMITTED);
    }

    private boolean isMutatingOperation(String operationType) {
        return "ADD".equals(operationType)
                || "UPDATE".equals(operationType)
                || "DELETE".equals(operationType)
                || "IMPORT".equals(operationType)
                || "EXPORT".equals(operationType)
                || "OTHER".equals(operationType);
    }

    private boolean shouldSkipOperationLog(String operationType, String requestMethod) {
        if ("QUERY".equals(operationType)) {
            return true;
        }
        if (isMutatingOperation(operationType)) {
            return false;
        }
        return isReadOnlyMethod(requestMethod);
    }

    private boolean isReadOnlyMethod(String requestMethod) {
        return "GET".equalsIgnoreCase(requestMethod)
                || "HEAD".equalsIgnoreCase(requestMethod)
                || "OPTIONS".equalsIgnoreCase(requestMethod);
    }

    private String resolveDefaultOperationType(String requestMethod, String requestUrl) {
        if (isReadOnlyMethod(requestMethod)) {
            return "QUERY";
        }
        if (isQueryLikeRequest(requestUrl)) {
            return "QUERY";
        }
        if ("DELETE".equalsIgnoreCase(requestMethod) || isDeleteLikeRequest(requestUrl)) {
            return "DELETE";
        }
        if (isAddLikeRequest(requestUrl)) {
            return "ADD";
        }
        return "UPDATE";
    }

    private boolean isQueryLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/page")
                || normalizedUrl.endsWith("/list")
                || normalizedUrl.endsWith("/tree")
                || normalizedUrl.endsWith("/detail")
                || normalizedUrl.endsWith("/getbyid")
                || normalizedUrl.endsWith("/options")
                || normalizedUrl.endsWith("/profile")
                || normalizedUrl.endsWith("/query");
    }

    private boolean isDeleteLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/remove")
                || normalizedUrl.endsWith("/removebatch")
                || normalizedUrl.endsWith("/delete")
                || normalizedUrl.endsWith("/deletebatch");
    }

    private boolean isAddLikeRequest(String requestUrl) {
        String normalizedUrl = normalizeRequestUrl(requestUrl);
        return normalizedUrl.endsWith("/add")
                || normalizedUrl.endsWith("/create")
                || normalizedUrl.endsWith("/import");
    }

    private String normalizeRequestUrl(String requestUrl) {
        if (StrUtil.isBlank(requestUrl)) {
            return "";
        }
        return requestUrl.trim().toLowerCase();
    }

    private void normalizeAuditMetadata(OperationLogInfo logInfo, String requestUrl) {
        if (StrUtil.isBlank(logInfo.getOperationModule())) {
            String pageTitle = StrUtil.blankToDefault(logInfo.getOperationPageTitle(), "");
            if (StrUtil.isNotBlank(pageTitle) && !isGenericPageTitle(pageTitle)) {
                logInfo.setOperationModule(pageTitle);
            }
        }
        if (StrUtil.isBlank(logInfo.getOperationDesc())) {
            logInfo.setOperationDesc(resolveDefaultOperationDesc(logInfo.getOperationType(), requestUrl));
        }
    }

    private boolean isGenericPageTitle(String pageTitle) {
        return "企业级中后台管理系统".equals(pageTitle)
                || "企业级中后台基础框架".equals(pageTitle);
    }

    private String resolveDefaultOperationDesc(String operationType, String requestUrl) {
        if ("ADD".equals(operationType)) {
            return "新增数据";
        }
        if ("UPDATE".equals(operationType)) {
            return "修改数据";
        }
        if ("DELETE".equals(operationType)) {
            return "删除数据";
        }
        if ("IMPORT".equals(operationType)) {
            return "导入数据";
        }
        if ("EXPORT".equals(operationType)) {
            return "导出数据";
        }
        return StrUtil.blankToDefault(requestUrl, "执行操作");
    }

    private String buildOperationContent(OperationLogInfo logInfo, String requestUrl) {
        String desc = StrUtil.blankToDefault(logInfo.getOperationDesc(), "");
        String module = StrUtil.blankToDefault(logInfo.getOperationModule(), "");
        String type = StrUtil.blankToDefault(logInfo.getOperationType(), "");
        String pageTitle = StrUtil.blankToDefault(logInfo.getOperationPageTitle(), "");
        StringBuilder content = new StringBuilder();
        if (StrUtil.isNotBlank(pageTitle)) {
            content.append("页面[").append(pageTitle).append("] ");
        }
        if (StrUtil.isNotBlank(module)) {
            content.append("模块[").append(module).append("] ");
        }
        if (StrUtil.isNotBlank(type)) {
            content.append("类型[").append(type).append("] ");
        }
        if (StrUtil.isNotBlank(desc)) {
            content.append(desc);
        } else {
            content.append(requestUrl);
        }
        return truncate(content.toString(), 1000);
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return JSONObject.toJSONString(value);
    }

    private String decodeHeader(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(ProceedingJoinPoint joinPoint, HttpServletRequest request, boolean redactRequestBody) {
        try {
            Map<String, Object> params = new HashMap<>();

            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();

            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    // 过滤掉Request、Response等对象
                    if (arg instanceof HttpServletRequest
                            || arg instanceof jakarta.servlet.http.HttpServletResponse
                            || arg instanceof org.springframework.web.multipart.MultipartFile) {
                        continue;
                    }
                    String paramName = parameterNames != null && i < parameterNames.length
                            ? parameterNames[i]
                            : "arg" + i;
                    if (redactRequestBody && hasParameterAnnotation(signature.getMethod(), i, RequestBody.class)) {
                        params.put(paramName, "[DECRYPTED_REQUEST_BODY_OMITTED]");
                    } else {
                        params.put(paramName, arg);
                    }
                }
            }

            // 获取URL参数
            request.getParameterMap().forEach((key, value) -> {
                if (value != null && value.length > 0) {
                    params.put(key, value.length == 1 ? value[0] : value);
                }
            });

            return JSONObject.toJSONString(params);
        } catch (Exception e) {
            log.error("获取请求参数失败", e);
            return "";
        }
    }

    /**
     * 判断当前接口是否启用了请求解密。
     */
    private boolean isApiDecryptEndpoint(ProceedingJoinPoint joinPoint, Method method) {
        if (method.isAnnotationPresent(ApiDecrypt.class)
                || method.getDeclaringClass().isAnnotationPresent(ApiDecrypt.class)) {
            return true;
        }
        Object target = joinPoint.getTarget();
        return target != null && target.getClass().isAnnotationPresent(ApiDecrypt.class);
    }

    /**
     * 判断方法参数是否带指定注解。
     */
    private boolean hasParameterAnnotation(Method method, int parameterIndex, Class<? extends Annotation> annotationClass) {
        Annotation[][] annotations = method.getParameterAnnotations();
        if (parameterIndex < 0 || parameterIndex >= annotations.length) {
            return false;
        }
        for (Annotation annotation : annotations[parameterIndex]) {
            if (annotationClass.isInstance(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP的情况，取第一个
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (StrUtil.isBlank(str) || maxLength <= 0) {
            return str;
        }
        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...";
        }
        return str;
    }

    /**
     * 判断是否在排除路径中
     */
    private boolean isExcludePath(String requestUrl) {
        String[] excludePaths = logProperties.getExcludePaths();
        if (excludePaths == null || excludePaths.length == 0) {
            return false;
        }
        for (String path : excludePaths) {
            if (requestUrl.matches(path.replace("**", ".*").replace("*", "[^/]*"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 异步保存日志
     */
    private void saveLogAsync(OperationLogInfo logInfo) {
        try {
            // 使用线程池异步保存日志，避免影响业务性能
            logTaskExecutor.execute(() -> {
                try {
                    logService.saveOperationLog(logInfo);
                } catch (Exception e) {
                    log.error("保存操作日志失败", e);
                }
            });
        } catch (Exception e) {
            log.error("提交日志保存任务失败", e);
        }
    }

    /**
     * 生成traceId
     */
    private String generateTraceId() {
        String timestamp = LocalDateTime.now().format(TRACE_ID_FORMATTER);
        String random = String.valueOf(Math.abs(System.nanoTime() % 1000000));
        return timestamp + random + "_LOG";
    }

    /**
     * 打印入参日志
     */
    private void printRequestLog(String methodName, String requestUrl, String module, String desc, String params) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【入参日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            if (StrUtil.isNotBlank(params)) {
                logMsg.append(", 请求参数: ").append(params);
            }
            log.info(logMsg.toString());
        } catch (Exception e) {
            log.debug("打印入参日志失败", e);
        }
    }

    /**
     * 打印出参日志
     */
    private void printResponseLog(String methodName, String requestUrl, String module, String desc, String response, long executeTime) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【出参日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            if (StrUtil.isNotBlank(response)) {
                logMsg.append(", 返回参数: ").append(response);
            }
            logMsg.append(", 耗时: ").append(executeTime).append("ms");
            log.info(logMsg.toString());
        } catch (Exception e) {
            log.debug("打印出参日志失败", e);
        }
    }

    /**
     * 打印异常日志
     */
    private void printErrorLog(String methodName, String requestUrl, String module, String desc, Throwable e, long executeTime) {
        try {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("【异常日志】");
            logMsg.append("调用 ").append(methodName).append(" 方法");
            logMsg.append(", 接口地址: ").append(requestUrl);
            if (StrUtil.isNotBlank(module)) {
                logMsg.append(", 操作模块: ").append(module);
            }
            if (StrUtil.isNotBlank(desc)) {
                logMsg.append(", 操作描述: ").append(desc);
            }
            logMsg.append(", 耗时: ").append(executeTime).append("ms");
            log.error(logMsg.toString(), e);
        } catch (Exception ex) {
            log.debug("打印异常日志失败", ex);
        }
    }
}
