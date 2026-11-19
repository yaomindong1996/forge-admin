package com.mdframe.forge.plugin.system.service;

import com.mdframe.forge.plugin.system.entity.SysConfig;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysConfigMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.plugin.system.mapper.TenantBaseDataBootstrapMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantBaseDataBootstrapServiceTest {

    @Test
    void shouldCopyDictAndConfigFromDefaultTenantAndRemapParent() {
        SysDictType sourceType = new SysDictType();
        sourceType.setDictId(1L);
        sourceType.setDictType("sys_user_sex");
        sourceType.setDictName("性别");
        sourceType.setDictStatus(1);

        SysDictData parent = new SysDictData();
        parent.setDictCode(10L);
        parent.setDictType("sys_user_sex");
        parent.setDictLabel("父");
        parent.setDictValue("P");
        parent.setDictStatus(1);

        SysDictData child = new SysDictData();
        child.setDictCode(11L);
        child.setDictType("sys_user_sex");
        child.setDictLabel("子");
        child.setDictValue("C");
        child.setParentDictCode(10L);
        child.setDictStatus(1);

        SysConfig sourceConfig = new SysConfig();
        sourceConfig.setConfigId(5L);
        sourceConfig.setConfigKey("sys.password.minLength");
        sourceConfig.setConfigName("最小长度");
        sourceConfig.setConfigValue("6");
        sourceConfig.setConfigType("Y");

        AtomicLong nextDictCode = new AtomicLong(100);
        AtomicInteger typeInserts = new AtomicInteger();
        AtomicInteger dataInserts = new AtomicInteger();
        AtomicInteger configInserts = new AtomicInteger();
        AtomicReference<Long> remappedParent = new AtomicReference<>();
        AtomicReference<Object> published = new AtomicReference<>();
        List<SysDictData> insertedData = new ArrayList<>();

        TenantBaseDataBootstrapMapper bootstrapMapper = proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> {
            if ("countActiveDictTypes".equals(method)) {
                Long tenantId = (Long) args[0];
                return tenantId.equals(1L) ? 1L : 0L;
            }
            if ("countActiveConfigs".equals(method)) {
                Long tenantId = (Long) args[0];
                return tenantId.equals(1L) ? 1L : 0L;
            }
            if ("selectActiveDictTypes".equals(method)) {
                return List.of(sourceType);
            }
            if ("selectActiveDictData".equals(method)) {
                return List.of(parent, child);
            }
            if ("selectActiveConfigs".equals(method)) {
                return List.of(sourceConfig);
            }
            return defaultValue(method);
        });

        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysDictType row = (SysDictType) args[0];
                assertThat(row.getTenantId()).isEqualTo(9L);
                assertThat(row.getDictId()).isNull();
                typeInserts.incrementAndGet();
                return 1;
            }
            return defaultValue(method);
        });

        SysDictDataMapper dataMapper = proxy(SysDictDataMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysDictData row = (SysDictData) args[0];
                assertThat(row.getTenantId()).isEqualTo(9L);
                assertThat(row.getParentDictCode()).isNull();
                row.setDictCode(nextDictCode.getAndIncrement());
                insertedData.add(row);
                dataInserts.incrementAndGet();
                return 1;
            }
            if ("updateById".equals(method)) {
                SysDictData patch = (SysDictData) args[0];
                remappedParent.set(patch.getParentDictCode());
                return 1;
            }
            return defaultValue(method);
        });

        SysConfigMapper configMapper = proxy(SysConfigMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                SysConfig row = (SysConfig) args[0];
                assertThat(row.getTenantId()).isEqualTo(9L);
                assertThat(row.getConfigKey()).isEqualTo("sys.password.minLength");
                configInserts.incrementAndGet();
                return 1;
            }
            return defaultValue(method);
        });

        ApplicationEventPublisher publisher = published::set;
        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                bootstrapMapper, typeMapper, dataMapper, configMapper, publisher);

        TenantBaseDataBootstrapService.BootstrapResult result = service.ensureFromDefaultTenant(9L);

        assertThat(result.dictTypesCopied()).isEqualTo(1);
        assertThat(result.dictDataCopied()).isEqualTo(2);
        assertThat(result.configsCopied()).isEqualTo(1);
        assertThat(result.dictSkipped()).isFalse();
        assertThat(result.configSkipped()).isFalse();
        assertThat(typeInserts.get()).isEqualTo(1);
        assertThat(dataInserts.get()).isEqualTo(2);
        assertThat(configInserts.get()).isEqualTo(1);
        assertThat(remappedParent.get()).isEqualTo(100L);
        assertThat(published.get()).isInstanceOf(DictChangeEvent.class);
        assertThat(((DictChangeEvent) published.get()).getDictTypes()).contains("sys_user_sex");
        assertThat(insertedData).hasSize(2);
    }

    @Test
    void shouldSkipWhenTargetAlreadyHasData() {
        TenantBaseDataBootstrapMapper bootstrapMapper = proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> {
            if ("countActiveDictTypes".equals(method) || "countActiveConfigs".equals(method)) {
                return 3L;
            }
            return defaultValue(method);
        });
        AtomicInteger inserts = new AtomicInteger();
        SysDictTypeMapper typeMapper = proxy(SysDictTypeMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                inserts.incrementAndGet();
            }
            return defaultValue(method);
        });
        SysDictDataMapper dataMapper = proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method));
        SysConfigMapper configMapper = proxy(SysConfigMapper.class, (method, args) -> {
            if ("insert".equals(method)) {
                inserts.incrementAndGet();
            }
            return defaultValue(method);
        });

        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                bootstrapMapper, typeMapper, dataMapper, configMapper, event -> {
                });

        TenantBaseDataBootstrapService.BootstrapResult result = service.ensureFromDefaultTenant(9L);
        assertThat(result.dictSkipped()).isTrue();
        assertThat(result.configSkipped()).isTrue();
        assertThat(result.dictTypesCopied()).isZero();
        assertThat(result.configsCopied()).isZero();
        assertThat(inserts.get()).isZero();
    }

    @Test
    void shouldNoopForDefaultTenant() {
        TenantBaseDataBootstrapService service = new TenantBaseDataBootstrapService(
                proxy(TenantBaseDataBootstrapMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysDictTypeMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysDictDataMapper.class, (method, args) -> defaultValue(method)),
                proxy(SysConfigMapper.class, (method, args) -> defaultValue(method)),
                event -> {
                });
        TenantBaseDataBootstrapService.BootstrapResult result = service.ensureFromDefaultTenant(1L);
        assertThat(result.dictSkipped()).isTrue();
        assertThat(result.configSkipped()).isTrue();
    }

    private interface Invoker {
        Object invoke(String method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invoker invoker) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> type.getSimpleName() + "Proxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return invoker.invoke(method.getName(), args == null ? new Object[0] : args);
                });
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "countActiveDictTypes", "countActiveConfigs" -> 0L;
            case "selectActiveDictTypes", "selectActiveDictData", "selectActiveConfigs" -> List.of();
            case "insert", "updateById", "deleteById" -> 0;
            default -> null;
        };
    }
}
