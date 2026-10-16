package com.mdframe.forge.starter.core.session;

import cn.dev33.satoken.stp.StpUtil;
import com.mdframe.forge.starter.core.context.ExecutionIdentityContextHolder;
import com.mdframe.forge.starter.core.context.LoginUserContextHolder;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.List;
import java.util.Set;

/**
 * Session助手类，用于获取当前登录用户信息
 */
public class SessionHelper {

    private static final String LOGIN_USER_KEY = "loginUser";

    /**
     * 设置用户Session
     */
    public static void setLoginUser(LoginUser loginUser) {
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * 获取当前登录用户信息。
     * 未登录或无有效 token 时返回 null，不抛 NotLoginException
     *（登录页匿名接口如 /auth/loginConfig 会间接触发此方法）。
     */
    public static LoginUser getLoginUser() {
        LoginUser explicitUser = ExecutionIdentityContextHolder.current()
                .map(identity -> identity.loginUser())
                .orElse(null);
        if (explicitUser != null) {
            return explicitUser;
        }
        // 机器身份优先；普通受信后台任务不伪造 MCP clientId，也不持有 Servlet 请求。
        LoginUser taskUser = LoginUserContextHolder.current();
        if (taskUser != null) {
            return taskUser;
        }
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return (LoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getUserId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser != null && loginUser.getUserId() != null) {
            return loginUser.getUserId();
        }
        // Token 已登录但 Session 里尚未写入 loginUser 时，回退到 Sa-Token loginId，
        // 避免私有文件 uploaderId 落库为 null，导致后续无法读取。
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
            // 无登录上下文
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     */
    public static String getUsername() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    /**
     * 获取当前租户ID
     */
    public static Long getTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getTenantId() : null;
    }

    /**
     * 获取当前用户的角色ID列表
     */
    public static List<Long> getRoleIds() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRoleIds() : null;
    }

    /**
     * 获取当前用户的角色编码列表
     */
    public static Set<String> getRoleKeys() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getRoleKeys() : null;
    }

    /**
     * 获取当前用户的权限标识列表
     */
    public static Set<String> getPermissions() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getPermissions() : null;
    }

    /**
     * 获取当前用户的组织ID列表
     */
    public static List<Long> getOrgIds() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getOrgIds() : null;
    }

    /**
     * 获取当前用户的主组织ID
     */
    public static Long getMainOrgId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getMainOrgId() : null;
    }

    /**
     * 获取当前用户的当前组织ID
     */
    public static Long getActiveOrgId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getActiveOrgId() : null;
    }

    /**
     * 获取当前用户的当前组织名称
     */
    public static String getActiveOrgName() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getActiveOrgName() : null;
    }

    /**
     * 判断当前用户是否为超级管理员
     */
    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isAdmin();
    }

    /**
     * 断言当前用户为超级管理员
     */
    public static void assertAdmin() {
        assertAdmin("只有超级管理员可以操作平台配置");
    }

    /**
     * 断言当前用户为超级管理员
     */
    public static void assertAdmin(String message) {
        if (!isAdmin()) {
            throw new BusinessException(403, message);
        }
    }

    /**
     * 判断当前用户是否为租户管理员
     */
    public static boolean isTenantAdmin() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null && loginUser.isTenantAdmin();
    }

    /**
     * 判断当前用户是否拥有指定角色
     */
    public static boolean hasRole(String roleKey) {
        Set<String> roleKeys = getRoleKeys();
        return roleKeys != null && roleKeys.contains(roleKey);
    }

    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        // 超级管理员拥有所有权限
        if (isAdmin()) {
            return true;
        }
        Set<String> permissions = getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        if (permissions.contains(permission) || permissions.contains("*") || permissions.contains("*:*:*")) {
            return true;
        }
        return false;
    }

    /**
     * 判断当前用户是否拥有任意一个指定权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (isAdmin()) {
            return true;
        }
        Set<String> userPermissions = getPermissions();
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前用户是否拥有所有指定权限
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (isAdmin()) {
            return true;
        }
        Set<String> userPermissions = getPermissions();
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 清除当前用户Session
     */
    public static void clearSession() {
        StpUtil.getTokenSession().clear();
    }
}
