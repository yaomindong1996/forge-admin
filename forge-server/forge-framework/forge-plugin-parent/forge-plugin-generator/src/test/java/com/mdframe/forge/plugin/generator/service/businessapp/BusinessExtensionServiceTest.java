package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessExtensionService")
class BusinessExtensionServiceTest {

    @Test
    @DisplayName("create persists a tenant-scoped draft and immutable version one")
    void createPersistsDraftAndVersionOne() throws Exception {
        AtomicReference<AiBusinessExtension> extensionRef = new AtomicReference<>();
        AtomicReference<AiBusinessExtensionVersion> versionRef = new AtomicReference<>();
        BusinessExtensionMapper extensionMapper = proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectApplicationById".equals(method)) {
                AiBusinessApplication application = new AiBusinessApplication();
                application.setId(10L);
                application.setTenantId(1L);
                return application;
            }
            if ("countByExtensionCode".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessExtension extension = (AiBusinessExtension) args[0];
                extension.setId(20L);
                extensionRef.set(extension);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionVersionMapper versionMapper = proxy(BusinessExtensionVersionMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                AiBusinessExtensionVersion version = (AiBusinessExtensionVersion) args[0];
                version.setId(30L);
                versionRef.set(version);
                return 1;
            }
            return defaultValue(method);
        });
        BusinessExtensionService service = new BusinessExtensionService(
                versionMapper, new ObjectMapper(), changeTracker());
        setBaseMapper(service, extensionMapper);

        Long id = service.create(extensionDto());

        assertEquals(20L, id);
        assertNotNull(extensionRef.get());
        assertEquals(1L, extensionRef.get().getTenantId());
        assertEquals(BusinessExtensionStatus.DRAFT, extensionRef.get().getStatus());
        assertEquals(1, extensionRef.get().getDraftVersion());
        assertNotNull(versionRef.get());
        assertEquals(1, versionRef.get().getVersionNo());
        assertTrue(versionRef.get().getContentHash().matches("[a-f0-9]{64}"));
    }

    @Test
    @DisplayName("object target must belong to the same application")
    void objectTargetMustBelongToApplication() throws Exception {
        BusinessExtensionMapper mapper = proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectApplicationById".equals(method)) {
                AiBusinessApplication application = new AiBusinessApplication();
                application.setId(10L);
                application.setTenantId(1L);
                return application;
            }
            if ("countApplicationObject".equals(method)) {
                return 0L;
            }
            return defaultValue(method);
        });
        BusinessExtensionService service = new BusinessExtensionService(
                proxy(BusinessExtensionVersionMapper.class, (method, args) -> defaultValue(method)),
                new ObjectMapper(), changeTracker());
        setBaseMapper(service, mapper);
        BusinessExtensionDTO dto = extensionDto();
        dto.setObjectId(99L);

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto));

        assertTrue(error.getMessage().contains("业务对象") && error.getMessage().contains("应用"));
    }

    @Test
    @DisplayName("plain secrets are rejected from extension configuration")
    void plainSecretsAreRejected() throws Exception {
        BusinessExtensionService service = new BusinessExtensionService(
                proxy(BusinessExtensionVersionMapper.class, (method, args) -> defaultValue(method)),
                new ObjectMapper(), changeTracker());
        setBaseMapper(service, proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectApplicationById".equals(method)) {
                AiBusinessApplication application = new AiBusinessApplication();
                application.setId(10L);
                return application;
            }
            return defaultValue(method);
        }));
        BusinessExtensionDTO dto = extensionDto();
        dto.setConfigJson("{\"apiToken\":\"plain-secret\"}");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto));

        assertTrue(error.getMessage().contains("敏感") || error.getMessage().contains("密钥"));
    }

    @Test
    @DisplayName("client scripts cannot bind database write hooks")
    void clientScriptCannotBindDatabaseWriteHook() throws Exception {
        BusinessExtensionService service = new BusinessExtensionService(
                proxy(BusinessExtensionVersionMapper.class, (method, args) -> defaultValue(method)),
                new ObjectMapper(), changeTracker());
        setBaseMapper(service, proxy(BusinessExtensionMapper.class, (method, args) -> {
            if ("selectApplicationById".equals(method)) {
                AiBusinessApplication application = new AiBusinessApplication();
                application.setId(10L);
                return application;
            }
            return defaultValue(method);
        }));
        BusinessExtensionDTO dto = extensionDto();
        dto.setExtensionType("CLIENT_JS");
        dto.setHookCode("BEFORE_CREATE");
        dto.setContent("return {};");

        BusinessException error = assertThrows(BusinessException.class, () -> service.create(dto));

        assertTrue(error.getMessage().contains("不支持钩子"));
    }

    private BusinessExtensionDTO extensionDto() {
        BusinessExtensionDTO dto = new BusinessExtensionDTO();
        dto.setApplicationId(10L);
        dto.setExtensionCode("validate_customer");
        dto.setExtensionName("客户提交校验");
        dto.setExtensionType("VISUAL_RULE");
        dto.setHookCode("BEFORE_SUBMIT");
        dto.setFailurePolicy("BLOCK");
        dto.setRiskLevel("MEDIUM");
        dto.setContent("{\"match\":\"ALL\",\"conditions\":[],\"actions\":[]}");
        dto.setConfigJson("{}");
        return dto;
    }

    private static BusinessApplicationChangeTracker changeTracker() {
        BusinessApplicationMapper mapper = proxy(BusinessApplicationMapper.class,
                (method, args) -> "markChanged".equals(method) ? 1 : defaultValue(method));
        return new BusinessApplicationChangeTracker(mapper);
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
            case "insert", "updateById", "deleteById", "updateDraftVersion" -> 1;
            case "countByExtensionCode", "countApplicationObject", "countApplicationEntry" -> 0L;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }
}
