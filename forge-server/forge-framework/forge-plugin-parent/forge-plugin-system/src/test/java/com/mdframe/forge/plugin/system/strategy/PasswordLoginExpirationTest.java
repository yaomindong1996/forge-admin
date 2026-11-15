package com.mdframe.forge.plugin.system.strategy;

import com.mdframe.forge.plugin.system.auth.LoginPasswordDecoder;
import com.mdframe.forge.plugin.system.auth.LoginPasswordEncryptionPolicy;
import com.mdframe.forge.plugin.system.auth.PasswordPolicySupport;
import com.mdframe.forge.plugin.system.auth.UserPasswordCredential;
import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.config.config.SecurityConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.context.AuthProperties;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordLoginExpirationTest {

    private static final Long USER_ID = 11L;
    private static final Path STRATEGY_DIR = Path.of("src/main/java/com/mdframe/forge/plugin/system/strategy");

    private final ConfigManagerService configManagerService = mock(ConfigManagerService.class);
    private final SysUserCredentialMapper credentialMapper = mock(SysUserCredentialMapper.class);
    private final IUserLoadService userLoadService = mock(IUserLoadService.class);
    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void passwordLoginMarksSessionWhenPasswordHasExpired() {
        securityConfig.getPasswordPolicy().setExpireEnabled(true);
        securityConfig.getPasswordPolicy().setExpireDays(90);
        passwordChangedDaysAgo(120);
        LoginUser authenticated = authenticatedUser();

        LoginUser result = strategy().doAuthenticate(loginRequest());

        assertThat(result).isSameAs(authenticated);
        assertThat(result.getPasswordExpired()).isTrue();
        assertThat(result.getForcePasswordChange()).isTrue();
    }

    @Test
    void passwordLoginLeavesSessionAloneWhileExpirationIsSwitchedOff() {
        passwordChangedDaysAgo(120);
        authenticatedUser();

        LoginUser result = strategy().doAuthenticate(loginRequest());

        assertThat(result.getPasswordExpired()).isNull();
        assertThat(result.getForcePasswordChange()).isNull();
        verifyNoInteractions(credentialMapper);
    }

    @Test
    void onlyPasswordStrategiesApplyExpiration() throws IOException {
        // 短信、三方登录的用户可能不知道本地密码，过期判定不能扩散到这些策略
        assertThat(source("UsernamePasswordAuthStrategy.java")).contains("applyPasswordExpiration(loginUser);");
        assertThat(source("UsernamePasswordCaptchaAuthStrategy.java")).contains("applyPasswordExpiration(loginUser);");
        assertThat(source("PhoneCaptchaAuthStrategy.java")).doesNotContain("applyPasswordExpiration");
        assertThat(source("SocialAuthStrategyImpl.java")).doesNotContain("applyPasswordExpiration");
    }

    private UsernamePasswordAuthStrategy strategy() {
        when(configManagerService.getSecurityConfig()).thenReturn(securityConfig);
        LoginPasswordEncryptionPolicy encryptionPolicy = mock(LoginPasswordEncryptionPolicy.class);
        when(encryptionPolicy.isEnabled()).thenReturn(false);
        @SuppressWarnings("unchecked")
        LoginPasswordDecoder decoder = new LoginPasswordDecoder(encryptionPolicy, mock(ObjectProvider.class));

        UsernamePasswordAuthStrategy strategy = new UsernamePasswordAuthStrategy();
        ReflectionTestUtils.setField(strategy, "loginPasswordDecoder", decoder);
        ReflectionTestUtils.setField(strategy, "userLoadService", userLoadService);
        ReflectionTestUtils.setField(strategy, "authProperties", new AuthProperties());
        ReflectionTestUtils.setField(strategy, "passwordPolicySupport",
                new PasswordPolicySupport(configManagerService, credentialMapper));
        return strategy;
    }

    private LoginUser authenticatedUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        when(userLoadService.authenticateByUsernamePassword("alice", "Secret123", null)).thenReturn(loginUser);
        return loginUser;
    }

    private void passwordChangedDaysAgo(int days) {
        UserPasswordCredential credential = new UserPasswordCredential();
        credential.setUserId(USER_ID);
        credential.setTenantId(1L);
        credential.setPasswordChangedTime(LocalDateTime.now().minusDays(days));
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential);
    }

    private static LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("Secret123");
        return request;
    }

    private static String source(String fileName) throws IOException {
        return Files.readString(STRATEGY_DIR.resolve(fileName));
    }
}
