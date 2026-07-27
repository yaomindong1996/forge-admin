package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysOnlineUser;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysOnlineUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.websocket.service.IMessagePushService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysOnlineUserServiceSecurityTest {

    @Test
    void managementOperationsShouldFailClosedWhenTenantScopedRecordsAreMissing() {
        SysOnlineUserMapper onlineUserMapper = proxy(SysOnlineUserMapper.class,
                (methodName, arguments) -> defaultValue(methodName));
        SysUserMapper userMapper = proxy(SysUserMapper.class,
                (methodName, arguments) -> defaultValue(methodName));
        SysOnlineUserServiceImpl service = new SysOnlineUserServiceImpl(
                onlineUserMapper, noOpMessagePushService(), userMapper);

        assertThatThrownBy(() -> service.kickoutSession(41L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("在线会话不存在或无权操作");
        assertThatThrownBy(() -> service.banUser(51L, 60L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在或无权操作");
        assertThatThrownBy(() -> service.unbanUser(51L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在或无权操作");
    }

    @Test
    void userSessionLookupShouldReturnOnlyMapperScopedIdentifiers() {
        AtomicInteger userLookupCount = new AtomicInteger();
        AtomicInteger sessionLookupCount = new AtomicInteger();
        SysOnlineUserMapper onlineUserMapper = proxy(SysOnlineUserMapper.class, (methodName, arguments) -> {
            if ("selectActiveSessionIdsByUserId".equals(methodName)) {
                sessionLookupCount.incrementAndGet();
                return List.of(101L, 102L);
            }
            return defaultValue(methodName);
        });
        SysUserMapper userMapper = proxy(SysUserMapper.class, (methodName, arguments) -> {
            if ("selectById".equals(methodName)) {
                userLookupCount.incrementAndGet();
                return new SysUser();
            }
            return defaultValue(methodName);
        });
        SysOnlineUserServiceImpl service = new SysOnlineUserServiceImpl(
                onlineUserMapper, noOpMessagePushService(), userMapper);

        assertThat(service.getUserSessionIds(52L)).containsExactly(101L, 102L);
        assertThat(userLookupCount.get()).isEqualTo(1);
        assertThat(sessionLookupCount.get()).isEqualTo(1);
    }

    @Test
    void batchKickoutShouldValidateEveryTenantScopedSessionBeforeSideEffects() {
        AtomicInteger activeTokenLookupCount = new AtomicInteger();
        AtomicInteger sessionLookupCount = new AtomicInteger();
        SysOnlineUser firstSession = new SysOnlineUser();
        firstSession.setId(101L);
        firstSession.setStatus(1);
        firstSession.setTokenValue("opaque-session-value");
        SysOnlineUserMapper onlineUserMapper = proxy(SysOnlineUserMapper.class, (methodName, arguments) -> {
            if ("selectById".equals(methodName)) {
                sessionLookupCount.incrementAndGet();
                return Long.valueOf(101L).equals(arguments[0]) ? firstSession : null;
            }
            if ("selectActiveByTokenValue".equals(methodName)) {
                activeTokenLookupCount.incrementAndGet();
            }
            return defaultValue(methodName);
        });

        SysOnlineUserServiceImpl service = new SysOnlineUserServiceImpl(
                onlineUserMapper, noOpMessagePushService(),
                proxy(SysUserMapper.class, (methodName, arguments) -> defaultValue(methodName)));

        assertThatThrownBy(() -> service.batchKickoutSessions(List.of(101L, 202L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("在线会话不存在或无权操作");
        assertThat(sessionLookupCount.get()).isEqualTo(2);
        assertThat(activeTokenLookupCount.get()).isZero();
    }

    private IMessagePushService noOpMessagePushService() {
        return proxy(IMessagePushService.class, (methodName, arguments) -> defaultValue(methodName));
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> invocation.invoke(method.getName(), args));
    }

    private Object defaultValue(String methodName) {
        if ("toString".equals(methodName)) {
            return "test-proxy";
        }
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String methodName, Object[] arguments);
    }
}
