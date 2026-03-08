package com.mdframe.forge.starter.trans.aspect;

import com.mdframe.forge.starter.trans.annotation.DictTranslate;
import com.mdframe.forge.starter.trans.manager.TransManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 方法级字典翻译切面
 * 对标记了 @DictTranslate 的方法返回值进行自动翻译
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DictTranslateAspect {

    private final TransManager transManager;

    @Around("@annotation(com.mdframe.forge.starter.trans.annotation.DictTranslate)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DictTranslate dictTranslate = method.getAnnotation(DictTranslate.class);
        if (dictTranslate == null || !dictTranslate.enabled()) {
            return result;
        }

        if (result == null) {
            return null;
        }

        try {
            // 支持对象或集合的自动翻译
            if (result instanceof Iterable<?>) {
                transManager.translate((Iterable<?>) result);
            } else {
                transManager.translate(result);
            }
        } catch (Exception e) {
            log.warn("方法返回值字典翻译失败: {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), e);
        }

        return result;
    }
}
