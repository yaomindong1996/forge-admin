package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessSuite;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationService")
class BusinessApplicationServiceTest {

    @Test
    @DisplayName("create initializes a tenant-scoped draft application")
    void createInitializesDraftApplication() throws Exception {
        AtomicReference<AiBusinessApplication> inserted = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessApplication application = (AiBusinessApplication) args[0];
                application.setId(101L);
                inserted.set(application);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();

        Long id = service.create(dto);

        assertEquals(101L, id);
        assertNotNull(inserted.get());
        assertEquals(1L, inserted.get().getTenantId());
        assertEquals(BusinessApplicationDesignStatus.DRAFT, inserted.get().getDesignStatus());
        assertEquals("crm_center", inserted.get().getApplicationCode());
    }

    @Test
    @DisplayName("application options reject embedded secrets")
    void applicationOptionsRejectSecrets() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class,
                BusinessApplicationServiceTest::defaultValue);
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setOptions("{\"integration\":{\"client_secret\":\"plain-text\"}}");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto));

        assertTrue(error.getMessage().contains("不能保存"));
    }

    @Test
    @DisplayName("duplicate active application code is rejected")
    void duplicateApplicationCodeIsRejected() throws Exception {
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("countByApplicationCode".equals(method)) {
                return 1L;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(applicationDto()));

        assertTrue(error.getMessage().contains("应用编码已存在"));
    }

    @Test
    @DisplayName("invalid query status is rejected before mapper execution")
    void invalidQueryStatusIsRejected() throws Exception {
        BusinessApplicationService service = service(
                proxy(BusinessApplicationMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationQueryDTO query = new BusinessApplicationQueryDTO();
        query.setStatus(2);

        BusinessException error = assertThrows(BusinessException.class, () -> service.list(query));

        assertEquals("状态值不正确", error.getMessage());
    }

    @Test
    @DisplayName("parent suite filter expands to its complete subtree")
    void parentSuiteFilterExpandsToSubtree() throws Exception {
        AtomicReference<BusinessApplicationQueryDTO> captured = new AtomicReference<>();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectApplicationPage".equals(method)) {
                captured.set((BusinessApplicationQueryDTO) args[2]);
                return args[0];
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationQueryDTO query = new BusinessApplicationQueryDTO();
        query.setSuiteCode("crm");

        service.page(1, 20, query);

        assertEquals(List.of("crm", "crm_sales"), captured.get().getSuiteCodes());
    }

    @Test
    @DisplayName("application code is immutable after creation")
    void applicationCodeIsImmutable() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue),
                proxy(BusinessAppMapper.class, BusinessApplicationServiceTest::defaultValue));
        BusinessApplicationDTO dto = applicationDto();
        dto.setId(existing.getId());
        dto.setApplicationCode("changed_code");

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(dto));

        assertEquals("应用编码创建后不能修改", error.getMessage());
    }

    @Test
    @DisplayName("active access entries block application deletion")
    void activeEntriesBlockDeletion() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        AtomicBoolean detached = new AtomicBoolean();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            return defaultValue(method, args);
        });
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("countActiveByApplicationId".equals(method)) {
                return 1L;
            }
            if ("detachDisabledByApplicationId".equals(method)) {
                detached.set(true);
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper,
                proxy(BusinessApplicationObjectMapper.class, BusinessApplicationServiceTest::defaultValue), appMapper);

        BusinessException error = assertThrows(BusinessException.class, () -> service.delete(existing.getId()));

        assertTrue(error.getMessage().contains("启用的访问入口"));
        assertFalse(detached.get());
    }

    @Test
    @DisplayName("deleting application detaches disabled entries and only deletes composition")
    void deleteDetachesDisabledEntriesAndComposition() throws Exception {
        AiBusinessApplication existing = applicationEntity();
        AtomicBoolean detached = new AtomicBoolean();
        AtomicBoolean compositionDeleted = new AtomicBoolean();
        AtomicBoolean applicationDeleted = new AtomicBoolean();
        BusinessApplicationMapper applicationMapper = proxy(BusinessApplicationMapper.class, (method, args) -> {
            if ("selectEntityById".equals(method)) {
                return existing;
            }
            if ("deleteById".equals(method)) {
                applicationDeleted.set(true);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationObjectMapper objectMapper = proxy(BusinessApplicationObjectMapper.class, (method, args) -> {
            if ("logicDeleteByApplicationId".equals(method)) {
                compositionDeleted.set(true);
                return 2;
            }
            return defaultValue(method, args);
        });
        BusinessAppMapper appMapper = proxy(BusinessAppMapper.class, (method, args) -> {
            if ("countActiveByApplicationId".equals(method)) {
                return 0L;
            }
            if ("detachDisabledByApplicationId".equals(method)) {
                detached.set(true);
                return 3;
            }
            return defaultValue(method, args);
        });
        BusinessApplicationService service = service(applicationMapper, objectMapper, appMapper);

        service.delete(existing.getId());

        assertTrue(detached.get());
        assertTrue(compositionDeleted.get());
        assertTrue(applicationDeleted.get());
    }

    private BusinessApplicationService service(BusinessApplicationMapper applicationMapper,
                                               BusinessApplicationObjectMapper objectMapper,
                                               BusinessAppMapper appMapper) throws Exception {
        BusinessApplicationService service = new BusinessApplicationService(
                new ExistingSuiteService(), objectMapper, appMapper);
        setBaseMapper(service, applicationMapper);
        return service;
    }

    private BusinessApplicationDTO applicationDto() {
        BusinessApplicationDTO dto = new BusinessApplicationDTO();
        dto.setApplicationCode("crm_center");
        dto.setApplicationName("客户经营");
        dto.setSuiteCode("crm");
        dto.setStatus(1);
        return dto;
    }

    private AiBusinessApplication applicationEntity() {
        AiBusinessApplication application = new AiBusinessApplication();
        application.setId(101L);
        application.setTenantId(1L);
        application.setApplicationCode("crm_center");
        application.setApplicationName("客户经营");
        application.setSuiteCode("crm");
        application.setStatus(1);
        application.setDesignStatus(BusinessApplicationDesignStatus.DRAFT);
        return application;
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

    private static Object defaultValue(String method, Object[] args) {
        return switch (method) {
            case "countByApplicationCode", "countByApplicationId", "countActiveByApplicationId" -> 0L;
            case "insert", "updateById", "deleteById", "detachDisabledByApplicationId",
                    "logicDeleteByApplicationId", "insertBatch" -> 1;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    private static class ExistingSuiteService extends BusinessSuiteService {

        ExistingSuiteService() {
            super(null, null, null);
        }

        @Override
        public AiBusinessSuite requireByCode(String suiteCode) {
            AiBusinessSuite suite = new AiBusinessSuite();
            suite.setId(1L);
            suite.setTenantId(1L);
            suite.setSuiteCode(suiteCode);
            return suite;
        }

        @Override
        public List<String> listSelfAndDescendantCodes(String suiteCode) {
            return List.of(suiteCode, suiteCode + "_sales");
        }
    }
}
