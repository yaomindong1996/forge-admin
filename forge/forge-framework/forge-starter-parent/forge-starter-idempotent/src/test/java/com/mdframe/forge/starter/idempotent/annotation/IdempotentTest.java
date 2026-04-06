package com.mdframe.forge.starter.idempotent.annotation;

import com.mdframe.forge.starter.idempotent.constant.IdempotentConstant;
import com.mdframe.forge.starter.idempotent.constant.IdempotentStrategy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Idempotent注解测试
 */
@Tag("dev")
class IdempotentTest {

    @Idempotent
    public void methodWithDefaultValues() {
    }

    @Idempotent(
            prefix = "test:",
            expire = 300,
            key = "'test:' + #id",
            message = "自定义消息",
            deleteKeyAfterSuccess = true,
            strategy = IdempotentStrategy.STRICT,
            cacheExpire = 1800,
            cacheResult = false,
            enableMetrics = false
    )
    public void methodWithCustomValues(Long id) {
    }

    @Test
    void testDefaultValues() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("methodWithDefaultValues");
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        assertNotNull(annotation);
        assertEquals(IdempotentConstant.DEFAULT_PREFIX, annotation.prefix());
        assertEquals(IdempotentConstant.DEFAULT_EXPIRE, annotation.expire());
        assertEquals("", annotation.key());
        assertEquals(IdempotentConstant.DEFAULT_MESSAGE, annotation.message());
        assertFalse(annotation.deleteKeyAfterSuccess());
        assertEquals(IdempotentStrategy.RETURN_CACHE, annotation.strategy());
        assertEquals(IdempotentConstant.CACHE_EXPIRE_DEFAULT, annotation.cacheExpire());
        assertTrue(annotation.cacheResult());
        assertTrue(annotation.enableMetrics());
    }

    @Test
    void testCustomValues() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("methodWithCustomValues", Long.class);
        Idempotent annotation = method.getAnnotation(Idempotent.class);

        assertNotNull(annotation);
        assertEquals("test:", annotation.prefix());
        assertEquals(300, annotation.expire());
        assertEquals("'test:' + #id", annotation.key());
        assertEquals("自定义消息", annotation.message());
        assertTrue(annotation.deleteKeyAfterSuccess());
        assertEquals(IdempotentStrategy.STRICT, annotation.strategy());
        assertEquals(1800, annotation.cacheExpire());
        assertFalse(annotation.cacheResult());
        assertFalse(annotation.enableMetrics());
    }

    @Test
    void testAnnotationRetention() throws NoSuchMethodException {
        Method method = this.getClass().getMethod("methodWithDefaultValues");
        Idempotent annotation = method.getAnnotation(Idempotent.class);
        
        // 验证注解在运行时可用
        assertNotNull(annotation, "注解应该在运行时可用");
        
        // 验证注解类型是Idempotent
        assertEquals(Idempotent.class, annotation.annotationType());
        
        // 验证注解有Retention注解，且策略为RUNTIME
        Retention retention = Idempotent.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void testAnnotationTarget() {
        // 验证注解可以用在方法上
        assertTrue(this.getClass().isAnnotationPresent(Idempotent.class) || 
                   hasMethodWithAnnotation());
    }

    private boolean hasMethodWithAnnotation() {
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Idempotent.class)) {
                return true;
            }
        }
        return false;
    }
}
