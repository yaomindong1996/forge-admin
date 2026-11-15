package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.auth.LoginPasswordDecoder;
import com.mdframe.forge.plugin.system.auth.LoginPasswordEncryptionPolicy;
import com.mdframe.forge.plugin.system.auth.PasswordPolicySupport;
import com.mdframe.forge.plugin.system.auth.RecoveryChannelSupport;
import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.auth.domain.RegisterRequest;
import com.mdframe.forge.starter.auth.domain.ResetPasswordRequest;
import com.mdframe.forge.starter.auth.domain.SendResetPasswordCodeRequest;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.config.config.LoginConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAuthServiceImplPasswordRecoveryTest {

    private static final Long USER_ID = 11L;
    private static final Long TENANT_ID = 1L;

    private final SysUserCredentialMapper credentialMapper = mock(SysUserCredentialMapper.class);

    @Test
    void shouldRejectSendCodeWhenNoChannelEnabled() {
        SystemAuthServiceImpl service = service(
                new RecoveryChannelSupport(Optional.empty(), Optional.empty()),
                mock(ICacheService.class), mock(ICaptchaService.class),
                mock(SysUserMapper.class), mock(ConfigManagerService.class));

        SendResetPasswordCodeRequest request = new SendResetPasswordCodeRequest();
        request.setChannel("sms");
        request.setAccount("13800138000");

        assertThatThrownBy(() -> service.sendResetPasswordCode(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未启用找回密码通道");
    }

    @Test
    void shouldNotRevealMissingAccountWhenSendingCode() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        ICacheService cacheService = mock(ICacheService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectByPhoneForLogin("13800138000", null)).thenReturn(null);

        SystemAuthServiceImpl service = service(
                smsSupport(), cacheService, captchaService, userMapper, mock(ConfigManagerService.class));

        SendResetPasswordCodeRequest request = new SendResetPasswordCodeRequest();
        request.setChannel("sms");
        request.setAccount("13800138000");
        service.sendResetPasswordCode(request);

        verify(captchaService, never()).sendSmsCaptcha(anyString());
    }

    @Test
    void shouldSendSmsCodeOnlyWhenChannelEnabledAndUserExists() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        ICacheService cacheService = mock(ICacheService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUser user = new SysUser();
        user.setId(11L);
        when(userMapper.selectByPhoneForLogin("13800138000", 1L)).thenReturn(user);

        SystemAuthServiceImpl service = service(
                smsSupport(), cacheService, captchaService, userMapper, mock(ConfigManagerService.class));

        SendResetPasswordCodeRequest request = new SendResetPasswordCodeRequest();
        request.setChannel("sms");
        request.setAccount("13800138000");
        request.setTenantId(1L);
        service.sendResetPasswordCode(request);

        verify(captchaService).sendSmsCaptcha("13800138000");
    }

    @Test
    void shouldRejectGraphicCaptchaStyleReset() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SystemAuthServiceImpl service = service(
                smsSupport(), mock(ICacheService.class), captchaService,
                mock(SysUserMapper.class), mock(ConfigManagerService.class));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setChannel("sms");
        request.setAccount("13800138000");
        request.setCode("123456");
        request.setNewPassword("NewPass123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");
        verify(captchaService, never()).validateAndDelete(any(), any());
        verify(captchaService).validateAndDeleteSmsCaptcha("13800138000", "123456");
    }

    @Test
    void shouldRejectRegisterWhenDisabled() {
        ConfigManagerService configManagerService = mock(ConfigManagerService.class);
        when(configManagerService.getLoginConfig()).thenReturn(new LoginConfig());
        SystemAuthServiceImpl service = service(
                new RecoveryChannelSupport(Optional.empty(), Optional.empty()),
                mock(ICacheService.class), mock(ICaptchaService.class),
                mock(SysUserMapper.class), configManagerService);

        assertThatThrownBy(() -> service.register(new RegisterRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("系统未开放注册");
        assertThat(new LoginConfig().getEnableRegister()).isFalse();
    }

    @Test
    void shouldRejectWeakPasswordBeforeConsumingRecoveryCode() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SystemAuthServiceImpl service = service(
                smsSupport(), mock(ICacheService.class), captchaService,
                mock(SysUserMapper.class), mock(ConfigManagerService.class));

        assertThatThrownBy(() -> service.resetPassword(resetRequest("123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码长度必须在8到72位之间");
        verify(captchaService, never()).validateAndDeleteSmsCaptcha(anyString(), anyString());
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), anyBoolean(), any());
    }

    @Test
    void shouldNotCompareHistoryUntilRecoveryCodeIsVerified() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectByPhoneForLogin("13800138000", null)).thenReturn(recoveryUser());
        when(captchaService.validateAndDeleteSmsCaptcha("13800138000", "123456")).thenReturn(false);
        SystemAuthServiceImpl service = service(
                smsSupport(), mock(ICacheService.class), captchaService,
                userMapper, mock(ConfigManagerService.class));

        assertThatThrownBy(() -> service.resetPassword(resetRequest("Current123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误或已过期");
        verify(credentialMapper, never()).selectCredential(anyLong());
    }

    @Test
    void shouldRejectReusedPasswordAfterRecoveryCodeIsVerified() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        when(userMapper.selectByPhoneForLogin("13800138000", null)).thenReturn(recoveryUser());
        when(captchaService.validateAndDeleteSmsCaptcha("13800138000", "123456")).thenReturn(true);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential(PasswordUtil.encrypt("Current123")));
        when(credentialMapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5))
                .thenReturn(List.of(PasswordUtil.encrypt("Previous123")));
        SystemAuthServiceImpl service = service(
                smsSupport(), mock(ICacheService.class), captchaService,
                userMapper, mock(ConfigManagerService.class));

        assertThatThrownBy(() -> service.resetPassword(resetRequest("Previous123")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码不能与最近5次使用过的密码相同");
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), anyBoolean(), any());
    }

    @Test
    void shouldResetPasswordThroughCredentialPolicy() {
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        String currentHash = PasswordUtil.encrypt("Current123");
        when(userMapper.selectByPhoneForLogin("13800138000", null)).thenReturn(recoveryUser());
        when(captchaService.validateAndDeleteSmsCaptcha("13800138000", "123456")).thenReturn(true);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential(currentHash));
        when(credentialMapper.updateCredential(eq(USER_ID), anyString(), eq(false), any())).thenReturn(1);
        SystemAuthServiceImpl service = service(
                smsSupport(), mock(ICacheService.class), captchaService,
                userMapper, mock(ConfigManagerService.class));

        assertThat(service.resetPassword(resetRequest("FreshPass123"))).isTrue();

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        verify(credentialMapper).updateCredential(eq(USER_ID), storedHash.capture(), eq(false), any());
        assertThat(PasswordUtil.matches("FreshPass123", storedHash.getValue())).isTrue();
        verify(credentialMapper).insertPasswordHistory(eq(TENANT_ID), eq(USER_ID), eq(currentHash), any());
    }

    @Test
    void shouldRejectWeakPasswordOnRegisterBeforeConsumingCaptcha() {
        ConfigManagerService configManagerService = mock(ConfigManagerService.class);
        LoginConfig loginConfig = new LoginConfig();
        loginConfig.setEnableRegister(true);
        when(configManagerService.getLoginConfig()).thenReturn(loginConfig);
        ICaptchaService captchaService = mock(ICaptchaService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SystemAuthServiceImpl service = service(
                new RecoveryChannelSupport(Optional.empty(), Optional.empty()),
                mock(ICacheService.class), captchaService, userMapper, configManagerService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("123456");
        request.setCode("abcd");
        request.setCodeKey("captcha-key");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("密码长度必须在8到72位之间");
        verify(captchaService, never()).validateAndDelete(any(), any());
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    private ResetPasswordRequest resetRequest(String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setChannel("sms");
        request.setAccount("13800138000");
        request.setCode("123456");
        request.setNewPassword(newPassword);
        return request;
    }

    private SysUser recoveryUser() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        return user;
    }

    private UserPasswordCredential credential(String passwordHash) {
        UserPasswordCredential credential = new UserPasswordCredential();
        credential.setUserId(USER_ID);
        credential.setTenantId(TENANT_ID);
        credential.setPassword(passwordHash);
        return credential;
    }

    private RecoveryChannelSupport smsSupport() {
        SmsConfigProvider.SmsConfig config = new SmsConfigProvider.SmsConfig();
        config.setStatus(1);
        config.setAccessKeyId("ak");
        config.setTemplateId("tpl");
        return new RecoveryChannelSupport(Optional.of(() -> config), Optional.empty());
    }

    private SystemAuthServiceImpl service(RecoveryChannelSupport recoveryChannelSupport,
                                          ICacheService cacheService,
                                          ICaptchaService captchaService,
                                          SysUserMapper userMapper,
                                          ConfigManagerService configManagerService) {
        // 关闭登录密码 RSA 加密，找回密码请求里的新密码即明文；
        // 密码策略读不到安全配置时使用默认策略（8 位 + 大小写 + 数字，历史 5 个）。
        LoginPasswordEncryptionPolicy encryptionPolicy = mock(LoginPasswordEncryptionPolicy.class);
        when(encryptionPolicy.isEnabled()).thenReturn(false);
        @SuppressWarnings("unchecked")
        LoginPasswordDecoder decoder = new LoginPasswordDecoder(encryptionPolicy, mock(ObjectProvider.class));
        return new SystemAuthServiceImpl(
                userMapper, captchaService, null, null, null, new AuthProperties(),
                configManagerService, null, cacheService, null, null, null,
                recoveryChannelSupport, decoder,
                new PasswordPolicySupport(configManagerService, credentialMapper));
    }
}
