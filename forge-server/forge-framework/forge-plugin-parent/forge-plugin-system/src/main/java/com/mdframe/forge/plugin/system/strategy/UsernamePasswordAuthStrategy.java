package com.mdframe.forge.plugin.system.strategy;

import com.mdframe.forge.plugin.system.auth.LoginPasswordDecoder;
import com.mdframe.forge.starter.auth.domain.LoginRequest;
import com.mdframe.forge.starter.auth.enums.AuthType;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户名+密码认证策略
 * 启用登录密码加密时强制使用 RSA 密文；只有显式关闭登录密码加密才接受明文密码。
 */
@Component
public class UsernamePasswordAuthStrategy extends AbstractAuthStrategy {

    @Autowired
    private LoginPasswordDecoder loginPasswordDecoder;

    @Override
    protected void validateRequest(LoginRequest request) {
        validateUsername(request.getUsername());
        validatePassword(request.getPassword());
    }

    @Override
    protected LoginUser doAuthenticate(LoginRequest request) {
        String rawPassword = loginPasswordDecoder.decode(request.getPassword());
        LoginUser loginUser = userLoadService.authenticateByUsernamePassword(
                request.getUsername(), rawPassword, request.getTenantId());
        checkAccountLocked(loginUser);
        if (loginUser == null) {
            recordLoginFailure(null, "用户名或密码错误");
        }
        applyPasswordExpiration(loginUser);
        return loginUser;
    }

    @Override
    public String getAuthType() {
        return AuthType.PASSWORD.getCode();
    }
}
