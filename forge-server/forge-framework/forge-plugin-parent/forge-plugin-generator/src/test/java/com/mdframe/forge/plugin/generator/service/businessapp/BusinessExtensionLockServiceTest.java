package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionLockVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionLockService")
class BusinessExtensionLockServiceTest {

    @Test
    @DisplayName("lock token is returned once while only its SHA-256 digest reaches persistence")
    void lockTokenIsPersistedAsDigest() {
        AtomicReference<String> persistedToken = new AtomicReference<>();
        BusinessExtensionMapper mapper = proxy((method, args) -> {
            if ("tryAcquireLock".equals(method)) {
                persistedToken.set((String) args[4]);
                return 1;
            }
            return null;
        });
        BusinessExtensionLockService service = new BusinessExtensionLockService(mapper);

        BusinessExtensionLockVO lock = service.acquire(10L);

        assertTrue(lock.getLockToken().matches("[a-f0-9]{32}"));
        assertTrue(persistedToken.get().matches("[a-f0-9]{64}"));
        assertNotEquals(lock.getLockToken(), persistedToken.get());
    }

    @Test
    @DisplayName("a lock cannot be released when tenant user or token conditions do not match")
    void lockCannotBeReleasedAcrossTrustedBoundary() {
        BusinessExtensionMapper mapper = proxy((method, args) ->
                "releaseLock".equals(method) ? 0 : null);
        BusinessExtensionLockService service = new BusinessExtensionLockService(mapper);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.release(10L, "0123456789abcdef0123456789abcdef"));

        assertEquals("不能释放其他用户或其他租户的编辑锁", error.getMessage());
    }

    @SuppressWarnings("unchecked")
    private BusinessExtensionMapper proxy(ProxyHandler handler) {
        return (BusinessExtensionMapper) Proxy.newProxyInstance(
                BusinessExtensionMapper.class.getClassLoader(),
                new Class[]{BusinessExtensionMapper.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "BusinessExtensionMapperProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
