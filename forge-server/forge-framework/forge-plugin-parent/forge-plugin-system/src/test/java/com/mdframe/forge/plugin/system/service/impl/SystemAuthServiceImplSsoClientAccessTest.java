package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.domain.SsoExchangeRequest;
import com.mdframe.forge.starter.auth.domain.SsoTicketPayload;
import com.mdframe.forge.starter.auth.domain.SsoTicketRequest;
import com.mdframe.forge.starter.auth.domain.SsoTicketResult;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAuthServiceImplSsoClientAccessTest {

    @Test
    void createSsoTicketRejectsClientOutsideAllowedList() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ICacheService cacheService = mock(ICacheService.class);
        when(userMapper.selectById(7L)).thenReturn(user(7L, "pc"));
        SystemAuthServiceImpl service = service(userMapper, cacheService, client("forge_report"), null);

        LoginUser current = loginUser(7L);
        SsoTicketRequest request = new SsoTicketRequest();
        request.setTargetClient("forge_report");
        request.setRedirectPath("/screen");

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(current);
            assertThatThrownBy(() -> service.createSsoTicket(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("该账号未授权此客户端登录权限");
        }

        verify(cacheService, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void createSsoTicketAllowsUnrestrictedUser() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ICacheService cacheService = mock(ICacheService.class);
        when(userMapper.selectById(7L)).thenReturn(user(7L, null));
        SystemAuthServiceImpl service = service(userMapper, cacheService, client("forge_report"), null);

        LoginUser current = loginUser(7L);
        SsoTicketRequest request = new SsoTicketRequest();
        request.setTargetClient("forge_report");

        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class);
             MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            session.when(SessionHelper::getLoginUser).thenReturn(current);
            stp.when(StpUtil::getTokenValue).thenReturn("source-token");

            SsoTicketResult result = service.createSsoTicket(request);

            assertThat(result.getTargetClient()).isEqualTo("forge_report");
            assertThat(result.getTicket()).isNotBlank();
        }

        verify(cacheService).set(anyString(), any(SsoTicketPayload.class), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void exchangeSsoTicketRejectsClientOutsideAllowedList() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ICacheService cacheService = mock(ICacheService.class);
        IUserLoadService userLoadService = mock(IUserLoadService.class);
        SysUser stored = user(7L, "pc");
        when(userMapper.selectById(7L)).thenReturn(stored);
        when(userLoadService.loadUserByUsername("alice", 1L)).thenReturn(loginUser(7L));

        SsoTicketPayload payload = new SsoTicketPayload();
        payload.setTicket("ticket-1");
        payload.setSourceToken("source-token");
        payload.setTargetClient("forge_report");
        payload.setUserId(7L);
        payload.setUsername("alice");
        payload.setTenantId(1L);
        when(cacheService.get("auth:sso:ticket:ticket-1", SsoTicketPayload.class)).thenReturn(payload);
        when(cacheService.delete("auth:sso:ticket:ticket-1")).thenReturn(true);

        SaSession sourceSession = mock(SaSession.class);
        when(sourceSession.get("loginUser")).thenReturn(loginUser(7L));

        SystemAuthServiceImpl service = service(userMapper, cacheService, client("forge_report"), userLoadService);
        SsoExchangeRequest request = new SsoExchangeRequest();
        request.setTicket("ticket-1");

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(() -> StpUtil.getTokenSessionByToken("source-token")).thenReturn(sourceSession);
            assertThatThrownBy(() -> service.exchangeSsoTicket(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("该账号未授权此客户端登录权限");
            stp.verify(() -> StpUtil.login(any(), any(SaLoginParameter.class)), never());
        }
    }

    private SystemAuthServiceImpl service(SysUserMapper userMapper, ICacheService cacheService,
                                          SysClient client, IUserLoadService userLoadService) {
        AuthProperties authProperties = new AuthProperties();
        return new SystemAuthServiceImpl(
                userMapper,
                null,
                null,
                userLoadService,
                null,
                authProperties,
                null,
                clientService(client),
                cacheService,
                null,
                null,
                null,
                null,
                null);
    }

    private IClientService clientService(SysClient client) {
        IClientService clientService = mock(IClientService.class);
        when(clientService.getByCode(anyString())).thenReturn(client);
        when(clientService.requiresAppSecret(client)).thenReturn(false);
        return clientService;
    }

    private SysClient client(String clientCode) {
        SysClient client = new SysClient();
        client.setClientCode(clientCode);
        client.setAppId(clientCode);
        client.setClientAuthMethod("none");
        client.setStatus(1);
        client.setTokenTimeout(7200L);
        client.setTokenActivityTimeout(-1L);
        client.setConcurrentLogin(false);
        client.setShareToken(false);
        return client;
    }

    private SysUser user(Long id, String allowedClients) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("alice");
        user.setUserStatus(1);
        user.setAllowedClients(allowedClients);
        user.setTenantId(1L);
        return user;
    }

    private LoginUser loginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername("alice");
        loginUser.setTenantId(1L);
        loginUser.setUserClient("pc");
        return loginUser;
    }
}
