package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.dto.SysDictTypeDTO;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysDictTypeServiceImplProtectionTest {

    @Test
    void shouldRejectDuplicateDictTypeOnInsert() {
        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("selectCount".equals(method)) {
                return 1L;
            }
            return defaultValue(method);
        });
        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(
                typeMapper,
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });

        SysDictTypeDTO dto = new SysDictTypeDTO();
        dto.setDictType("biz_status");
        dto.setDictName("状态");
        assertThatThrownBy(() -> service.insertDictType(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("字典类型已存在");
    }

    @Test
    void shouldRejectRenamingDictTypeCode() {
        SysDictType existing = new SysDictType();
        existing.setDictId(8L);
        existing.setDictType("biz_status");
        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("selectById".equals(method)) {
                return existing;
            }
            return defaultValue(method);
        });
        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(
                typeMapper,
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });

        SysDictTypeDTO dto = new SysDictTypeDTO();
        dto.setDictId(8L);
        dto.setDictType("biz_status_v2");
        dto.setDictName("状态");
        assertThatThrownBy(() -> service.updateDictType(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能修改");
    }

    @Test
    void shouldCascadeDeleteDataAndPublishEventForCustomType() {
        SysDictType existing = new SysDictType();
        existing.setDictId(9L);
        existing.setDictType("biz_custom");
        AtomicBoolean dataDeleted = new AtomicBoolean();
        AtomicReference<Object> published = new AtomicReference<>();
        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("selectById".equals(method)) {
                return existing;
            }
            if ("deleteById".equals(method)) {
                return 1;
            }
            return defaultValue(method);
        });
        SysDictDataMapper dataMapper = proxy(SysDictDataMapper.class, (method, args) -> {
            if ("delete".equals(method)) {
                dataDeleted.set(true);
                return 2;
            }
            return defaultValue(method);
        });
        ApplicationEventPublisher publisher = published::set;
        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(typeMapper, dataMapper, publisher);

        assertThat(service.deleteDictTypeById(9L)).isTrue();
        assertThat(dataDeleted.get()).isTrue();
        assertThat(published.get()).isInstanceOf(DictChangeEvent.class);
        assertThat(((DictChangeEvent) published.get()).getDictTypes()).containsExactly("biz_custom");
    }

    @Test
    void shouldRejectDeletingSystemDictType() {
        SysDictType existing = new SysDictType();
        existing.setDictId(1L);
        existing.setDictType("sys_user_sex");
        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(
                proxy(SysDictTypeMapper.class, (method, args) ->
                        "selectById".equals(method) ? existing : defaultValue(method)),
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });

        assertThatThrownBy(() -> service.deleteDictTypeById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置字典不能删除");
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
            return "proxy";
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
