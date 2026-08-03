package com.mdframe.forge.task;

import com.mdframe.forge.plugin.system.mapper.SysOnlineUserMapper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineUserCleanTaskTest {

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDownTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void cleanupShouldExplicitlyIgnoreTenantOnlyDuringSystemTaskExecution() {
        AtomicReference<Boolean> cleanupIgnore = new AtomicReference<>();
        SysOnlineUserMapper mapper = proxy((methodName, arguments) -> {
            if ("cleanExpiredUsers".equals(methodName)) {
                cleanupIgnore.set(TenantContextHolder.isIgnore());
                return 2;
            }
            return null;
        });
        OnlineUserCleanTask task = new OnlineUserCleanTask(mapper);

        TenantContextHolder.setTenantId(17L);
        TenantContextHolder.setIgnore(false);
        task.cleanExpiredUsers();

        assertThat(cleanupIgnore.get()).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo(17L);
        assertThat(TenantContextHolder.isIgnore()).isFalse();
    }

    private SysOnlineUserMapper proxy(Invocation invocation) {
        return (SysOnlineUserMapper) Proxy.newProxyInstance(
                SysOnlineUserMapper.class.getClassLoader(),
                new Class<?>[]{SysOnlineUserMapper.class},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
