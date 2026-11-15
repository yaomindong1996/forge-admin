package com.mdframe.forge.plugin.system.auth;

import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.config.config.SecurityConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordPolicySupportTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 11L;

    private final ConfigManagerService configManagerService = mock(ConfigManagerService.class);
    private final SysUserCredentialMapper credentialMapper = mock(SysUserCredentialMapper.class);
    private final PasswordPolicySupport support = new PasswordPolicySupport(configManagerService, credentialMapper);
    private final SecurityConfig securityConfig = new SecurityConfig();

    @BeforeEach
    void useConfiguredPolicy() {
        when(configManagerService.getSecurityConfig()).thenReturn(securityConfig);
    }

    // ==================== 复杂度 ====================

    @Test
    void rejectsBlankPassword() {
        assertRejected(null, "密码不能为空");
        assertRejected("   ", "密码不能为空");
    }

    @Test
    void rejectsPasswordOutsideLengthBounds() {
        assertRejected("Ab1", "密码长度必须在8到72位之间");
        assertRejected("Aa1" + "x".repeat(70), "密码长度必须在8到72位之间");
        assertAccepted("Aa1" + "x".repeat(69));
    }

    @Test
    void rejectsEachMissingCharacterClass() {
        securityConfig.getPasswordPolicy().setRequireSpecialChars(true);

        assertRejected("abcdefg1!", "密码必须包含大写字母");
        assertRejected("ABCDEFG1!", "密码必须包含小写字母");
        assertRejected("Abcdefgh!", "密码必须包含数字");
        assertRejected("Abcdefg12", "密码必须包含特殊字符");
        assertRejected("Abcdefg1 ", "密码必须包含特殊字符");
        assertAccepted("Abcdefg1!");
    }

    @Test
    void skipsRequirementsThatAreSwitchedOff() {
        SecurityConfig.PasswordPolicyConfig policy = securityConfig.getPasswordPolicy();
        policy.setMinLength(6);
        policy.setRequireUppercase(false);
        policy.setRequireLowercase(false);
        policy.setRequireNumbers(false);
        policy.setRequireSpecialChars(false);

        assertAccepted("aaaaaa");
        assertRejected("aaaaa", "密码长度必须在6到72位之间");
    }

    @Test
    void fallsBackToDefaultPolicyWhenConfigIsMissing() {
        when(configManagerService.getSecurityConfig()).thenReturn(null);
        assertRejected("12345678", "密码必须包含大写字母");
        assertAccepted("Abcdefg1");

        SecurityConfig withoutPolicy = new SecurityConfig();
        withoutPolicy.setPasswordPolicy(null);
        when(configManagerService.getSecurityConfig()).thenReturn(withoutPolicy);
        assertRejected("12345678", "密码必须包含大写字母");
    }

    @Test
    void treatsInvalidMinLengthAsDefault() {
        securityConfig.getPasswordPolicy().setMinLength(0);
        assertRejected("Abcde1", "密码长度必须在8到72位之间");

        securityConfig.getPasswordPolicy().setMinLength(null);
        assertRejected("Abcde1", "密码长度必须在8到72位之间");
    }

    // ==================== 历史 ====================

    @Test
    void rejectsCurrentPassword() {
        when(credentialMapper.selectCredential(USER_ID))
                .thenReturn(credential(PasswordUtil.encrypt("Current123")));

        assertThatThrownBy(() -> support.validateNotReused(USER_ID, "Current123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码不能与当前密码相同");
        verify(credentialMapper, never()).selectRecentPasswordHashes(anyLong(), anyLong(), anyInt());
    }

    @Test
    void rejectsPasswordInsideHistoryWindow() {
        securityConfig.getPasswordPolicy().setHistoryCount(3);
        when(credentialMapper.selectCredential(USER_ID))
                .thenReturn(credential(PasswordUtil.encrypt("Current123")));
        when(credentialMapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 3))
                .thenReturn(List.of(PasswordUtil.encrypt("Older1234"), PasswordUtil.encrypt("Previous123")));

        assertThatThrownBy(() -> support.validateNotReused(USER_ID, "Previous123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("新密码不能与最近3次使用过的密码相同");
        assertThatCode(() -> support.validateNotReused(USER_ID, "FreshPass123")).doesNotThrowAnyException();
    }

    @Test
    void skipsHistoryLookupWhenWindowIsDisabled() {
        securityConfig.getPasswordPolicy().setHistoryCount(0);
        when(credentialMapper.selectCredential(USER_ID))
                .thenReturn(credential(PasswordUtil.encrypt("Current123")));

        assertThatCode(() -> support.validateNotReused(USER_ID, "FreshPass123")).doesNotThrowAnyException();
        verify(credentialMapper, never()).selectRecentPasswordHashes(anyLong(), anyLong(), anyInt());
    }

    @Test
    void capsHistoryWindowAtUpperBound() {
        securityConfig.getPasswordPolicy().setHistoryCount(500);
        when(credentialMapper.selectCredential(USER_ID))
                .thenReturn(credential(PasswordUtil.encrypt("Current123")));

        support.validateNotReused(USER_ID, "FreshPass123");

        verify(credentialMapper).selectRecentPasswordHashes(TENANT_ID, USER_ID, PasswordPolicySupport.MAX_HISTORY_COUNT);
    }

    @Test
    void treatsUnrecognisedHashAsNotMatching() {
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential("legacy-not-bcrypt"));
        when(credentialMapper.selectRecentPasswordHashes(TENANT_ID, USER_ID, 5))
                .thenReturn(List.of("", "also-not-bcrypt"));

        assertThatCode(() -> support.validateNotReused(USER_ID, "FreshPass123")).doesNotThrowAnyException();
    }

    @Test
    void skipsReuseCheckWhenUserIsUnknown() {
        assertThatCode(() -> support.validateNotReused(null, "FreshPass123")).doesNotThrowAnyException();
        verifyNoInteractions(credentialMapper);

        when(credentialMapper.selectCredential(USER_ID)).thenReturn(null);
        assertThatCode(() -> support.validateNotReused(USER_ID, "FreshPass123")).doesNotThrowAnyException();
    }

    // ==================== 凭证写入 ====================

    @Test
    void recordsReplacedHashAndPrunesBeyondWindow() {
        securityConfig.getPasswordPolicy().setHistoryCount(2);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential("old-hash"));
        when(credentialMapper.updateCredential(eq(USER_ID), eq("new-hash"), eq(true), any())).thenReturn(1);
        when(credentialMapper.selectHistoryIds(TENANT_ID, USER_ID)).thenReturn(List.of(9L, 8L, 7L, 6L));

        assertThat(support.changeCredential(USER_ID, "new-hash", true)).isTrue();

        ArgumentCaptor<LocalDateTime> updateTime = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> historyTime = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(credentialMapper).updateCredential(eq(USER_ID), eq("new-hash"), eq(true), updateTime.capture());
        verify(credentialMapper).insertPasswordHistory(eq(TENANT_ID), eq(USER_ID), eq("old-hash"), historyTime.capture());
        assertThat(historyTime.getValue()).isEqualTo(updateTime.getValue());
        verify(credentialMapper).deleteHistoryByIds(TENANT_ID, USER_ID, List.of(7L, 6L));
    }

    @Test
    void keepsHistoryThatFitsInsideWindow() {
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential("old-hash"));
        when(credentialMapper.updateCredential(eq(USER_ID), eq("new-hash"), eq(false), any())).thenReturn(1);
        when(credentialMapper.selectHistoryIds(TENANT_ID, USER_ID)).thenReturn(List.of(9L, 8L));

        assertThat(support.changeCredential(USER_ID, "new-hash", false)).isTrue();

        verify(credentialMapper).insertPasswordHistory(eq(TENANT_ID), eq(USER_ID), eq("old-hash"), any());
        verify(credentialMapper, never()).deleteHistoryByIds(anyLong(), anyLong(), anyList());
    }

    @Test
    void purgesHistoryWhenWindowIsDisabled() {
        securityConfig.getPasswordPolicy().setHistoryCount(0);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential("old-hash"));
        when(credentialMapper.updateCredential(eq(USER_ID), eq("new-hash"), eq(false), any())).thenReturn(1);
        when(credentialMapper.selectHistoryIds(TENANT_ID, USER_ID)).thenReturn(List.of(9L, 8L));

        assertThat(support.changeCredential(USER_ID, "new-hash", false)).isTrue();

        verify(credentialMapper, never()).insertPasswordHistory(anyLong(), anyLong(), anyString(), any());
        verify(credentialMapper).deleteHistoryByIds(TENANT_ID, USER_ID, List.of(9L, 8L));
    }

    @Test
    void doesNotTouchHistoryWhenCredentialIsNotUpdated() {
        assertThat(support.changeCredential(null, "new-hash", false)).isFalse();
        assertThat(support.changeCredential(USER_ID, " ", false)).isFalse();
        verifyNoInteractions(credentialMapper);

        when(credentialMapper.selectCredential(USER_ID)).thenReturn(null);
        assertThat(support.changeCredential(USER_ID, "new-hash", false)).isFalse();
        verify(credentialMapper, never()).updateCredential(anyLong(), anyString(), eq(false), any());

        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential("old-hash"));
        when(credentialMapper.updateCredential(eq(USER_ID), eq("new-hash"), eq(false), any())).thenReturn(0);
        assertThat(support.changeCredential(USER_ID, "new-hash", false)).isFalse();
        verify(credentialMapper, never()).insertPasswordHistory(anyLong(), anyLong(), anyString(), any());
        verify(credentialMapper, never()).selectHistoryIds(anyLong(), anyLong());
    }

    @Test
    void updatesCredentialButSkipsHistoryWhenOwnerTenantIsUnknown() {
        UserPasswordCredential credential = credential("old-hash");
        credential.setTenantId(null);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential);
        when(credentialMapper.updateCredential(eq(USER_ID), eq("new-hash"), eq(false), any())).thenReturn(1);

        assertThat(support.changeCredential(USER_ID, "new-hash", false)).isTrue();

        verify(credentialMapper, never()).insertPasswordHistory(any(), anyLong(), anyString(), any());
        verify(credentialMapper, never()).selectHistoryIds(any(), anyLong());
    }

    // ==================== 过期 ====================

    @Test
    void expirationIsOffByDefault() {
        LoginUser loginUser = loginUser();

        support.applyExpiration(loginUser);

        assertThat(new SecurityConfig.PasswordPolicyConfig().getExpireEnabled()).isFalse();
        assertThat(loginUser.getPasswordExpired()).isNull();
        assertThat(loginUser.getForcePasswordChange()).isNull();
        verifyNoInteractions(credentialMapper);
    }

    @Test
    void marksSessionWhenPasswordIsOlderThanConfiguredDays() {
        securityConfig.getPasswordPolicy().setExpireEnabled(true);
        securityConfig.getPasswordPolicy().setExpireDays(30);
        UserPasswordCredential credential = credential("hash");
        credential.setPasswordChangedTime(LocalDateTime.now().minusDays(31));
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential);
        LoginUser loginUser = loginUser();

        support.applyExpiration(loginUser);

        assertThat(loginUser.getPasswordExpired()).isTrue();
        assertThat(loginUser.getForcePasswordChange()).isTrue();
    }

    @Test
    void leavesSessionUntouchedWhenPasswordIsStillValid() {
        securityConfig.getPasswordPolicy().setExpireEnabled(true);
        securityConfig.getPasswordPolicy().setExpireDays(30);
        UserPasswordCredential credential = credential("hash");
        credential.setPasswordChangedTime(LocalDateTime.now().minusDays(29));
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(credential);
        LoginUser loginUser = loginUser();

        support.applyExpiration(loginUser);

        assertThat(loginUser.getPasswordExpired()).isNull();
        assertThat(loginUser.getForcePasswordChange()).isNull();
    }

    @Test
    void ignoresExpirationWhenDaysAreInvalidOrUserIsUnknown() {
        securityConfig.getPasswordPolicy().setExpireEnabled(true);
        securityConfig.getPasswordPolicy().setExpireDays(null);
        LoginUser loginUser = loginUser();
        support.applyExpiration(loginUser);
        support.applyExpiration(null);
        support.applyExpiration(new LoginUser());
        verifyNoInteractions(credentialMapper);

        securityConfig.getPasswordPolicy().setExpireDays(30);
        when(credentialMapper.selectCredential(USER_ID)).thenReturn(null);
        support.applyExpiration(loginUser);
        assertThat(loginUser.getPasswordExpired()).isNull();
    }

    @Test
    void expiresExactlyWhenValidityPeriodHasElapsed() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 12, 0);

        assertThat(PasswordPolicySupport.isExpired(now.minusDays(90), 90, now)).isTrue();
        assertThat(PasswordPolicySupport.isExpired(now.minusDays(90).plusSeconds(1), 90, now)).isFalse();
        assertThat(PasswordPolicySupport.isExpired(now.minusDays(400), 90, now)).isTrue();
        assertThat(PasswordPolicySupport.isExpired(null, 90, now)).isFalse();
        assertThat(PasswordPolicySupport.isExpired(now.minusDays(400), 0, now)).isFalse();
    }

    private void assertRejected(String password, String message) {
        assertThatThrownBy(() -> support.validateComplexity(password))
                .isInstanceOf(BusinessException.class)
                .hasMessage(message);
    }

    private void assertAccepted(String password) {
        assertThatCode(() -> support.validateComplexity(password)).doesNotThrowAnyException();
    }

    private static UserPasswordCredential credential(String passwordHash) {
        UserPasswordCredential credential = new UserPasswordCredential();
        credential.setUserId(USER_ID);
        credential.setTenantId(TENANT_ID);
        credential.setPassword(passwordHash);
        credential.setPasswordChangedTime(LocalDateTime.now());
        return credential;
    }

    private static LoginUser loginUser() {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(USER_ID);
        return loginUser;
    }
}
