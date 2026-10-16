package com.mdframe.forge.starter.core.context;

import com.mdframe.forge.starter.core.session.LoginUser;

import java.util.List;
import java.util.Set;

/** 只复制已验证的用户数据，不绑定客户端、不携带原始 Token，不共享可变权限集合。 */
public final class LoginUserSnapshot {

    private LoginUserSnapshot() {
    }

    public static LoginUser copy(LoginUser source) {
        LoginUser target = new LoginUser();
        target.setUserId(source.getUserId());
        target.setTenantId(source.getTenantId());
        target.setTenantName(source.getTenantName());
        target.setSystemName(source.getSystemName());
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
}
