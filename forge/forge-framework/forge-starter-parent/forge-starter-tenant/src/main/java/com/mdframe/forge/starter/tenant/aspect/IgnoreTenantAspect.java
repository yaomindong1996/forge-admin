package com.mdframe.forge.starter.tenant.aspect;

import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 租户忽略切面
 * 处理 @IgnoreTenant 注解
 */
@Slf4j
@Aspect
@Component
@Order(1)  // 优先级较高，确保在其他切面之前执行
public class IgnoreTenantAspect {

    /**
     * 拦截标记了 @IgnoreTenant 注解的方法
     */
    @Around("@annotation(com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        IgnoreTenant annotation = method.getAnnotation(IgnoreTenant.class);
        
        if (annotation != null && annotation.value()) {
            // 忽略租户执行
            log.debug("执行忽略租户方法: {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    method.getName());
            
            return TenantContextHolder.executeIgnore(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    log.error("处理异常:",e);
                    throw new RuntimeException(e);
                }
            });
        }
        
        return joinPoint.proceed();
    }
}
