package com.mdframe.forge.plugin.system.auth;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.mapper.SysUserCredentialMapper;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.config.config.SecurityConfig;
import com.mdframe.forge.starter.config.config.SecurityConfig.PasswordPolicyConfig;
import com.mdframe.forge.starter.config.service.ConfigManagerService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * 密码策略：让配置中心「安全配置 → 密码策略」在后端生效。
 * <ul>
 *   <li>复杂度：所有由人设定密码的入口都要调用 {@link #validateComplexity(String)}</li>
 *   <li>历史：用户自助改密、找回密码调用 {@link #validateNotReused(Long, String)}</li>
 *   <li>写入：改密统一走 {@link #changeCredential(Long, String, boolean)}，保证历史与凭证列同事务更新</li>
 *   <li>过期：用户名密码登录成功后调用 {@link #applyExpiration(LoginUser)}，默认关闭</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordPolicySupport {

    /** BCrypt 只使用前 72 字节，超出部分不参与校验，因此不允许设置更长的密码。 */
    static final int MAX_LENGTH = 72;
    static final int DEFAULT_MIN_LENGTH = 8;
    /** 与配置页「密码历史记录」上限一致，同时限制单次改密的哈希比对次数。 */
    static final int MAX_HISTORY_COUNT = 20;
    /** sys_user_password_history.password_hash 列宽。 */
    private static final int HISTORY_HASH_MAX_LENGTH = 100;

    private final ConfigManagerService configManagerService;
    private final SysUserCredentialMapper credentialMapper;

    /**
     * 校验明文密码是否满足长度与字符类别要求。
     */
    public void validateComplexity(String rawPassword) {
        if (StrUtil.isBlank(rawPassword)) {
            throw new BusinessException("密码不能为空");
        }
        PasswordPolicyConfig policy = resolvePolicy();
        int minLength = resolveMinLength(policy);
        if (rawPassword.length() < minLength || rawPassword.length() > MAX_LENGTH) {
            throw new BusinessException("密码长度必须在" + minLength + "到" + MAX_LENGTH + "位之间");
        }
        requireCharacter(policy.getRequireUppercase(), rawPassword, Character::isUpperCase, "密码必须包含大写字母");
        requireCharacter(policy.getRequireLowercase(), rawPassword, Character::isLowerCase, "密码必须包含小写字母");
        requireCharacter(policy.getRequireNumbers(), rawPassword, Character::isDigit, "密码必须包含数字");
        requireCharacter(policy.getRequireSpecialChars(), rawPassword, PasswordPolicySupport::isSpecialChar,
                "密码必须包含特殊字符");
    }

    /**
     * 校验新密码不得与当前密码、最近 N 个历史密码相同。
     * 只用于用户本人操作（自助改密、已通过验证码的找回密码）：比对结果会暴露密码是否命中，
     * 不能提供给管理员重置等「替别人设密码」的入口。
     */
    public void validateNotReused(Long userId, String rawPassword) {
        if (userId == null) {
            return;
        }
        UserPasswordCredential current = TenantContextHolder.executeIgnore(
                () -> credentialMapper.selectCredential(userId));
        if (current == null) {
            return;
        }
        if (matches(rawPassword, current.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        int historyCount = resolveHistoryCount();
        Long tenantId = current.getTenantId();
        if (historyCount == 0 || tenantId == null) {
            return;
        }
        List<String> hashes = TenantContextHolder.executeIgnore(
                () -> credentialMapper.selectRecentPasswordHashes(tenantId, userId, historyCount));
        if (hashes != null && hashes.stream().anyMatch(hash -> matches(rawPassword, hash))) {
            throw new BusinessException("新密码不能与最近" + historyCount + "次使用过的密码相同");
        }
    }

    /**
     * 写入新密码：更新凭证列（密码、版本、变更时间、强制改密标记），并把被替换的旧哈希记入历史。
     *
     * @return 用户不存在或更新未命中时返回 false
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeCredential(Long userId, String encodedPassword, boolean forcePasswordChange) {
        if (userId == null || StrUtil.isBlank(encodedPassword)) {
            return false;
        }
        return TenantContextHolder.executeIgnore(
                () -> doChangeCredential(userId, encodedPassword, forcePasswordChange));
    }

    /**
     * 密码过期判定。只在用户名密码登录成功后调用：过期时给本次会话打标记，
     * 后续由强制改密拦截限制为只能改密。未启用过期时不做任何事。
     */
    public void applyExpiration(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            return;
        }
        int expireDays = resolveExpireDays();
        if (expireDays <= 0) {
            return;
        }
        UserPasswordCredential credential = TenantContextHolder.executeIgnore(
                () -> credentialMapper.selectCredential(loginUser.getUserId()));
        if (credential == null) {
            return;
        }
        if (isExpired(credential.getPasswordChangedTime(), expireDays, LocalDateTime.now())) {
            loginUser.setPasswordExpired(true);
            loginUser.setForcePasswordChange(true);
            log.info("密码已过期，要求改密: userId={}, expireDays={}", loginUser.getUserId(), expireDays);
        }
    }

    /**
     * 密码变更时间距今达到或超过有效期即过期；没有变更时间时无从判断，按未过期处理。
     */
    static boolean isExpired(LocalDateTime passwordChangedTime, int expireDays, LocalDateTime now) {
        if (passwordChangedTime == null || expireDays <= 0) {
            return false;
        }
        return !passwordChangedTime.plusDays(expireDays).isAfter(now);
    }

    private boolean doChangeCredential(Long userId, String encodedPassword, boolean forcePasswordChange) {
        UserPasswordCredential previous = credentialMapper.selectCredential(userId);
        if (previous == null) {
            return false;
        }
        LocalDateTime changedTime = LocalDateTime.now();
        if (credentialMapper.updateCredential(userId, encodedPassword, forcePasswordChange, changedTime) <= 0) {
            return false;
        }
        recordHistory(userId, previous, changedTime);
        return true;
    }

    private void recordHistory(Long userId, UserPasswordCredential previous, LocalDateTime changedTime) {
        Long tenantId = previous.getTenantId();
        if (tenantId == null) {
            return;
        }
        int historyCount = resolveHistoryCount();
        if (historyCount > 0 && isStorableHash(previous.getPassword())) {
            credentialMapper.insertPasswordHistory(tenantId, userId, previous.getPassword(), changedTime);
        }
        // 超出保留窗口的哈希物理删除，尽量少留凭证材料；历史校验关闭时窗口为 0，即全部清除。
        List<Long> ids = credentialMapper.selectHistoryIds(tenantId, userId);
        if (ids != null && ids.size() > historyCount) {
            credentialMapper.deleteHistoryByIds(tenantId, userId,
                    new ArrayList<>(ids.subList(historyCount, ids.size())));
        }
    }

    private static boolean isStorableHash(String hash) {
        return StrUtil.isNotBlank(hash) && hash.length() <= HISTORY_HASH_MAX_LENGTH;
    }

    private static void requireCharacter(Boolean required, String password, IntPredicate matcher, String message) {
        if (Boolean.TRUE.equals(required) && password.chars().noneMatch(matcher)) {
            throw new BusinessException(message);
        }
    }

    private static boolean isSpecialChar(int ch) {
        return !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch);
    }

    private boolean matches(String rawPassword, String encodedPassword) {
        if (StrUtil.isBlank(encodedPassword)) {
            return false;
        }
        try {
            return PasswordUtil.matches(rawPassword, encodedPassword);
        } catch (RuntimeException e) {
            // 历史数据里可能存在非 BCrypt 格式的哈希，无法比对时按不匹配处理，不阻断改密。
            log.warn("密码哈希格式无法识别，跳过比对: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    private PasswordPolicyConfig resolvePolicy() {
        SecurityConfig config = configManagerService.getSecurityConfig();
        PasswordPolicyConfig policy = config == null ? null : config.getPasswordPolicy();
        return policy == null ? new PasswordPolicyConfig() : policy;
    }

    private static int resolveMinLength(PasswordPolicyConfig policy) {
        Integer minLength = policy.getMinLength();
        if (minLength == null || minLength < 1) {
            return DEFAULT_MIN_LENGTH;
        }
        return Math.min(minLength, MAX_LENGTH);
    }

    /** 历史校验窗口：未配置或小于等于 0 表示关闭。 */
    private int resolveHistoryCount() {
        Integer historyCount = resolvePolicy().getHistoryCount();
        if (historyCount == null || historyCount <= 0) {
            return 0;
        }
        return Math.min(historyCount, MAX_HISTORY_COUNT);
    }

    /** 密码有效期天数：未启用过期或天数无效时返回 0。 */
    private int resolveExpireDays() {
        PasswordPolicyConfig policy = resolvePolicy();
        if (!Boolean.TRUE.equals(policy.getExpireEnabled())) {
            return 0;
        }
        Integer expireDays = policy.getExpireDays();
        return expireDays == null || expireDays <= 0 ? 0 : expireDays;
    }
}
