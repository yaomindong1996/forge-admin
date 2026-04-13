package com.mdframe.forge.plugin.system.strategy;

import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.auth.enums.AuthType;
import com.mdframe.forge.starter.crypto.keyexchange.RsaKeyPairHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户名+密码认证策略
 * 支持明文密码和 RSA 加密密码（自动探测解密）
 */
@Slf4j
@Component
public class UsernamePasswordAuthStrategy extends AbstractAuthStrategy {

    @Autowired(required = false)
    private RsaKeyPairHolder rsaKeyPairHolder;

    @Override
    protected void validateRequest(LoginRequest request) {
        validateUsername(request.getUsername());
        validatePassword(request.getPassword());
    }

    @Override
    protected LoginUser doAuthenticate(LoginRequest request) {
        String username = request.getUsername();

        // 1. 加载用户信息
        LoginUser loginUser = userLoadService.loadUserByUsername(username, request.getTenantId());

        // 2. 检查账号是否被锁定
        checkAccountLocked(loginUser);

        // 3. 校验用户是否存在
        if (loginUser == null) {
            recordLoginFailure(null, "用户不存在");
        }

        // 4. 解密密码（如果是 RSA 加密，则先解密；否则直接用明文）
        String rawPassword = decryptPasswordIfNeeded(request.getPassword());

        // 5. 验证密码
        String encodedPassword = userLoadService.getUserPassword(loginUser.getUserId());
        if (!userLoadService.matchPassword(rawPassword, encodedPassword)) {
            recordLoginFailure(loginUser, "密码错误");
        }

        return loginUser;
    }

    /**
     * 尝试 RSA 解密密码（Base64 密文），失败则返回原始密码（明文降级）
     */
    private String decryptPasswordIfNeeded(String password) {
        if (rsaKeyPairHolder == null) {
            return password;
        }
        try {
            String decrypted = rsaKeyPairHolder.decryptByPrivateKey(password);
            if (decrypted != null && !decrypted.isBlank()) {
                return decrypted;
            }
        } catch (Exception e) {
            log.debug("密码 RSA 解密失败，使用明文: {}", e.getMessage());
        }
        return password;
    }

    @Override
    public String getAuthType() {
        return AuthType.PASSWORD.getCode();
    }
}
