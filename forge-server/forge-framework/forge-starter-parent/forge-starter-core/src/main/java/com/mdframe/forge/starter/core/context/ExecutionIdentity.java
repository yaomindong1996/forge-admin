package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.session.LoginUser;

import java.util.Objects;
import java.util.Set;

/**
 * 协议无关的受信执行身份快照。
 */
public record ExecutionIdentity(
        LoginUser loginUser,
        String actorType,
        Long actorUserId,
        Long serviceUserId,
        Long clientId,
        String clientCode,
        String tokenId,
        Set<String> scopes) {

    public ExecutionIdentity {
        loginUser = snapshot(Objects.requireNonNull(loginUser, "loginUser 不能为空"));
        actorType = requireText(actorType, "actorType");
        actorUserId = requirePositive(actorUserId, "actorUserId");
        if ("SERVICE".equals(actorType)) {
            serviceUserId = requirePositive(serviceUserId, "serviceUserId");
            if (!actorUserId.equals(serviceUserId)) {
                throw new IllegalArgumentException("SERVICE 主体必须与 serviceUserId 一致");
            }
        }
        else if (serviceUserId != null) {
            serviceUserId = requirePositive(serviceUserId, "serviceUserId");
        }
        clientId = requirePositive(clientId, "clientId");
        clientCode = requireText(clientCode, "clientCode");
        tokenId = requireText(tokenId, "tokenId");
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        if (!actorUserId.equals(loginUser.getUserId())) {
            throw new IllegalArgumentException("actorUserId 必须与 loginUser.userId 一致");
        }
    }

    @Override
    public LoginUser loginUser() {
        return snapshot(loginUser);
    }

    private static LoginUser snapshot(LoginUser source) {
        return LoginUserSnapshot.copy(source);
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
