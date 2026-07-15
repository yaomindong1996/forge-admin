package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationObjectService")
class BusinessApplicationObjectServiceTest {

    @Test
    @DisplayName("two primary objects are rejected before replacing associations")
    void twoPrimaryObjectsAreRejected() throws Exception {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubObjectService objectService = new StubObjectService(Map.of(
                1L, object(1L, "customer", "crm"),
                2L, object(2L, "contact", "crm")
        ));
        AtomicBoolean deleted = new AtomicBoolean();
        BusinessApplicationObjectMapper mapper = proxy((method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                deleted.set(true);
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectService service = service(applicationService, objectService, mapper);

        BusinessException error = assertThrows(BusinessException.class, () -> service.replace(10L, List.of(
                association(1L, "PRIMARY"), association(2L, "PRIMARY")
        )));

        assertEquals("一个业务应用最多只能有一个主对象", error.getMessage());
        assertFalse(deleted.get());
    }

    @Test
    @DisplayName("object from another suite cannot join the application")
    void crossSuiteObjectIsRejected() throws Exception {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubObjectService objectService = new StubObjectService(Map.of(
                1L, object(1L, "warehouse", "scm")
        ));
        BusinessApplicationObjectService service = service(
                applicationService, objectService, proxy(BusinessApplicationObjectServiceTest::defaultValue));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replace(10L, List.of(association(1L, "PRIMARY"))));

        assertEquals("业务对象与业务应用必须属于同一业务域", error.getMessage());
    }

    @Test
    @DisplayName("duplicate objects are rejected before replacing associations")
    void duplicateObjectsAreRejected() throws Exception {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubObjectService objectService = new StubObjectService(Map.of(
                1L, object(1L, "customer", "crm")
        ));
        AtomicBoolean deleted = new AtomicBoolean();
        BusinessApplicationObjectMapper mapper = proxy((method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                deleted.set(true);
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectService service = service(applicationService, objectService, mapper);

        BusinessException error = assertThrows(BusinessException.class, () -> service.replace(10L, List.of(
                association(1L, "PRIMARY"), association(1L, "DETAIL")
        )));

        assertEquals("同一业务对象不能重复加入应用", error.getMessage());
        assertFalse(deleted.get());
    }

    @Test
    @DisplayName("invalid object options are rejected before replacing associations")
    void invalidObjectOptionsAreRejected() throws Exception {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubObjectService objectService = new StubObjectService(Map.of(
                1L, object(1L, "customer", "crm")
        ));
        AtomicBoolean deleted = new AtomicBoolean();
        BusinessApplicationObjectMapper mapper = proxy((method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                deleted.set(true);
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectService service = service(applicationService, objectService, mapper);
        BusinessApplicationObjectDTO dto = association(1L, "PRIMARY");
        dto.setOptions("not-json");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.replace(10L, List.of(dto)));

        assertEquals("应用内对象配置必须是合法 JSON 对象", error.getMessage());
        assertFalse(deleted.get());
    }

    @Test
    @DisplayName("valid replacement logically deletes old rows then inserts normalized rows")
    void validReplacementUsesLogicalDeleteAndBatchInsert() throws Exception {
        StubApplicationService applicationService = new StubApplicationService(application());
        StubObjectService objectService = new StubObjectService(Map.of(
                1L, object(1L, "customer", "crm"),
                2L, object(2L, "contact", "crm")
        ));
        AtomicInteger deleteCalls = new AtomicInteger();
        AtomicReference<List<AiBusinessApplicationObject>> inserted = new AtomicReference<>();
        BusinessApplicationObjectMapper mapper = proxy((method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                deleteCalls.incrementAndGet();
                return 2;
            }
            if ("insertBatch".equals(method)) {
                inserted.set((List<AiBusinessApplicationObject>) args[0]);
                return inserted.get().size();
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectService service = service(applicationService, objectService, mapper);

        service.replace(10L, List.of(
                association(1L, "primary"), association(2L, "detail")
        ));

        assertEquals(1, deleteCalls.get());
        assertEquals(2, inserted.get().size());
        assertTrue(inserted.get().stream().allMatch(item -> item.getId() != null));
        assertEquals("PRIMARY", inserted.get().get(0).getObjectRole());
        assertEquals("DETAIL", inserted.get().get(1).getObjectRole());
        assertEquals(1, applicationService.changedCalls);
    }

    private BusinessApplicationObjectService service(StubApplicationService applicationService,
                                                       StubObjectService objectService,
                                                       BusinessApplicationObjectMapper mapper) throws Exception {
        BusinessApplicationObjectService service = new BusinessApplicationObjectService(
                applicationService, objectService);
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
        return service;
    }

    private AiBusinessApplication application() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(10L);
        application.setTenantId(1L);
        application.setSuiteCode("crm");
        application.setApplicationCode("crm_center");
        application.setDesignStatus(BusinessApplicationDesignStatus.DRAFT);
        return application;
    }

    private static AiBusinessObject object(Long id, String code, String suiteCode) {
        AiBusinessObject object = new AiBusinessObject();
        object.setId(id);
        object.setTenantId(1L);
        object.setSuiteCode(suiteCode);
        object.setObjectCode(code);
        return object;
    }

    private BusinessApplicationObjectDTO association(Long objectId, String role) {
        BusinessApplicationObjectDTO dto = new BusinessApplicationObjectDTO();
        dto.setObjectId(objectId);
        dto.setObjectRole(role);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static BusinessApplicationObjectMapper proxy(ProxyHandler handler) {
        return (BusinessApplicationObjectMapper) Proxy.newProxyInstance(
                BusinessApplicationObjectMapper.class.getClassLoader(),
                new Class[]{BusinessApplicationObjectMapper.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("toString") ? "BusinessApplicationObjectMapperProxy" : null;
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method, Object[] args) {
        return switch (method) {
            case "countByApplicationId", "countByObjectId", "countByApplicationAndObjectCode" -> 0L;
            case "logicDeleteByApplicationId", "insertBatch", "insert", "updateById", "deleteById" -> 1;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    private static class StubApplicationService extends BusinessApplicationService {

        private final AiBusinessApplication application;
        private int changedCalls;

        StubApplicationService(AiBusinessApplication application) {
            super(null, null, null);
            this.application = application;
        }

        @Override
        public AiBusinessApplication requireEntity(Long id) {
            return application;
        }

        @Override
        public void markCompositionChanged(Long applicationId) {
            changedCalls++;
        }
    }

    private static class StubObjectService extends BusinessObjectService {

        private final Map<Long, AiBusinessObject> objects;

        StubObjectService(Map<Long, AiBusinessObject> objects) {
            super(null, null, null, null);
            this.objects = objects;
        }

        @Override
        public AiBusinessObject requireEntity(Long id) {
            AiBusinessObject object = objects.get(id);
            if (object == null) {
                throw new BusinessException("业务对象不存在");
            }
            return object;
        }
    }
}
