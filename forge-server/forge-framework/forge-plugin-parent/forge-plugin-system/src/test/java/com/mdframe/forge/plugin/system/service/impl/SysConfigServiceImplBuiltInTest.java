package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysConfigDTO;
import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysConfigServiceImplBuiltInTest {

    @Test
    void shouldRejectDeletingBuiltInConfig() {
        SysConfig builtIn = existing("Y", "sys.export.max");
        SysConfigServiceImpl service = service(builtIn);
        assertThatThrownBy(() -> service.deleteConfigById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置参数不能删除");
    }

    @Test
    void shouldRejectChangingBuiltInKey() {
        SysConfig builtIn = existing("Y", "sys.export.max");
        SysConfigServiceImpl service = service(builtIn);
        SysConfigDTO dto = new SysConfigDTO();
        dto.setConfigId(1L);
        dto.setConfigKey("sys.export.other");
        dto.setConfigType("Y");
        dto.setConfigValue("100");
        assertThatThrownBy(() -> service.updateConfig(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("键名不能修改");
    }

    @Test
    void shouldAllowDeletingCustomConfig() {
        SysConfig custom = existing("N", "biz.custom");
        SysConfigMapper mapper = proxy(SysConfigMapper.class, (method, args) -> {
            if ("selectById".equals(method)) {
                return custom;
            }
            if ("deleteById".equals(method)) {
                return 1;
            }
            return null;
        });
        assertTrue(new SysConfigServiceImpl(mapper).deleteConfigById(2L));
    }

    private static SysConfigServiceImpl service(SysConfig existing) {
        return new SysConfigServiceImpl(proxy(SysConfigMapper.class, (method, args) -> {
            if ("selectById".equals(method)) {
                return existing;
            }
            return null;
        }));
    }

    private static SysConfig existing(String type, String key) {
        SysConfig config = new SysConfig();
        config.setConfigId(1L);
        config.setConfigType(type);
        config.setConfigKey(key);
        config.setConfigValue("1");
        return config;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
