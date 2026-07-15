package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BusinessBinding APPLICATION target")
class BusinessBindingApplicationTargetTest {

    @Test
    @DisplayName("application target validates id and code then persists")
    void applicationTargetValidatesIdAndCode() throws Exception {
        StubApplicationService applicationService = new StubApplicationService();
        AtomicReference<AiBusinessBinding> inserted = new AtomicReference<>();
        BusinessBindingMapper mapper = proxy((method, args) -> {
            if ("countByScope".equals(method)) {
                return 0L;
            }
            if ("insert".equals(method)) {
                AiBusinessBinding binding = (AiBusinessBinding) args[0];
                binding.setId(88L);
                inserted.set(binding);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessBindingService service = new BusinessBindingService(null, null, applicationService, null);
        setBaseMapper(service, mapper);

        Long id = service.create(binding("crm_center"));

        assertEquals(88L, id);
        assertEquals("APPLICATION", inserted.get().getTargetType());
        assertEquals(10L, inserted.get().getTargetId());
    }

    @Test
    @DisplayName("application target rejects mismatched code")
    void applicationTargetRejectsMismatchedCode() throws Exception {
        BusinessBindingService service = new BusinessBindingService(
                null, null, new StubApplicationService(), null);
        setBaseMapper(service, proxy(BusinessBindingApplicationTargetTest::defaultValue));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(binding("another_application")));

        assertEquals("业务应用挂接目标ID与编码不一致", error.getMessage());
    }

    @Test
    @DisplayName("legacy APP target still validates an access entry")
    void legacyAppTargetStillValidatesAccessEntry() throws Exception {
        AtomicReference<AiBusinessBinding> inserted = new AtomicReference<>();
        BusinessBindingMapper mapper = proxy((method, args) -> {
            if ("insert".equals(method)) {
                AiBusinessBinding binding = (AiBusinessBinding) args[0];
                binding.setId(99L);
                inserted.set(binding);
                return 1;
            }
            return defaultValue(method, args);
        });
        BusinessBindingService service = new BusinessBindingService(
                null, null, null, new StubEntryService());
        setBaseMapper(service, mapper);
        BusinessBindingDTO dto = binding("legacy_entry");
        dto.setTargetType("APP");
        dto.setTargetId(20L);

        Long id = service.create(dto);

        assertEquals(99L, id);
        assertEquals("APP", inserted.get().getTargetType());
        assertEquals(20L, inserted.get().getTargetId());
    }

    private BusinessBindingDTO binding(String targetCode) {
        BusinessBindingDTO dto = new BusinessBindingDTO();
        dto.setTargetType("APPLICATION");
        dto.setTargetId(10L);
        dto.setTargetCode(targetCode);
        dto.setBindingType("REPORT");
        dto.setBindingKey("sales_dashboard");
        dto.setBindingName("销售看板");
        dto.setStatus(1);
        return dto;
    }

    private static void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    private static BusinessBindingMapper proxy(ProxyHandler handler) {
        return (BusinessBindingMapper) Proxy.newProxyInstance(
                BusinessBindingMapper.class.getClassLoader(), new Class[]{BusinessBindingMapper.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.getName().equals("toString") ? "BusinessBindingMapperProxy" : null;
                    }
                    return handler.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method, Object[] args) {
        return switch (method) {
            case "countByScope" -> 0L;
            case "insert", "updateById", "deleteById" -> 1;
            default -> null;
        };
    }

    @FunctionalInterface
    private interface ProxyHandler {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    private static class StubApplicationService extends BusinessApplicationService {

        StubApplicationService() {
            super(null, null, null);
        }

        @Override
        public AiBusinessApplication requireEntity(Long id) {
            AiBusinessApplication application = new AiBusinessApplication();
            application.setId(10L);
            application.setTenantId(1L);
            application.setApplicationCode("crm_center");
            return application;
        }
    }

    private static class StubEntryService extends BusinessAppService {

        StubEntryService() {
            super(null, null, null, null, null);
        }

        @Override
        public AiBusinessApp requireEntity(Long id) {
            AiBusinessApp entry = new AiBusinessApp();
            entry.setId(20L);
            entry.setTenantId(1L);
            entry.setAppCode("legacy_entry");
            return entry;
        }
    }
}
