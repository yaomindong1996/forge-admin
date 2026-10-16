package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessSuiteMapper;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessSuiteService")
class BusinessSuiteServiceTest {

    @Test
    @DisplayName("explicit cleanup removes orphan object relations and logically deletes objects before the suite")
    void deleteCleansOrphanObjectsAfterExplicitConfirmation() throws Exception {
        List<String> operations = new ArrayList<>();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> switch (method) {
            case "countObjectsBySuite" -> 2L;
            case "countActiveApplicationObjectReferencesBySuite" -> 0L;
            case "deleteObjectRelationsBySuite" -> {
                operations.add("relations");
                yield 3;
            }
            case "logicDeleteObjectsBySuite" -> {
                operations.add("objects");
                yield 2;
            }
            case "deleteById" -> {
                operations.add("suite");
                yield 1;
            }
            default -> defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        service.delete(10L, true);

        assertEquals(List.of("relations", "objects", "suite"), operations);
    }

    @Test
    @DisplayName("orphan objects require an explicit cleanup confirmation")
    void deleteRejectsObjectsWithoutExplicitConfirmation() throws Exception {
        AtomicBoolean cleanupCalled = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countObjectsBySuite".equals(method)) {
                return 2L;
            }
            if ("deleteObjectRelationsBySuite".equals(method) || "logicDeleteObjectsBySuite".equals(method)) {
                cleanupCalled.set(true);
            }
            return defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(10L, false));

        assertTrue(error.getMessage().contains("2 个业务对象"));
        assertTrue(error.getMessage().contains("确认"));
        assertFalse(cleanupCalled.get());
    }

    @Test
    @DisplayName("active applications block deletion before orphan object cleanup")
    void deleteRejectsCleanupWhenApplicationStillExists() throws Exception {
        AtomicBoolean objectCountCalled = new AtomicBoolean();
        AtomicBoolean cleanupCalled = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countObjectsBySuite".equals(method)) {
                objectCountCalled.set(true);
                return 2L;
            }
            if ("deleteObjectRelationsBySuite".equals(method) || "logicDeleteObjectsBySuite".equals(method)) {
                cleanupCalled.set(true);
            }
            return defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 1L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(10L, true));

        assertTrue(error.getMessage().contains("业务应用"));
        assertFalse(objectCountCalled.get());
        assertFalse(cleanupCalled.get());
    }

    @Test
    @DisplayName("explicit cleanup disables menus and logically deletes orphan application entries")
    void deleteCleansOrphanApplicationEntriesAfterExplicitConfirmation() throws Exception {
        List<String> operations = new ArrayList<>();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countAppsBySuite".equals(method)) {
                return 2L;
            }
            if ("logicDeleteEntriesBySuite".equals(method)) {
                operations.add("entries");
                return 2;
            }
            if ("deleteById".equals(method)) {
                operations.add("suite");
                return 1;
            }
            return defaultSuiteValue(method);
        });
        AiBusinessApp entry = new AiBusinessApp();
        entry.setId(21L);
        entry.setSuiteCode("sales");
        entry.setOptions("{\"adminMenu\":{\"menuResourceId\":\"88\"}}");
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("selectAppsBySuiteCodes".equals(method)) {
                return List.of(entry);
            }
            return defaultValue(method);
        });
        MenuRegisterAdapter menuAdapter = proxy(MenuRegisterAdapter.class, (method, args) -> {
            if ("disableMenu".equals(method)) {
                operations.add("menu:" + args[0]);
            }
            return defaultValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L, appMapper, menuAdapter);

        service.delete(10L, true);

        assertEquals(List.of("menu:88", "entries", "suite"), operations);
    }

    @Test
    @DisplayName("orphan application entries require an explicit cleanup confirmation")
    void deleteRejectsOrphanApplicationEntriesWithoutExplicitConfirmation() throws Exception {
        AtomicBoolean cleanupCalled = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countAppsBySuite".equals(method)) {
                return 2L;
            }
            if ("logicDeleteEntriesBySuite".equals(method)) {
                cleanupCalled.set(true);
            }
            return defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(10L, false));

        assertTrue(error.getMessage().contains("2 个访问入口"));
        assertTrue(error.getMessage().contains("确认"));
        assertFalse(cleanupCalled.get());
    }

    @Test
    @DisplayName("application entries attached to an active application fail closed")
    void deleteRejectsEntriesReferencedByActiveApplication() throws Exception {
        AtomicBoolean cleanupCalled = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countAppsBySuite".equals(method)
                    || "countActiveApplicationEntryReferencesBySuite".equals(method)) {
                return 1L;
            }
            if ("logicDeleteEntriesBySuite".equals(method)) {
                cleanupCalled.set(true);
            }
            return defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(10L, true));

        assertTrue(error.getMessage().contains("仍被业务应用使用"));
        assertFalse(cleanupCalled.get());
    }

    @Test
    @DisplayName("combined cleanup removes entries before object metadata and the suite")
    void deleteCleansEntriesAndObjectsInDependencyOrder() throws Exception {
        List<String> operations = new ArrayList<>();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> switch (method) {
            case "countAppsBySuite" -> 1L;
            case "countObjectsBySuite" -> 2L;
            case "logicDeleteEntriesBySuite" -> {
                operations.add("entries");
                yield 1;
            }
            case "deleteObjectRelationsBySuite" -> {
                operations.add("relations");
                yield 2;
            }
            case "logicDeleteObjectsBySuite" -> {
                operations.add("objects");
                yield 2;
            }
            case "deleteById" -> {
                operations.add("suite");
                yield 1;
            }
            default -> defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        service.delete(10L, true);

        assertEquals(List.of("entries", "relations", "objects", "suite"), operations);
    }

    @Test
    @DisplayName("objects referenced by an active application fail closed")
    void deleteRejectsObjectsReferencedByActiveApplication() throws Exception {
        AtomicBoolean cleanupCalled = new AtomicBoolean();
        AtomicBoolean menuDisabled = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("countAppsBySuite".equals(method)
                    || "countObjectsBySuite".equals(method)
                    || "countActiveApplicationObjectReferencesBySuite".equals(method)) {
                return 1L;
            }
            if ("logicDeleteEntriesBySuite".equals(method)
                    || "deleteObjectRelationsBySuite".equals(method)
                    || "logicDeleteObjectsBySuite".equals(method)) {
                cleanupCalled.set(true);
            }
            return defaultSuiteValue(method);
        });
        AiBusinessApp entry = new AiBusinessApp();
        entry.setOptions("{\"adminMenu\":{\"menuResourceId\":\"88\"}}");
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("selectAppsBySuiteCodes".equals(method)) {
                return List.of(entry);
            }
            return defaultValue(method);
        });
        MenuRegisterAdapter menuAdapter = proxy(MenuRegisterAdapter.class, (method, args) -> {
            if ("disableMenu".equals(method)) {
                menuDisabled.set(true);
            }
            return defaultValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L, appMapper, menuAdapter);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(10L, true));

        assertTrue(error.getMessage().contains("仍被业务应用使用"));
        assertFalse(menuDisabled.get());
        assertFalse(cleanupCalled.get());
    }

    @Test
    @DisplayName("an empty suite is deleted without requesting object cleanup")
    void deleteRemovesEmptySuiteWithoutCleanupConfirmation() throws Exception {
        AtomicBoolean suiteDeleted = new AtomicBoolean();
        BusinessSuiteMapper suiteMapper = suiteMapper((method, args) -> {
            if ("deleteById".equals(method)) {
                suiteDeleted.set(true);
                return 1;
            }
            return defaultSuiteValue(method);
        });
        BusinessSuiteService service = service(suiteMapper, 0L);

        service.delete(10L, false);

        assertTrue(suiteDeleted.get());
    }

    private BusinessSuiteService service(BusinessSuiteMapper suiteMapper, Long applicationCount) throws Exception {
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("selectAppsBySuiteCodes".equals(method)) {
                return List.of();
            }
            return defaultValue(method);
        });
        return service(suiteMapper, applicationCount, appMapper, null);
    }

    private BusinessSuiteService service(BusinessSuiteMapper suiteMapper,
                                         Long applicationCount,
                                         BusinessAppMapper appMapper,
                                         MenuRegisterAdapter menuAdapter) throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countBySuiteCode".equals(method)) {
                return applicationCount;
            }
            return defaultValue(method);
        });
        BusinessSuiteService service = new BusinessSuiteService(
                menuAdapter,
                appMapper,
                applicationMapper);
        setBaseMapper(service, suiteMapper);
        return service;
    }

    private BusinessSuiteMapper suiteMapper(ProxyHandler handler) {
        return proxy(BusinessSuiteMapper.class, (method, args) -> {
            if ("selectById".equals(method)) {
                return suite();
            }
            return handler.invoke(method, args);
        });
    }

    private AiBusinessSuite suite() {
        AiBusinessSuite suite = new AiBusinessSuite();
        suite.setId(10L);
        suite.setTenantId(1L);
        suite.setSuiteCode("sales");
        suite.setSuiteName("销售管理");
        suite.setStatus(1);
        suite.setDelFlag(0L);
        return suite;
    }

    private static Object defaultSuiteValue(String method) {
        return switch (method) {
            case "countChildrenBySuite", "countObjectsBySuite", "countAppsBySuite",
                    "countActiveApplicationEntryReferencesBySuite",
                    "countActiveApplicationObjectReferencesBySuite" -> 0L;
            case "logicDeleteEntriesBySuite", "deleteObjectRelationsBySuite",
                    "logicDeleteObjectsBySuite", "deleteById" -> 1;
            default -> null;
        };
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "insert", "updateById", "deleteById" -> 1;
            default -> null;
        };
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = org.springframework.util.ReflectionUtils.findField(ServiceImpl.class, "baseMapper");
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

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
