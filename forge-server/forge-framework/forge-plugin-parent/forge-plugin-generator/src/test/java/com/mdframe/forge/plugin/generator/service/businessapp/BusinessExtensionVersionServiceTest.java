package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionVersionDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("BusinessExtensionVersionService")
class BusinessExtensionVersionServiceTest {

    @Test
    @DisplayName("saving content appends a version and preserves the enabled runtime version")
    void saveAppendsVersionAndPreservesEnabledVersion() throws Exception {
        AiBusinessExtension extension = enabledExtension();
        AtomicReference<AiBusinessExtensionVersion> inserted = new AtomicReference<>();
        AtomicReference<String> nextStatus = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return extension;
            }
            if ("updateDraftVersion".equals(method)) {
                nextStatus.set((String) args[3]);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) -> {
            if ("selectMaxVersionNo".equals(method)) {
                return 3;
            }
            if ("insert".equals(method)) {
                AiBusinessExtensionVersion version = (AiBusinessExtensionVersion) args[0];
                version.setId(44L);
                inserted.set(version);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionService service = new BusinessExtensionVersionService(
                extensionMapper, new PermissiveLockService(extensionMapper), new ObjectMapper(),
                new BusinessExtensionStateMachine());
        setBaseMapper(service, versionMapper);
        BusinessExtensionVersionDTO dto = new BusinessExtensionVersionDTO();
        dto.setContent("return { changed: true }");
        dto.setConfigJson("{}");
        dto.setLockToken("lock-token");

        Integer versionNo = service.saveDraft(extension.getId(), dto);

        assertEquals(4, versionNo);
        assertEquals(BusinessExtensionStatus.DRAFT, nextStatus.get());
        assertEquals(3, extension.getEnabledVersion());
        assertEquals(4, inserted.get().getVersionNo());
    }

    @Test
    @DisplayName("rollback copies history into a new draft without mutating the source")
    void rollbackCreatesNewDraft() throws Exception {
        AiBusinessExtension extension = enabledExtension();
        AiBusinessExtensionVersion source = new AiBusinessExtensionVersion();
        source.setId(12L);
        source.setExtensionId(extension.getId());
        source.setVersionNo(1);
        source.setContent("return { historical: true }");
        source.setContentHash("source-hash");
        AtomicReference<AiBusinessExtensionVersion> inserted = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) ->
                "selectEntityById".equals(method) ? extension : defaultValue(method));
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) -> {
            if ("selectVersion".equals(method)) {
                return source;
            }
            if ("selectMaxVersionNo".equals(method)) {
                return 3;
            }
            if ("insert".equals(method)) {
                AiBusinessExtensionVersion version = (AiBusinessExtensionVersion) args[0];
                version.setId(55L);
                inserted.set(version);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionService service = new BusinessExtensionVersionService(
                extensionMapper, new PermissiveLockService(extensionMapper), new ObjectMapper(),
                new BusinessExtensionStateMachine());
        setBaseMapper(service, versionMapper);

        Integer rollbackVersion = service.rollback(extension.getId(), 1, "lock-token");

        assertEquals(4, rollbackVersion);
        assertEquals(1, source.getVersionNo());
        assertEquals(4, inserted.get().getVersionNo());
        assertNotEquals(source.getId(), inserted.get().getId());
        assertEquals(source.getContent(), inserted.get().getContent());
    }

    private AiBusinessExtension enabledExtension() {
        AiBusinessExtension extension = new AiBusinessExtension();
        extension.setId(9L);
        extension.setTenantId(1L);
        extension.setExtensionCode("client_rule");
        extension.setExtensionType("CLIENT_JS");
        extension.setStatus(BusinessExtensionStatus.ENABLED);
        extension.setDraftVersion(3);
        extension.setEnabledVersion(3);
        return extension;
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "insert", "updateDraftVersion", "releaseLock", "renewLock", "tryAcquireLock" -> 1;
            case "countOwnedLock" -> 1L;
            default -> null;
        };
    }

    private static class PermissiveLockService extends BusinessExtensionLockService {
        PermissiveLockService(BusinessExtensionMapper mapper) {
            super(mapper);
        }

        @Override
        public void assertOwned(Long extensionId, String lockToken) {
            // Version tests focus on immutable append behavior; lock isolation has its own tests.
        }
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
