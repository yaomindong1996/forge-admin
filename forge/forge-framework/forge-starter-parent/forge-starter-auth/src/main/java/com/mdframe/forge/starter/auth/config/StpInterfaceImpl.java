package com.mdframe.forge.starter.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Set<String> permissions = SessionHelper.getPermissions();
        return permissions != null ? new ArrayList<>(permissions) : new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Set<String> roleKeys = SessionHelper.getRoleKeys();
        return roleKeys != null ? new ArrayList<>(roleKeys) : new ArrayList<>();
    }
}
