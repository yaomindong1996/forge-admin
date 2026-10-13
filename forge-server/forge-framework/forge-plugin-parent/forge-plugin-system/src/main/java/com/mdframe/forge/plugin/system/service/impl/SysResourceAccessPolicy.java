package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.starter.core.exception.BusinessException;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 核心系统菜单/权限资源删除保护，避免误删导致管理面不可用。
 * <p>
 * 仅拦截登录、用户/角色/组织/租户/菜单/客户端/在线会话等底座能力；
 * 业务自定义菜单与其它 system 扩展页不受影响。
 */
final class SysResourceAccessPolicy {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/platform",
            "/system/user",
            "/system/role",
            "/system/menu",
            "/system/org",
            "/system/tenant",
            "/system/client",
            "/system/online"
    );

    private static final Set<String> PROTECTED_COMPONENTS = Set.of(
            "system/user",
            "system/role",
            "system/menu",
            "system/org",
            "system/tenant",
            "system/client",
            "system/online"
    );

    private static final Set<String> PROTECTED_PERM_PREFIXES = Set.of(
            "system:user:",
            "system:role:",
            "system:menu:",
            "system:org:",
            "system:tenant:",
            "system:client:",
            "system:online:"
    );

    private static final Set<String> PROTECTED_EXACT_PERMS = Set.of(
            "system:user:list",
            "system:role:list",
            "system:menu:list",
            "system:org:list",
            "system:tenant:list",
            "system:client:list",
            "system:online:query"
    );

    private SysResourceAccessPolicy() {
    }

    static void assertDeletable(Collection<SysResource> resources) {
        if (CollUtil.isEmpty(resources)) {
            return;
        }
        List<String> blocked = resources.stream()
                .filter(SysResourceAccessPolicy::isProtected)
                .map(SysResourceAccessPolicy::describe)
                .collect(Collectors.toList());
        if (!blocked.isEmpty()) {
            throw new BusinessException("系统核心资源不能删除：" + String.join("、", blocked));
        }
    }

    static boolean isProtected(SysResource resource) {
        if (resource == null) {
            return false;
        }
        String path = normalizePath(resource.getPath());
        if (path != null && PROTECTED_PATHS.contains(path)) {
            return true;
        }
        String component = normalizeComponent(resource.getComponent());
        if (component != null && PROTECTED_COMPONENTS.contains(component)) {
            return true;
        }
        String perms = normalizePerms(resource.getPerms());
        if (perms == null) {
            return false;
        }
        if (PROTECTED_EXACT_PERMS.contains(perms)) {
            return true;
        }
        for (String prefix : PROTECTED_PERM_PREFIXES) {
            if (perms.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(SysResource resource) {
        if (StrUtil.isNotBlank(resource.getResourceName())) {
            return resource.getResourceName();
        }
        if (StrUtil.isNotBlank(resource.getPerms())) {
            return resource.getPerms();
        }
        return String.valueOf(resource.getId());
    }

    private static String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String normalized = path.trim();
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizeComponent(String component) {
        if (StrUtil.isBlank(component)) {
            return null;
        }
        String normalized = component.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".vue")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private static String normalizePerms(String perms) {
        return StrUtil.isBlank(perms) ? null : perms.trim().toLowerCase(Locale.ROOT);
    }
}
