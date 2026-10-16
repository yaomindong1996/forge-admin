package com.mdframe.forge.starter.tenant.aspect;

import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 租户忽略切面
 * 处理 @IgnoreTenant 注解（同时支持方法级与类级标注）
 */
@Slf4j
@Aspect
@Component
@Order(1)  // 优先级较高，确保在其他切面之前执行
public class IgnoreTenantAspect {

    /**
     * 拦截标记了 @IgnoreTenant 注解的方法或所在类。
     * <p>注解 {@link IgnoreTenant} 声明了 {@code @Target({TYPE, METHOD})}，因此需同时匹配
     * 方法级（{@code @annotation}）与类级（{@code @within}）标注，否则类级标注会被静默忽略，
     * 典型场景如定时任务 Handler 在无租户上下文的调度线程中执行时租户条件被追加为
     * {@code tenant_id = NULL} 导致查不到数据。</p>
     */
    @Around("@annotation(com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant) "
            + "|| @within(com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        IgnoreTenant annotation = resolveIgnoreTenant(method, joinPoint.getTarget());

        if (annotation != null && annotation.value()) {
            // 忽略租户执行
            log.debug("执行忽略租户方法: {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    method.getName());

            return TenantContextHolder.executeIgnore(() -> {
                try {
                    return joinPoint.proceed();
                } catch (RuntimeException | Error e) {
                    // 保留原异常类型（如 ResponseStatusException/BusinessException），避免被包装成 500
                    throw e;
                } catch (Throwable e) {
                    log.error("忽略租户切面捕获受检异常: {}.{}",
                            joinPoint.getTarget().getClass().getSimpleName(),
                            method.getName(), e);
                    throw new RuntimeException(e);
                }
            });
        }

        return joinPoint.proceed();
    }

    /**
     * 解析生效的 @IgnoreTenant：优先方法级标注，其次回退到目标类（含父类/接口）的类级标注。
     */
    private IgnoreTenant resolveIgnoreTenant(Method method, Object target) {
        IgnoreTenant onMethod = method.getAnnotation(IgnoreTenant.class);
        if (onMethod != null) {
            return onMethod;
        }
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, IgnoreTenant.class);
    }
}
