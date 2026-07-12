package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.session.LoginUser;

import java.util.List;
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
        serviceUserId = requirePositive(serviceUserId, "serviceUserId");
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
        LoginUser target = new LoginUser();
        target.setUserId(source.getUserId());
        target.setTenantId(source.getTenantId());
        target.setTenantName(source.getTenantName());
        target.setTenantIds(copyList(source.getTenantIds()));
        target.setUsername(source.getUsername());
        target.setRealName(source.getRealName());
        target.setUserType(source.getUserType());
        target.setPhone(source.getPhone());
        target.setEmail(source.getEmail());
        target.setAvatar(source.getAvatar());
        target.setUserStatus(source.getUserStatus());
        target.setForcePasswordChange(source.getForcePasswordChange());
        target.setRoleIds(copyList(source.getRoleIds()));
        target.setRoleKeys(copySet(source.getRoleKeys()));
        target.setPermissions(copySet(source.getPermissions()));
        target.setApiPermissions(copyList(source.getApiPermissions()));
        target.setOrgIds(copyList(source.getOrgIds()));
        target.setMainOrgId(source.getMainOrgId());
        target.setActiveOrgId(source.getActiveOrgId());
        target.setActiveOrgName(source.getActiveOrgName());
        target.setLoginTime(source.getLoginTime());
        target.setLoginIp(source.getLoginIp());
        target.setUserClient(source.getUserClient());
        target.setDeptName(source.getDeptName());
        target.setRegionCode(source.getRegionCode());
        target.setRegionName(source.getRegionName());
        target.setRegionLevel(source.getRegionLevel());
        target.setRegionFullName(source.getRegionFullName());
        target.setRegionAncestors(source.getRegionAncestors());
        target.setCreateTime(source.getCreateTime());
        return target;
    }

    private static <T> List<T> copyList(List<T> source) {
        return source == null ? null : List.copyOf(source);
    }

    private static <T> Set<T> copySet(Set<T> source) {
        return source == null ? null : Set.copyOf(source);
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
