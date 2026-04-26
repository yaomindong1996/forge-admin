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
import java.util.List;

/**
 * 方法级字典翻译切面
 * 对标记了 @DictTranslate 的方法返回值进行自动翻译
 * 支持：单个对象、Iterable（List 等）、分页对象（含 getRecords() 方法，如 MyBatis-Plus Page）
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
            if (result instanceof Iterable<?>) {
                // List / Set 等可迭代集合
                transManager.translate((Iterable<?>) result);
            } else if (isPageObject(result)) {
                // 分页对象（如 MyBatis-Plus Page），通过反射取 getRecords() 翻译
                List<?> records = getRecords(result);
                if (records != null && !records.isEmpty()) {
                    transManager.translate(records);
                }
            } else {
                transManager.translate(result);
            }
        } catch (Exception e) {
            log.warn("方法返回值字典翻译失败: {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), e);
        }

        return result;
    }

    /**
     * 判断对象是否为分页对象（具有 getRecords() 方法）
     */
    private boolean isPageObject(Object obj) {
        try {
            obj.getClass().getMethod("getRecords");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 通过反射调用 getRecords() 获取分页数据列表
     */
    @SuppressWarnings("unchecked")
    private List<?> getRecords(Object pageObj) {
        try {
            Method getRecords = pageObj.getClass().getMethod("getRecords");
            Object records = getRecords.invoke(pageObj);
            if (records instanceof List) {
                return (List<?>) records;
            }
        } catch (Exception e) {
            log.warn("从分页对象获取 records 失败", e);
        }
        return null;
    }
}
