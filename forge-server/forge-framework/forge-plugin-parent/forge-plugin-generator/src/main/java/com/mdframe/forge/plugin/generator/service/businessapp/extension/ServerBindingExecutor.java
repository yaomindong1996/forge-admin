package com.mdframe.forge.plugin.generator.service.businessapp.extension;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务端白名单绑定执行器。
 */
@Service
public class ServerBindingExecutor implements AutoCloseable {

    private static final int MIN_TIMEOUT_MS = 10;
    private static final int MAX_TIMEOUT_MS = 5000;
    private static final int MAX_INPUT_FIELDS = 100;

    private final LowcodeExtensionRegistry registry;
    private final ExecutorService executorService;

    public ServerBindingExecutor(LowcodeExtensionRegistry registry) {
        this.registry = registry;
        this.executorService = Executors.newFixedThreadPool(4, new ExtensionThreadFactory());
    }

    public ExtensionExecutionResult execute(ExtensionExecutionContext context) {
        validateTrustedContext(context);
        LowcodeExtensionHandler handler = registry.require(context.getHandlerCode());
        if (handler.allowedHooks() == null || !handler.allowedHooks().contains(context.getHookCode())) {
            throw new BusinessException("处理器不允许在当前扩展钩子执行");
        }
        String requiredPermission = StringUtils.trimToNull(handler.requiredPermission());
        if (requiredPermission != null && !hasPermission(requiredPermission)) {
            throw new BusinessException(403, "没有服务端扩展处理器所需权限");
        }
        context.setInput(validateInput(handler.inputSchema(), context.getInput()));
        int timeoutMs = Math.max(MIN_TIMEOUT_MS, Math.min(handler.timeoutMs(), MAX_TIMEOUT_MS));
        Future<ExtensionExecutionResult> future = executorService.submit(() -> handler.execute(context));
        try {
            ExtensionExecutionResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new BusinessException("服务端扩展没有返回结构化结果");
            }
            validateOutputSize(result.getOutput());
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException("服务端扩展执行超时");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new BusinessException("服务端扩展执行被中断");
        } catch (ExecutionException e) {
            throw new BusinessException("服务端扩展执行失败，请查看脱敏审计摘要");
        }
    }

    private void validateTrustedContext(ExtensionExecutionContext context) {
        if (context == null || context.getApplicationId() == null || context.getExtensionId() == null) {
            throw new BusinessException("扩展执行上下文不完整");
        }
        Long sessionTenantId = resolveTenantId();
        if (context.getTenantId() == null) {
            context.setTenantId(sessionTenantId);
        } else if (!sessionTenantId.equals(context.getTenantId())) {
            throw new BusinessException("扩展执行上下文跨租户");
        }
        if (context.getActorUserId() == null) {
            context.setActorUserId(resolveUserId());
        }
        if (StringUtils.isBlank(context.getHookCode())) {
            throw new BusinessException("扩展执行钩子不能为空");
        }
    }

    private Map<String, Object> validateInput(Map<String, ExtensionInputField> schema, Map<String, Object> input) {
        Map<String, ExtensionInputField> safeSchema = schema == null ? Map.of() : schema;
        Map<String, Object> safeInput = input == null ? Map.of() : input;
        if (safeInput.size() > MAX_INPUT_FIELDS) {
            throw new BusinessException("服务端扩展输入字段过多");
        }
        for (String key : safeInput.keySet()) {
            if (!safeSchema.containsKey(key)) {
                throw new BusinessException("服务端扩展输入包含未声明字段: " + key);
            }
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, ExtensionInputField> entry : safeSchema.entrySet()) {
            Object value = safeInput.get(entry.getKey());
            ExtensionInputField field = entry.getValue();
            if (field != null && field.isRequired() && value == null) {
                throw new BusinessException("服务端扩展缺少必填输入: " + entry.getKey());
            }
            if (value != null && field != null && !matchesType(field.getType(), value)) {
                throw new BusinessException("服务端扩展输入类型不正确: " + entry.getKey());
            }
            if (value != null) {
                normalized.put(entry.getKey(), value);
            }
        }
        return normalized;
    }

    private boolean matchesType(String type, Object value) {
        return switch (StringUtils.defaultString(type).toUpperCase(Locale.ROOT)) {
            case "STRING" -> value instanceof String;
            case "LONG", "INTEGER" -> value instanceof Byte || value instanceof Short
                    || value instanceof Integer || value instanceof Long;
            case "NUMBER", "DECIMAL" -> value instanceof Number || value instanceof BigDecimal;
            case "BOOLEAN" -> value instanceof Boolean;
            case "OBJECT", "MAP" -> value instanceof Map<?, ?>;
            case "ARRAY", "LIST" -> value instanceof Collection<?>;
            default -> false;
        };
    }

    private void validateOutputSize(Map<String, Object> output) {
        if (output != null && output.toString().length() > 64 * 1024) {
            throw new BusinessException("服务端扩展输出超过64KB限制");
        }
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private Long resolveTenantId() {
        try {
            Long value = SessionHelper.getTenantId();
            return value == null ? 1L : value;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long resolveUserId() {
        try {
            Long value = SessionHelper.getUserId();
            return value == null ? 1L : value;
        } catch (Exception e) {
            return 1L;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        executorService.shutdownNow();
    }

    private static final class ExtensionThreadFactory implements ThreadFactory {

        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "lowcode-extension-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
