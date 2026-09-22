package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessObjectService page-form orphan reclaim")
class BusinessObjectPageFormOrphanReclaimTest {

    @Test
    @DisplayName("reclaims unused PAGE_FORM object so the same object code can be reused")
    void reclaimsUnusedPageFormObject() throws Exception {
        AiBusinessObject orphan = object(11L, "indicator_group", "kpi",
                "{\"managedBy\":\"PAGE_FORM\",\"sourcePageId\":\"page_old\"}");
        AtomicBoolean removed = new AtomicBoolean();
        AtomicInteger relationDeletes = new AtomicInteger();
        AtomicInteger modelDeletes = new AtomicInteger();

        BusinessObjectMapper objectMapper = proxy(BusinessObjectMapper.class, (method, args) -> {
            if ("selectFirstByObjectCode".equals(method)) {
                return orphan;
            }
            if ("countAppsByObject".equals(method)) {
                return 0L;
            }
            if ("selectByModelId".equals(method)) {
                return null;
            }
            if ("logicDeleteModelById".equals(method)) {
                modelDeletes.incrementAndGet();
                return 1;
            }
            return defaultValue(method);
        });
        BusinessApplicationObjectMapper applicationObjectMapper = proxy(BusinessApplicationObjectMapper.class,
                (method, args) -> "countByObjectId".equals(method) ? 0L : defaultValue(method));
        BusinessObjectRelationMapper relationMapper = proxy(BusinessObjectRelationMapper.class, (method, args) -> {
            if ("deleteRelationsByObjectCode".equals(method)) {
                relationDeletes.incrementAndGet();
                return null;
            }
            return defaultValue(method);
        });

        BusinessObjectService reclaimService = new BusinessObjectService(
                null, null, applicationObjectMapper, relationMapper, null) {
            @Override
            public boolean removeById(Serializable id) {
                removed.set(true);
                return true;
            }
        };
        setBaseMapper(reclaimService, objectMapper);

        assertTrue(reclaimService.reclaimUnusedPageFormObject("indicator_group"));
        assertTrue(removed.get());
        assertEquals(1, relationDeletes.get());
        assertEquals(1, modelDeletes.get());
    }

    @Test
    @DisplayName("does not reclaim PAGE_FORM object still linked to an application")
    void keepsLinkedPageFormObject() throws Exception {
        AiBusinessObject linked = object(12L, "indicator_group", "kpi",
                "{\"managedBy\":\"PAGE_FORM\",\"sourcePageId\":\"page_live\"}");
        BusinessObjectMapper objectMapper = proxy(BusinessObjectMapper.class, (method, args) -> {
            if ("selectFirstByObjectCode".equals(method)) {
                return linked;
            }
            return defaultValue(method);
        });
        BusinessApplicationObjectMapper applicationObjectMapper = proxy(BusinessApplicationObjectMapper.class,
                (method, args) -> "countByObjectId".equals(method) ? 1L : defaultValue(method));
        BusinessObjectService service = service(objectMapper, applicationObjectMapper,
                proxy(BusinessObjectRelationMapper.class, (method, args) -> defaultValue(method)));

        assertFalse(service.reclaimUnusedPageFormObject("indicator_group"));
    }

    @Test
    @DisplayName("does not reclaim manually created business objects")
    void keepsManualObjects() throws Exception {
        AiBusinessObject manual = object(13L, "indicator_group", "kpi", null);
        BusinessObjectMapper objectMapper = proxy(BusinessObjectMapper.class, (method, args) -> {
            if ("selectFirstByObjectCode".equals(method)) {
                return manual;
            }
            return defaultValue(method);
        });
        BusinessObjectService service = service(objectMapper,
                proxy(BusinessApplicationObjectMapper.class, (method, args) -> defaultValue(method)),
                proxy(BusinessObjectRelationMapper.class, (method, args) -> defaultValue(method)));

        assertFalse(service.reclaimUnusedPageFormObject("indicator_group"));
    }

    private BusinessObjectService service(BusinessObjectMapper objectMapper,
                                          BusinessApplicationObjectMapper applicationObjectMapper,
                                          BusinessObjectRelationMapper relationMapper) throws Exception {
        BusinessObjectService service = new BusinessObjectService(
                null, null, applicationObjectMapper, relationMapper, null);
        setBaseMapper(service, objectMapper);
        return service;
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = findField(ServiceImpl.class, "baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static AiBusinessObject object(Long id, String code, String suiteCode, String options) {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(id);
        object.setTenantId(1L);
        object.setSuiteCode(suiteCode);
        object.setObjectCode(code);
        object.setModelId(99L);
        object.setOptions(options);
        return object;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, ProxyHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("toString") ? type.getSimpleName() + "Proxy" : null;
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "countByObjectId", "countAppsByObject", "countRelationsByObject", "countActiveByObjectCode" -> 0L;
            case "logicDeleteModelById", "deleteRelationsByObjectCode", "insert", "updateById", "deleteById" -> 1;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
