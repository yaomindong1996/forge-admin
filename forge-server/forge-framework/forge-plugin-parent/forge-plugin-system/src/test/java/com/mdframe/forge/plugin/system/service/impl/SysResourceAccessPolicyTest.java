package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SysResourceAccessPolicyTest {

    @Test
    void shouldProtectCorePathsComponentsAndPerms() {
        assertTrue(SysResourceAccessPolicy.isProtected(resource("平台管理", "/platform", null, null)));
        assertTrue(SysResourceAccessPolicy.isProtected(resource("用户管理", "/system/user", "system/user", "system:user:list")));
        assertTrue(SysResourceAccessPolicy.isProtected(resource("用户查询", null, null, "system:user:query")));
        assertTrue(SysResourceAccessPolicy.isProtected(resource("菜单", "/system/menu/", "system/menu.vue", null)));
        assertFalse(SysResourceAccessPolicy.isProtected(resource("采购单", "/biz/purchase", "biz/purchase", "biz:purchase:list")));
        assertFalse(SysResourceAccessPolicy.isProtected(resource("缓存策略", "/system/cache", "system/cache", "system:cache:list")));
    }

    @Test
    void shouldRejectDeletingProtectedResourcesWithReadableMessage() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> SysResourceAccessPolicy.assertDeletable(List.of(
                        resource("用户管理", "/system/user", "system/user", "system:user:list"),
                        resource("自定义", "/biz/demo", "biz/demo", "biz:demo:list"))));
        assertTrue(error.getMessage().contains("系统核心资源不能删除"));
        assertTrue(error.getMessage().contains("用户管理"));
    }

    @Test
    void shouldAllowDeletingCustomResources() {
        assertDoesNotThrow(() -> SysResourceAccessPolicy.assertDeletable(List.of(
                resource("自定义菜单", "/biz/demo", "biz/demo", "biz:demo:list"))));
    }

    private static SysResource resource(String name, String path, String component, String perms) {
        SysResource resource = new SysResource();
        resource.setId(1L);
        resource.setResourceName(name);
        resource.setPath(path);
        resource.setComponent(component);
        resource.setPerms(perms);
        return resource;
    }
}
