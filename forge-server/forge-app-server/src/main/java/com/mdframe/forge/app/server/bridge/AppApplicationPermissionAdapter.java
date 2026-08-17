package com.mdframe.forge.app.server.bridge;

import com.mdframe.forge.plugin.generator.service.ApplicationPermissionAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * App 服务只承载低代码运行态，不提供应用权限管理能力。
 */
@Component
public class AppApplicationPermissionAdapter implements ApplicationPermissionAdapter {

    private static final String UNSUPPORTED_MESSAGE = "App 服务不提供应用权限管理能力";

    @Override
    public List<RoleInfo> listAssignableRoles() {
        throw unsupported();
    }

    @Override
    public Map<String, ResourceInfo> findResourcesByPermissions(Set<String> permissions) {
        throw unsupported();
    }

    @Override
    public RoleGrant loadRoleGrant(Long roleId, Set<Long> resourceIds, Set<String> moduleCodes) {
        throw unsupported();
    }

    @Override
    public void saveRoleGrant(Long roleId,
                              Set<Long> scopeResourceIds,
                              Set<Long> selectedResourceIds,
                              Set<String> scopeModuleCodes,
                              Map<String, Integer> moduleScopes) {
        throw unsupported();
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }
}
