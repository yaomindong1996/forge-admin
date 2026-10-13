package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.mapper.SysClientMapper;
import com.mdframe.forge.plugin.system.security.ClientCredentialPolicy;
import com.mdframe.forge.plugin.system.security.ClientSecretCodec;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientServiceImplBuiltInProtectionTest {

    @Test
    void shouldRejectDeletingBuiltInClient() {
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> {
            if ("selectById".equals(methodName)) {
                return builtIn("pc");
            }
            return defaultValue(methodName);
        });
        ClientServiceImpl service = service(mapper);

        assertThatThrownBy(() -> service.deleteClient(6L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能删除");
    }

    @Test
    void shouldRejectRenamingOrDisablingBuiltInClient() {
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> {
            if ("selectById".equals(methodName)) {
                return builtIn("h5");
            }
            return defaultValue(methodName);
        });
        ClientServiceImpl service = service(mapper);

        SysClientDTO rename = dto(9L, "mobile", EnableStatus.ENABLED.getCode());
        assertThatThrownBy(() -> service.updateClient(rename))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("编码不能修改");

        SysClientDTO disable = dto(9L, "h5", EnableStatus.DISABLED.getCode());
        assertThatThrownBy(() -> service.updateClient(disable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能禁用");
    }

    @Test
    void shouldAllowDeletingCustomClient() {
        SysClientMapper mapper = proxy(SysClientMapper.class, (methodName, arguments) -> {
            if ("selectById".equals(methodName)) {
                SysClient custom = builtIn("custom-x");
                custom.setClientCode("custom-x");
                return custom;
            }
            if ("deleteById".equals(methodName)) {
                return 1;
            }
            return defaultValue(methodName);
        });
        ClientServiceImpl service = service(mapper);
        assertTrue(service.deleteClient(99L));
    }

    private static ClientServiceImpl service(SysClientMapper mapper) {
        ClientSecretCodec codec = new ClientSecretCodec();
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnableLegacyClientSecretRead(false);
        return new ClientServiceImpl(
                proxy(ICacheService.class, (methodName, arguments) -> defaultValue(methodName)),
                mapper,
                codec,
                new ClientCredentialPolicy(codec),
                authProperties);
    }

    private static SysClient builtIn(String code) {
        SysClient client = new SysClient();
        client.setId(1L);
        client.setClientCode(code);
        client.setClientAuthMethod("none");
        client.setStatus(EnableStatus.ENABLED.getCode());
        return client;
    }

    private static SysClientDTO dto(Long id, String code, Integer status) {
        SysClientDTO dto = new SysClientDTO();
        dto.setId(id);
        dto.setClientCode(code);
        dto.setClientName("name");
        dto.setAppId(code);
        dto.setClientAuthMethod("none");
        dto.setStatus(status);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    private static Object defaultValue(String methodName) {
        if ("toString".equals(methodName)) {
            return "test-proxy";
        }
        if ("delete".equals(methodName)) {
            return true;
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments) throws Throwable;
    }
}
