package com.mdframe.forge.admin.bridge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageMenuDTO;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuRegisterAdapterImpl implements MenuRegisterAdapter {

    private static final String DEFAULT_CLIENT_CODE = "pc";
    private static final String DOMAIN_MENU_PERMS_PREFIX = "ai:lowcode:domain-menu:";
    private static final String BUSINESS_SUITE_MENU_PERMS_PREFIX = "ai:business:suite-menu:";

    /** 业务对象单据动作权限码后缀 → 资源名称，与 BusinessPermissionService 的动作定义保持一致。 */
    private static final LinkedHashMap<String, String> OBJECT_ACTION_PERMISSION_NAMES;

    static {
        OBJECT_ACTION_PERMISSION_NAMES = new LinkedHashMap<>();
        OBJECT_ACTION_PERMISSION_NAMES.put("list", "查看列表");
        OBJECT_ACTION_PERMISSION_NAMES.put("query", "查看详情");
        OBJECT_ACTION_PERMISSION_NAMES.put("add", "新增");
        OBJECT_ACTION_PERMISSION_NAMES.put("edit", "编辑");
        OBJECT_ACTION_PERMISSION_NAMES.put("delete", "删除");
        OBJECT_ACTION_PERMISSION_NAMES.put("export", "导出");
        OBJECT_ACTION_PERMISSION_NAMES.put("import", "导入");
    }

    private final ISysResourceService resourceService;
    private final SysResourceMapper resourceMapper;
    private final SysRoleResourceMapper roleResourceMapper;

    @Override
    public Long registerMenu(String menuName, Long parentId, String configKey, Integer sort) {
        Long tenantId = resolveTenantId();
        String perms = "ai:crud:" + configKey;
        SysResource existing = resourceMapper.selectOneByPermsAndClientCodeAnyType(
                tenantId, perms, DEFAULT_CLIENT_CODE);
        SysResource resource = new SysResource();
        resource.setId(existing == null ? null : existing.getId());
        resource.setTenantId(tenantId);
        resource.setResourceName(menuName);
        resource.setParentId(parentId != null ? parentId : resolveDefaultLowcodeParentId());
        resource.setResourceType(2);
        resource.setSort(sort);
        resource.setPath("/ai/crud-page/" + configKey);
        resource.setComponent("ai/crud-page");
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(EnableStatus.ENABLED.getCode());
        resource.setVisible(EnableStatus.ENABLED.getCode());
        resource.setPerms(perms);
        resource.setKeepAlive(0);
        resource.setAlwaysShow(0);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        if (existing == null) {
            resourceService.save(resource);
        } else {
            resourceService.updateById(resource);
        }

        Long menuId = resource.getId();
        log.info("[MenuRegisterAdapter] 注册菜单成功: menuName={}, configKey={}, menuId={}", menuName, configKey, menuId);
        return menuId;
    }

    @Override
    public void updateMenu(Long menuResourceId, String menuName, Long parentId, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(menuResourceId);
        resource.setResourceName(menuName);
        if (parentId != null) {
            resource.setParentId(parentId);
        }
        resource.setSort(sort);
        resourceService.updateById(resource);
        log.info("[MenuRegisterAdapter] 更新菜单成功: menuId={}, menuName={}, parentId={}", menuResourceId, menuName, parentId);
    }

    @Override
    public void deleteMenu(Long menuResourceId) {
        resourceService.removeById(menuResourceId);
        log.info("[MenuRegisterAdapter] 删除菜单成功: menuId={}", menuResourceId);
    }

    @Override
    public void disableMenu(Long menuResourceId) {
        if (menuResourceId == null) {
            return;
        }
        SysResource resource = new SysResource();
        resource.setId(menuResourceId);
        resource.setMenuStatus(EnableStatus.DISABLED.getCode());
        resource.setVisible(EnableStatus.DISABLED.getCode());
        resourceService.updateById(resource);
        log.info("[MenuRegisterAdapter] 禁用菜单成功: menuId={}", menuResourceId);
    }

    @Override
    public Long registerAppMenu(String menuName, Long parentId, String path, String component,
                                String perms, String icon, Integer sort, boolean enabled) {
        return registerAppMenu(menuName, parentId, path, component, perms, icon, sort, enabled,
                DEFAULT_CLIENT_CODE);
    }

    @Override
    public Long registerAppMenu(String menuName, Long parentId, String path, String component,
                                String perms, String icon, Integer sort, boolean enabled, String clientCode) {
        Long tenantId = resolveTenantId();
        String resolvedClientCode = StringUtils.defaultIfBlank(clientCode, DEFAULT_CLIENT_CODE);
        // Menu resources are isolated by client.  Looking up by permission only
        // would make a mobile registration reuse the management-side row (and
        // would also hide the mobile row when both clients share a permission).
        SysResource existing = resourceMapper.selectOneByPermsAndClientCode(
                tenantId, 2, perms, resolvedClientCode);
        if (existing == null) {
            // 兼容历史应用入口从目录/菜单类型迁移的情况；唯一键仍按
            // perms 约束，必须复用旧资源而不是再次 insert。
            existing = resourceMapper.selectOneByPermsAndClientCodeAnyType(
                    tenantId, perms, resolvedClientCode);
        }
        if (existing != null && existing.getId() != null) {
            updateAppMenu(existing.getId(), menuName, parentId, path, component, perms, icon, sort, enabled,
                    resolvedClientCode);
            return existing.getId();
        }
        SysResource existingByPath = resourceMapper.selectOneByPathAndClientCode(
                tenantId, 2, path, resolvedClientCode);
        if (existingByPath != null && existingByPath.getId() != null) {
            updateAppMenu(existingByPath.getId(), menuName, parentId, path, component, perms, icon, sort, enabled,
                    resolvedClientCode);
            return existingByPath.getId();
        }
        SysResource resource = new SysResource();
        resource.setTenantId(tenantId);
        resource.setResourceName(menuName);
        resource.setParentId(parentId != null ? parentId : 0L);
        resource.setResourceType(2);
        resource.setSort(sort != null ? sort : 0);
        resource.setPath(path);
        resource.setComponent(component);
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(enabled ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        resource.setVisible(enabled ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        resource.setPerms(perms);
        resource.setIcon(StringUtils.defaultIfBlank(icon, "ionicons5:AppsOutline"));
        resource.setKeepAlive(0);
        resource.setAlwaysShow(0);
        resource.setClientCode(resolvedClientCode);
        resourceService.save(resource);
        log.info("[MenuRegisterAdapter] 注册应用入口菜单成功: menuName={}, path={}, clientCode={}, menuId={}",
                menuName, path, resolvedClientCode, resource.getId());
        return resource.getId();
    }

    @Override
    public void updateAppMenu(Long menuResourceId, String menuName, Long parentId, String path,
                              String component, String perms, String icon, Integer sort, boolean enabled) {
        updateAppMenu(menuResourceId, menuName, parentId, path, component, perms, icon, sort, enabled,
                DEFAULT_CLIENT_CODE);
    }

    @Override
    public void updateAppMenu(Long menuResourceId, String menuName, Long parentId, String path,
                              String component, String perms, String icon, Integer sort, boolean enabled,
                              String clientCode) {
        Long resolvedParentId = normalizeResourceParentId(menuResourceId, parentId);
        SysResource resource = new SysResource();
        resource.setId(menuResourceId);
        resource.setResourceName(menuName);
        resource.setParentId(resolvedParentId);
        resource.setPath(path);
        resource.setComponent(component);
        resource.setPerms(perms);
        resource.setIcon(StringUtils.defaultIfBlank(icon, "ionicons5:AppsOutline"));
        resource.setSort(sort != null ? sort : 0);
        resource.setMenuStatus(enabled ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        resource.setVisible(enabled ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        resource.setClientCode(StringUtils.defaultIfBlank(clientCode, DEFAULT_CLIENT_CODE));
        resourceService.updateById(resource);
        log.info("[MenuRegisterAdapter] 更新应用入口菜单成功: menuId={}, menuName={}, parentId={}, clientCode={}",
                menuResourceId, menuName, resolvedParentId, resource.getClientCode());
    }

    @Override
    public boolean hasRolePermission(Long menuResourceId) {
        if (menuResourceId == null) {
            return false;
        }
        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getResourceId, menuResourceId);
        return roleResourceMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long resolveDefaultLowcodeParentId() {
        SysResource aiRoot = resourceMapper.selectOneByPath(resolveTenantId(), 1, "/ai");
        return aiRoot != null && aiRoot.getId() != null ? aiRoot.getId() : 0L;
    }

    @Override
    public Long resolveOrCreateDomainParentId(String domainCode, String domainName, Integer sort) {
        return resolveOrCreateDomainParentId(domainCode, domainName, sort, resolveDefaultLowcodeParentId());
    }

    @Override
    public Long resolveOrCreateDomainParentId(String domainCode, String domainName, Integer sort, Long parentMenuId) {
        String normalizedCode = StringUtils.trimToNull(domainCode);
        if (normalizedCode == null) {
            return null;
        }
        Long tenantId = resolveTenantId();
        String perms = DOMAIN_MENU_PERMS_PREFIX + normalizedCode;
        SysResource existing = resourceMapper.selectOneByPerms(tenantId, 1, perms);
        Long resolvedParentId = normalizeDomainParentId(existing == null ? null : existing.getId(), parentMenuId);
        if (existing != null && existing.getId() != null) {
            updateDomainParentIfNeeded(existing, resolvedParentId, domainName, sort);
            return existing.getId();
        }

        SysResource resource = new SysResource();
        resource.setTenantId(tenantId);
        resource.setResourceName(StringUtils.defaultIfBlank(domainName, normalizedCode));
        resource.setParentId(resolvedParentId);
        resource.setResourceType(1);
        resource.setSort(sort != null ? sort : 0);
        resource.setPath("/ai/lowcode-domain/" + normalizedCode);
        resource.setComponent(null);
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(EnableStatus.ENABLED.getCode());
        resource.setVisible(EnableStatus.ENABLED.getCode());
        resource.setPerms(perms);
        resource.setIcon("ionicons5:FolderOpenOutline");
        resource.setKeepAlive(0);
        resource.setAlwaysShow(1);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        resourceService.save(resource);
        log.info("[MenuRegisterAdapter] 创建领域菜单目录成功: domainCode={}, parentId={}, menuId={}",
                normalizedCode, resolvedParentId, resource.getId());
        return resource.getId();
    }

    @Override
    public Long resolveOrCreateBusinessSuiteParentId(Long parentId, String suiteCode, String suiteName,
                                                     String icon, Integer sort) {
        String normalizedCode = StringUtils.trimToNull(suiteCode);
        if (normalizedCode == null) {
            return parentId;
        }
        Long tenantId = resolveTenantId();
        String perms = BUSINESS_SUITE_MENU_PERMS_PREFIX + normalizedCode;
        SysResource existing = resourceMapper.selectOneByPerms(tenantId, 1, perms);
        Long resolvedParentId = parentId != null ? parentId : 0L;
        if (existing != null && existing.getId() != null) {
            resolvedParentId = normalizeResourceParentId(existing.getId(), resolvedParentId);
            updateBusinessSuiteParentIfNeeded(existing, resolvedParentId, suiteName, icon, sort);
            return existing.getId();
        }

        SysResource resource = new SysResource();
        resource.setTenantId(tenantId);
        resource.setResourceName(StringUtils.defaultIfBlank(suiteName, normalizedCode));
        resource.setParentId(resolvedParentId);
        resource.setResourceType(1);
        resource.setSort(sort != null ? sort : 0);
        resource.setPath("/app-center/suite-menu/" + normalizedCode);
        resource.setComponent(null);
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(EnableStatus.ENABLED.getCode());
        resource.setVisible(EnableStatus.ENABLED.getCode());
        resource.setPerms(perms);
        resource.setIcon(StringUtils.defaultIfBlank(icon, "ionicons5:AlbumsOutline"));
        resource.setKeepAlive(0);
        resource.setAlwaysShow(1);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        resourceService.save(resource);
        log.info("[MenuRegisterAdapter] 创建业务套件菜单目录成功: suiteCode={}, menuId={}",
                normalizedCode, resource.getId());
        return resource.getId();
    }

    @Override
    public Map<String, Long> syncApplicationPageMenus(String applicationCode,
                                                       List<BusinessApplicationPageMenuDTO> menus) {
        String normalizedCode = StringUtils.trimToNull(applicationCode);
        if (normalizedCode == null) {
            return Map.of();
        }
        Long tenantId = resolveTenantId();
        String prefix = "ai:business:application:" + normalizedCode + ":page:";
        List<BusinessApplicationPageMenuDTO> pending = new ArrayList<>(menus == null ? List.of() : menus);
        pending.removeIf(item -> item == null || StringUtils.isBlank(item.getNodeId()) || StringUtils.isBlank(item.getPerms()));
        pending.sort(Comparator.comparing(item -> item.getSort() == null ? 0 : item.getSort()));
        Map<String, Long> resolvedIds = new LinkedHashMap<>();
        Set<String> activePerms = new HashSet<>();
        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (java.util.Iterator<BusinessApplicationPageMenuDTO> iterator = pending.iterator(); iterator.hasNext();) {
                BusinessApplicationPageMenuDTO item = iterator.next();
                String clientCode = StringUtils.defaultIfBlank(item.getClientCode(), DEFAULT_CLIENT_CODE);
                Long parentId;
                if (item.isExternalParent()) {
                    // 外部门级：parentId 直接是 sys_resource.id，不经内部节点树解析
                    parentId = parseLong(item.getParentNodeId());
                } else if (item.getParentNodeId() == null) {
                    // 应用页面菜单不再默认挂到 /ai（AI应用）。未指定父级时挂顶级，
                    // 正式挂载应通过 externalParent + menuParentId 显式指定。
                    parentId = 0L;
                } else {
                    parentId = resolvedIds.get(resourceKey(item.getParentNodeId(), clientCode));
                }
                if (item.getParentNodeId() != null && parentId == null) {
                    continue;
                }
                Long resourceId = upsertApplicationPageMenu(tenantId, item, parentId);
                resolvedIds.put(resourceKey(item.getNodeId(), clientCode), resourceId);
                // Keep the historical plain key for pc-only callers/snapshots.
                resolvedIds.putIfAbsent(item.getNodeId(), resourceId);
                activePerms.add(resourceKey(item.getPerms(), clientCode));
                iterator.remove();
                progressed = true;
            }
            if (!progressed) {
                throw new IllegalStateException("应用页面菜单父级引用无效或存在循环");
            }
        }
        for (SysResource stale : resourceMapper.selectList(new LambdaQueryWrapper<SysResource>()
                .eq(SysResource::getTenantId, tenantId)
                .likeRight(SysResource::getPerms, prefix))) {
            if (!activePerms.contains(resourceKey(stale.getPerms(),
                    StringUtils.defaultIfBlank(stale.getClientCode(), DEFAULT_CLIENT_CODE)))) {
                SysResource disabled = new SysResource();
                disabled.setId(stale.getId());
                disabled.setMenuStatus(EnableStatus.DISABLED.getCode());
                disabled.setVisible(EnableStatus.DISABLED.getCode());
                resourceService.updateById(disabled);
            }
        }
        return resolvedIds;
    }

    /**
     * 对象发布时将单据动作权限码注册为按钮资源：存在则刷新名称/父级，不存在则新建，
     * 保证重复发布幂等；角色授权由管理员在角色管理中按需勾选，不自动授予。
     */
    @Override
    public void syncBusinessObjectActionPermissions(String objectCode, String objectName, String menuConfigKey) {
        String normalizedObjectCode = StringUtils.trimToNull(objectCode);
        if (normalizedObjectCode == null) {
            return;
        }
        Long tenantId = resolveTenantId();
        String displayName = StringUtils.defaultIfBlank(objectName, normalizedObjectCode);
        Long parentId = resolveObjectButtonParentId(tenantId, menuConfigKey);
        int sort = 0;
        for (Map.Entry<String, String> action : OBJECT_ACTION_PERMISSION_NAMES.entrySet()) {
            String perms = "ai:business:" + normalizedObjectCode + ":" + action.getKey();
            String resourceName = displayName + "-" + action.getValue();
            upsertButtonResource(tenantId, perms, resourceName, parentId, sort++);
        }
        log.info("[MenuRegisterAdapter] 同步业务对象按钮权限成功: objectCode={}, menuConfigKey={}",
                normalizedObjectCode, menuConfigKey);
    }

    private Long resolveObjectButtonParentId(Long tenantId, String menuConfigKey) {
        String configKey = StringUtils.trimToNull(menuConfigKey);
        if (configKey != null) {
            SysResource objectMenu = resourceMapper.selectOneByPerms(tenantId, 2, "ai:crud:" + configKey);
            if (objectMenu != null && objectMenu.getId() != null) {
                return objectMenu.getId();
            }
        }
        return resolveDefaultLowcodeParentId();
    }

    private void upsertButtonResource(Long tenantId, String perms, String resourceName, Long parentId, int sort) {
        SysResource existing = resourceMapper.selectOneByPerms(tenantId, 3, perms);
        if (existing != null && existing.getId() != null) {
            SysResource resource = new SysResource();
            resource.setId(existing.getId());
            boolean changed = false;
            if (!resourceName.equals(existing.getResourceName())) {
                resource.setResourceName(resourceName);
                changed = true;
            }
            if (parentId != null && !parentId.equals(existing.getParentId())) {
                resource.setParentId(parentId);
                changed = true;
            }
            if (changed) {
                resourceService.updateById(resource);
            }
            return;
        }
        SysResource resource = new SysResource();
        resource.setTenantId(tenantId);
        resource.setResourceName(resourceName);
        resource.setParentId(parentId);
        resource.setResourceType(3);
        resource.setSort(sort);
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        resource.setMenuStatus(EnableStatus.ENABLED.getCode());
        resource.setVisible(EnableStatus.ENABLED.getCode());
        resource.setPerms(perms);
        resource.setKeepAlive(0);
        resource.setAlwaysShow(0);
        resource.setClientCode(DEFAULT_CLIENT_CODE);
        resourceService.save(resource);
    }

    private Long upsertApplicationPageMenu(Long tenantId, BusinessApplicationPageMenuDTO item, Long parentId) {
        int resourceType = item.isDirectory() ? 1 : 2;
        String clientCode = StringUtils.defaultIfBlank(item.getClientCode(), DEFAULT_CLIENT_CODE);
        SysResource existing = resourceMapper.selectOneByPermsAndClientCode(
                tenantId, resourceType, item.getPerms(), clientCode);
        if (existing == null) {
            // 页面组/根节点历史上可能以菜单类型落库。查询不能把
            // resource_type 当成资源身份，否则会绕过唯一性校验并触发
            // uk_tenant_resource_active 冲突。
            existing = resourceMapper.selectOneByPermsAndClientCodeAnyType(
                    tenantId, item.getPerms(), clientCode);
        }
        SysResource resource = new SysResource();
        resource.setId(existing == null ? null : existing.getId());
        resource.setTenantId(tenantId);
        resource.setResourceName(item.getMenuName());
        resource.setParentId(normalizeResourceParentId(resource.getId(), parentId));
        resource.setResourceType(resourceType);
        resource.setSort(item.getSort() == null ? 0 : item.getSort());
        resource.setPath(item.getPath());
        resource.setComponent(item.isDirectory() ? null : item.getComponent());
        resource.setIsExternal(0);
        resource.setIsPublic(0);
        // 同步清单内的资源始终保持 menu_status=启用，便于角色授权；
        // visible 只控制是否进入系统侧栏（门户入口常用 visible=0 的隐藏资源）。
        resource.setMenuStatus(EnableStatus.ENABLED.getCode());
        resource.setVisible(item.isVisible() ? EnableStatus.ENABLED.getCode() : EnableStatus.DISABLED.getCode());
        resource.setPerms(item.getPerms());
        resource.setIcon(StringUtils.defaultIfBlank(item.getIcon(), item.isDirectory()
                ? "ionicons5:FolderOpenOutline" : "ionicons5:AppsOutline"));
        resource.setKeepAlive(0);
        resource.setAlwaysShow(item.isDirectory() ? 1 : 0);
        resource.setClientCode(clientCode);
        if (existing == null) {
            resourceService.save(resource);
        } else {
            resourceService.updateById(resource);
        }
        return resource.getId();
    }

    private String resourceKey(String nodeOrPerms, String clientCode) {
        return StringUtils.defaultString(nodeOrPerms) + "::"
                + StringUtils.defaultIfBlank(clientCode, DEFAULT_CLIENT_CODE);
    }

    private void updateDomainParentIfNeeded(SysResource existing, Long parentId, String domainName, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(existing.getId());
        boolean changed = false;
        if (parentId != null && !parentId.equals(existing.getParentId())) {
            resource.setParentId(parentId);
            changed = true;
        }
        if (StringUtils.isNotBlank(domainName) && !domainName.equals(existing.getResourceName())) {
            resource.setResourceName(domainName);
            changed = true;
        }
        if (sort != null && !sort.equals(existing.getSort())) {
            resource.setSort(sort);
            changed = true;
        }
        if (!EnableStatus.ENABLED.matches(existing.getMenuStatus())) {
            resource.setMenuStatus(EnableStatus.ENABLED.getCode());
            changed = true;
        }
        if (!EnableStatus.ENABLED.matches(existing.getVisible())) {
            resource.setVisible(EnableStatus.ENABLED.getCode());
            changed = true;
        }
        if (changed) {
            resourceService.updateById(resource);
        }
    }

    private Long normalizeDomainParentId(Long resourceId, Long parentId) {
        Long fallbackParentId = resolveDefaultLowcodeParentId();
        Long resolvedParentId = parentId != null ? parentId : fallbackParentId;
        if (resourceId != null && resourceId.equals(resolvedParentId)) {
            log.warn("[MenuRegisterAdapter] 检测到领域目录自父级配置，已自动挂载到低代码根目录: menuId={}", resourceId);
            return fallbackParentId;
        }
        return resolvedParentId;
    }

    private void updateBusinessSuiteParentIfNeeded(SysResource existing, Long parentId, String suiteName,
                                                   String icon, Integer sort) {
        SysResource resource = new SysResource();
        resource.setId(existing.getId());
        boolean changed = false;
        if (parentId != null && !parentId.equals(existing.getParentId())) {
            resource.setParentId(parentId);
            changed = true;
        }
        if (StringUtils.isNotBlank(suiteName) && !suiteName.equals(existing.getResourceName())) {
            resource.setResourceName(suiteName);
            changed = true;
        }
        if (StringUtils.isNotBlank(icon) && !icon.equals(existing.getIcon())) {
            resource.setIcon(icon);
            changed = true;
        }
        if (sort != null && !sort.equals(existing.getSort())) {
            resource.setSort(sort);
            changed = true;
        }
        if (!EnableStatus.ENABLED.matches(existing.getMenuStatus())) {
            resource.setMenuStatus(EnableStatus.ENABLED.getCode());
            changed = true;
        }
        if (!EnableStatus.ENABLED.matches(existing.getVisible())) {
            resource.setVisible(EnableStatus.ENABLED.getCode());
            changed = true;
        }
        if (changed) {
            resourceService.updateById(resource);
        }
    }

    private Long normalizeResourceParentId(Long resourceId, Long parentId) {
        Long resolvedParentId = parentId != null ? parentId : 0L;
        if (resourceId != null && resourceId.equals(resolvedParentId)) {
            log.warn("[MenuRegisterAdapter] 检测到菜单自父级配置，已自动挂载到顶级: menuId={}", resourceId);
            return 0L;
        }
        return resolvedParentId;
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
