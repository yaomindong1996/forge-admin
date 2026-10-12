package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.RoleDataScopeSettingsDTO;
import com.mdframe.forge.plugin.system.dto.RoleModuleDataScopeDTO;
import com.mdframe.forge.plugin.system.dto.ScopedRolePermissionDTO;
import com.mdframe.forge.plugin.system.dto.RoleUserQuery;
import com.mdframe.forge.plugin.system.dto.SysRoleDTO;
import com.mdframe.forge.plugin.system.dto.SysRoleQuery;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleOrg;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.plugin.system.service.ISysRoleService;
import com.mdframe.forge.plugin.system.vo.RoleDataScopeSettingsVO;
import com.mdframe.forge.plugin.system.vo.RoleModuleDataScopeVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.datascope.entity.SysDataScopeConfig;
import com.mdframe.forge.starter.datascope.entity.SysRoleModuleDataScope;
import com.mdframe.forge.starter.datascope.mapper.SysDataScopeConfigMapper;
import com.mdframe.forge.starter.datascope.mapper.SysRoleModuleDataScopeMapper;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysOrgMapper orgMapper;
    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysResourceMapper resourceMapper;
    private final SysDataScopeConfigMapper dataScopeConfigMapper;
    private final SysRoleModuleDataScopeMapper roleModuleDataScopeMapper;
    private final IDataScopeService dataScopeService;
    @Lazy
    private final ISysResourceService resourceService;

    @Override
    public IPage<SysRole> selectRolePage(SysRoleQuery query) {
        accessPolicy().assertRoleManagementAllowed();
        query = query == null ? new SysRoleQuery() : query;
        accessPolicy().normalizeRoleQueryTenant(query);
        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        SysRoleQuery finalQuery = query;
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRolePage(page, finalQuery));
    }

    @Override
    public SysRole selectRoleById(Long id) {
        accessPolicy().loadRoleForAccess(id);
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRoleById(id));
    }

    @Override
    public boolean insertRole(SysRoleDTO dto) {
        accessPolicy().assertRoleManagementAllowed();
        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        role.setTenantId(accessPolicy().resolveWriteTenantId(dto.getTenantId()));
        role.setOrgScopeType(resolveOrgScopeType(dto.getOrgScopeType()));
        accessPolicy().validateDataScopeAllowedForCurrentUser(role.getDataScope());
        boolean inserted = TenantContextHolder.executeIgnore(() -> roleMapper.insert(role) > 0);
        if (inserted && dto.getOrgIds() != null) {
            bindRoleOrgs(role.getId(), dto.getOrgIds());
        }
        return inserted;
    }

    @Override
    public boolean updateRole(SysRoleDTO dto) {
        SysRole existing = accessPolicy().loadRoleForAccess(dto.getId());
        accessPolicy().assertCanMaintainRole(existing);
        SysRole role = new SysRole();
        BeanUtil.copyProperties(dto, role);
        role.setOrgScopeType(null);
        LoginUser loginUser = accessPolicy().requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = dto.getTenantId() != null ? dto.getTenantId() : existing.getTenantId();
            role.setTenantId(accessPolicy().resolveWriteTenantId(tenantId));
        } else {
            role.setTenantId(null);
        }
        Integer nextDataScope = dto.getDataScope() != null ? dto.getDataScope() : existing.getDataScope();
        accessPolicy().validateDataScopeAllowedForCurrentUser(nextDataScope);
        accessPolicy().validateDataScopeAllowedForBoundUsers(existing, nextDataScope);
        boolean updated = TenantContextHolder.executeIgnore(() -> roleMapper.updateById(role) > 0);
        if (updated && dto.getOrgIds() != null) {
            bindRoleOrgs(existing.getId(), dto.getOrgIds());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleById(Long id) {
        SysRole role = accessPolicy().loadRoleForAccess(id);
        accessPolicy().assertCanMaintainRole(role);
        accessPolicy().validateRoleDeletable(role);
        return TenantContextHolder.executeIgnore(() -> {
            roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>()
                    .eq(SysRoleResource::getRoleId, id)
                    .eq(SysRoleResource::getTenantId, role.getTenantId()));
            roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                    .eq(SysRoleOrg::getRoleId, id)
                    .eq(SysRoleOrg::getTenantId, role.getTenantId()));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getRoleId, id)
                    .eq(SysUserOrgRole::getTenantId, role.getTenantId()));
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, id)
                    .eq(SysUserRole::getTenantId, role.getTenantId()));
            return roleMapper.deleteById(id) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return false;
        }
        for (Long id : ids) {
            deleteRoleById(id);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleResources(Long roleId, Long[] resourceIds) {
        return bindRoleResources(roleId, resourceIds, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleResources(Long roleId, Long[] resourceIds, String clientCode) {
        if (roleId == null) {
            return false;
        }

        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);

        List<SysResource> assignableResources;
        Set<Long> resourceIdSet = resourceIds != null ? new HashSet<>(Arrays.asList(resourceIds)) : Collections.emptySet();
        if (StringUtils.isNotBlank(clientCode)) {
            assignableResources = new ArrayList<>(resourceService.lambdaQuery()
                    .and(wrapper -> wrapper
                            .eq(SysResource::getClientCode, clientCode)
                            .or()
                            .isNull(SysResource::getClientCode)
                            .or()
                            .eq(SysResource::getClientCode, ""))
                    .list());
            if (!resourceIdSet.isEmpty()) {
                Set<Long> existingIds = assignableResources.stream()
                        .map(SysResource::getId)
                        .collect(Collectors.toSet());
                Set<Long> missingIds = new HashSet<>(resourceIdSet);
                missingIds.removeAll(existingIds);
                if (!missingIds.isEmpty()) {
                    for (SysResource ancestor : loadResourcesWithAncestors(missingIds)) {
                        if (existingIds.add(ancestor.getId())) {
                            assignableResources.add(ancestor);
                        }
                    }
                }
            }
        } else {
            assignableResources = resourceIdSet.isEmpty()
                    ? Collections.emptyList()
                    : loadResourcesWithAncestors(resourceIdSet);
        }
        Map<Long, SysResource> resourceMap = assignableResources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource, (left, right) -> left));
        Set<Long> clientResourceIdSet = Collections.emptySet();
        Set<Long> clientAssignableResourceIdSet = Collections.emptySet();
        if (StringUtils.isNotBlank(clientCode)) {
            clientResourceIdSet = assignableResources.stream()
                    .filter(resource -> isClientCompatibleResource(resource, clientCode))
                    .map(SysResource::getId)
                    .collect(Collectors.toSet());
            clientAssignableResourceIdSet = new HashSet<>(clientResourceIdSet);
            if (resourceIds != null) {
                for (Long resourceId : resourceIds) {
                    if (!clientResourceIdSet.contains(resourceId)) {
                        throw new RuntimeException("权限溢出：不能分配其他客户端的资源权限");
                    }
                }
            }
        }

        // 防止权限溢出校验：非管理员只能分配自己拥有的资源
        LoginUser loginUser = SessionHelper.getLoginUser();
        Set<Long> currentUserResourceIdSet = Collections.emptySet();
        if (loginUser != null && !loginUser.isAdmin()) {
            List<Long> currentUserResourceIds = resourceService.selectCurrentUserResourceIds();
            currentUserResourceIdSet = new HashSet<>(currentUserResourceIds);
            if (resourceIds != null) {
                for (Long resourceId : resourceIds) {
                    if (!currentUserResourceIdSet.contains(resourceId)) {
                        throw new RuntimeException("权限溢出：不能分配自己没有的资源权限");
                    }
                }
            }
        }

        // 核心修复：自动补充所有父级ID，确保菜单树渲染完整
        Set<Long> finalResourceIdSet = new HashSet<>();
        if (resourceIds != null && resourceIds.length > 0) {
            finalResourceIdSet.addAll(Arrays.asList(resourceIds));

            // 自动补齐父级时使用全量资源构建父链。历史数据中可能存在父级 client_code 为空，
            // 这类父级只作为当前客户端树的结构节点补齐；真正属于其他客户端的父级仍然拦截。
            Set<Long> parentIdsToAdd = new HashSet<>();
            for (Long id : finalResourceIdSet) {
                SysResource resource = resourceMap.get(id);
                Long pid = resource == null ? null : normalizeParentId(resource.getParentId());
                while (pid != null && pid != 0L) {
                    if (finalResourceIdSet.contains(pid) || parentIdsToAdd.contains(pid)) {
                        break;
                    }
                    SysResource parentResource = resourceMap.get(pid);
                    if (parentResource == null) {
                        break;
                    }
                    if (StringUtils.isNotBlank(clientCode) && !isClientCompatibleParent(parentResource, clientCode)) {
                        throw new RuntimeException("权限溢出：不能分配其他客户端的父级资源权限");
                    }
                    parentIdsToAdd.add(pid);
                    if (StringUtils.isNotBlank(clientCode)) {
                        clientAssignableResourceIdSet.add(pid);
                    }
                    pid = normalizeParentId(parentResource.getParentId());
                }
            }
            finalResourceIdSet.addAll(parentIdsToAdd);
        }

        if (loginUser != null && !loginUser.isAdmin() && !currentUserResourceIdSet.containsAll(finalResourceIdSet)) {
            throw new RuntimeException("权限溢出：不能分配自己没有的父级资源权限");
        }
        if (StringUtils.isNotBlank(clientCode) && !clientAssignableResourceIdSet.containsAll(finalResourceIdSet)) {
            throw new RuntimeException("权限溢出：不能分配其他客户端的父级资源权限");
        }

        // 存储前归一化：目录和页面父链只为明确勾选的页面入口保留。
        // 授权弹窗会把角色历史绑定（含其下页面已全部取消的"孤儿目录"）回填后原样提交，
        // 全量替换会把这些目录反复写回，导致角色出现点开为空的菜单入口（如已废弃模块）。
        finalResourceIdSet = RoleResourceSelectionNormalizer.normalize(
                finalResourceIdSet, resourceIdSet, resourceMap);

        // 1. 先删除该角色的资源关联。指定客户端时仅替换当前客户端资源，避免清空其他客户端权限。
        LambdaQueryWrapper<SysRoleResource> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleResource::getRoleId, roleId)
                .eq(SysRoleResource::getTenantId, role.getTenantId());
        if (StringUtils.isNotBlank(clientCode)) {
            if (clientAssignableResourceIdSet.isEmpty()) {
                return true;
            }
            deleteWrapper.in(SysRoleResource::getResourceId, clientAssignableResourceIdSet);
        }
        roleResourceMapper.delete(deleteWrapper);

        // 2. 如果没有新的资源ID，直接返回（表示清空所有权限）
        if (finalResourceIdSet.isEmpty()) {
            return true;
        }

        // 3. 批量插入新的角色资源关联
        List<SysRoleResource> roleResources = new ArrayList<>();
        for (Long resourceId : finalResourceIdSet) {
            SysRoleResource roleResource = new SysRoleResource();
            roleResource.setTenantId(role.getTenantId());
            roleResource.setRoleId(roleId);
            roleResource.setResourceId(resourceId);
            roleResources.add(roleResource);
        }

        if (!roleResources.isEmpty()) {
            roleResourceMapper.insertBatch(roleResources);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveScopedRolePermissions(Long roleId, ScopedRolePermissionDTO settings) {
        if (settings == null) {
            throw new RuntimeException("范围化角色权限不能为空");
        }
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);

        Set<Long> scopeResourceIds = normalizeIds(settings.getScopeResourceIds());
        Set<Long> selectedResourceIds = normalizeIds(settings.getSelectedResourceIds());
        if (!scopeResourceIds.containsAll(selectedResourceIds)) {
            throw new RuntimeException("权限溢出：选择了授权范围以外的资源");
        }

        Set<Long> resourceIdsToLoad = new LinkedHashSet<>(scopeResourceIds);
        resourceIdsToLoad.addAll(selectedResourceIds);
        List<SysResource> resources = loadResourcesWithAncestors(resourceIdsToLoad);
        Map<Long, SysResource> resourceMap = resources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource, (left, right) -> left));
        if (!resourceMap.keySet().containsAll(scopeResourceIds)) {
            throw new RuntimeException("授权范围包含不存在的系统资源");
        }
        validateScopedResources(settings.getClientCode(), scopeResourceIds, resourceMap);

        Set<Long> finalSelectedResourceIds = withResourceAncestors(
                selectedResourceIds, resourceMap, settings.getClientCode());
        validateCurrentUserCanAssign(finalSelectedResourceIds);

        Set<String> scopeModuleCodes = normalizeModuleCodes(settings.getScopeModuleCodes());
        Map<String, Integer> moduleScopes = normalizeScopedModuleDataScopes(
                settings.getModuleScopes(), scopeModuleCodes, role);

        return TenantContextHolder.executeIgnore(() -> {
            List<Long> currentResourceIds = roleResourceMapper.selectResourceIdsByRole(
                    role.getTenantId(), role.getId());
            Set<Long> retainedResourceIds = new HashSet<>(currentResourceIds);
            retainedResourceIds.removeAll(scopeResourceIds);

            if (!scopeResourceIds.isEmpty()) {
                roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>()
                        .eq(SysRoleResource::getTenantId, role.getTenantId())
                        .eq(SysRoleResource::getRoleId, role.getId())
                        .in(SysRoleResource::getResourceId, scopeResourceIds));
            }
            List<SysRoleResource> resourceBindings = finalSelectedResourceIds.stream()
                    .filter(resourceId -> !retainedResourceIds.contains(resourceId))
                    .map(resourceId -> roleResource(role, resourceId))
                    .toList();
            if (!resourceBindings.isEmpty()) {
                roleResourceMapper.insertBatch(resourceBindings);
            }

            if (!scopeModuleCodes.isEmpty()) {
                roleModuleDataScopeMapper.deleteByRoleAndModules(
                        role.getTenantId(), role.getId(), scopeModuleCodes);
            }
            List<SysRoleModuleDataScope> dataScopeBindings = moduleScopes.entrySet().stream()
                    .map(entry -> roleModuleDataScope(role, entry.getKey(), entry.getValue()))
                    .toList();
            if (!dataScopeBindings.isEmpty()) {
                roleModuleDataScopeMapper.insertBatch(dataScopeBindings);
            }
            return true;
        }) && refreshDataScopeCacheAfterScopedSave();
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private boolean isClientCompatibleParent(SysResource resource, String clientCode) {
        return isClientCompatibleResource(resource, clientCode);
    }

    private boolean isClientCompatibleResource(SysResource resource, String clientCode) {
        return resource != null
                && (StringUtils.isBlank(resource.getClientCode()) || clientCode.equals(resource.getClientCode()));
    }

    private List<SysResource> loadResourcesWithAncestors(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysResource> resourceMap = new HashMap<>();
        Set<Long> pendingIds = new HashSet<>(ids);
        while (!pendingIds.isEmpty()) {
            List<SysResource> batch = resourceService.listByIds(pendingIds);
            pendingIds.clear();
            for (SysResource resource : batch) {
                if (resourceMap.put(resource.getId(), resource) == null) {
                    Long pid = normalizeParentId(resource.getParentId());
                    if (pid != 0L && !resourceMap.containsKey(pid)) {
                        pendingIds.add(pid);
                    }
                }
            }
        }
        return new ArrayList<>(resourceMap.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindRoleResources(Long roleId, Long[] resourceIds) {
        if (roleId == null || resourceIds == null || resourceIds.length == 0) {
            return false;
        }
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);

        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getRoleId, roleId)
                .eq(SysRoleResource::getTenantId, role.getTenantId())
                .in(SysRoleResource::getResourceId, Arrays.asList(resourceIds));
        return TenantContextHolder.executeIgnore(() -> roleResourceMapper.delete(wrapper) > 0);
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId) {
        return selectRoleResourceIds(roleId, null);
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId, String clientCode) {
        return selectRoleResourceIds(roleId, clientCode, false);
    }

    @Override
    public List<Long> selectRoleResourceIds(Long roleId, String clientCode, boolean includeParents) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        SysRole role = accessPolicy().loadRoleForAccess(roleId);

        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleResource::getRoleId, roleId)
                .eq(SysRoleResource::getTenantId, role.getTenantId())
                .select(SysRoleResource::getResourceId);
        List<Long> resourceIds = roleResourceMapper.selectList(wrapper)
                .stream()
                .map(SysRoleResource::getResourceId)
                .collect(Collectors.toList());

        if (StringUtils.isNotBlank(clientCode) && CollUtil.isNotEmpty(resourceIds)) {
            Set<Long> clientResourceIdSet = resourceService.lambdaQuery()
                    .and(clientScope -> clientScope
                            .eq(SysResource::getClientCode, clientCode)
                            .or()
                            .isNull(SysResource::getClientCode)
                            .or()
                            .eq(SysResource::getClientCode, ""))
                    .select(SysResource::getId)
                    .list()
                    .stream()
                    .map(SysResource::getId)
                    .collect(Collectors.toSet());
            resourceIds = resourceIds.stream()
                    .filter(clientResourceIdSet::contains)
                    .collect(Collectors.toList());
        }

        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser != null && !loginUser.isAdmin() && CollUtil.isNotEmpty(resourceIds)) {
            Set<Long> currentUserResourceIdSet = new HashSet<>(resourceService.selectCurrentUserResourceIds());
            resourceIds = resourceIds.stream()
                    .filter(currentUserResourceIdSet::contains)
                    .collect(Collectors.toList());
        }

        if (includeParents) {
            return resourceIds;
        }

        // 优化：过滤掉父级ID，只返回叶子节点（在当前选中集合中没有子节点的节点）
        // 这样可以适配前端树组件的 cascade 模式，防止因父节点存在导致子节点全选
        if (CollUtil.isNotEmpty(resourceIds)) {
            List<SysResource> selectedResources = resourceService.listByIds(resourceIds);
            Set<Long> selectedParentIds = selectedResources.stream()
                .map(SysResource::getParentId)
                .filter(pid -> pid != null && pid != 0L)
                .collect(Collectors.toSet());

            return resourceIds.stream()
                .filter(id -> !selectedParentIds.contains(id))
                .collect(Collectors.toList());
        }

        return resourceIds;
    }

    @Override
    public List<Long> selectRoleOrgIds(Long roleId) {
        if (roleId == null) {
            return new ArrayList<>();
        }
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        return TenantContextHolder.executeIgnore(() -> roleOrgMapper.selectList(new LambdaQueryWrapper<SysRoleOrg>()
                        .eq(SysRoleOrg::getTenantId, role.getTenantId())
                        .eq(SysRoleOrg::getRoleId, roleId)
                        .select(SysRoleOrg::getOrgId)))
                .stream()
                .map(SysRoleOrg::getOrgId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindRoleOrgs(Long roleId, List<Long> orgIds) {
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);
        List<Long> normalizedOrgIds = normalizeOrgIds(orgIds);

        List<Long> existingOrgIds = selectRoleOrgIds(roleId);
        if (normalizedOrgIds.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                    .eq(SysRoleOrg::getTenantId, role.getTenantId())
                    .eq(SysRoleOrg::getRoleId, roleId)));
            updateRoleOrgScopeType(role, SystemConstants.RoleOrgScope.GLOBAL);
            return true;
        }

        accessPolicy().validateOrgTenant(normalizedOrgIds, role.getTenantId());
        updateRoleOrgScopeType(role, SystemConstants.RoleOrgScope.CUSTOM);

        List<Long> removedOrgIds = existingOrgIds.stream()
                .filter(orgId -> !normalizedOrgIds.contains(orgId))
                .collect(Collectors.toList());
        if (!removedOrgIds.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> {
                userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                        .eq(SysUserOrgRole::getTenantId, role.getTenantId())
                        .eq(SysUserOrgRole::getRoleId, roleId)
                        .in(SysUserOrgRole::getOrgId, removedOrgIds));
                roleOrgMapper.delete(new LambdaQueryWrapper<SysRoleOrg>()
                        .eq(SysRoleOrg::getTenantId, role.getTenantId())
                        .eq(SysRoleOrg::getRoleId, roleId)
                        .in(SysRoleOrg::getOrgId, removedOrgIds));
                return true;
            });
        }

        Set<Long> existingOrgIdSet = new HashSet<>(existingOrgIds);
        TenantContextHolder.executeIgnore(() -> {
            for (Long orgId : normalizedOrgIds) {
                if (existingOrgIdSet.contains(orgId)) {
                    continue;
                }
                SysRoleOrg roleOrg = new SysRoleOrg();
                roleOrg.setTenantId(role.getTenantId());
                roleOrg.setRoleId(roleId);
                roleOrg.setOrgId(orgId);
                roleOrgMapper.insert(roleOrg);
            }
            return true;
        });
        return true;
    }

    private Integer resolveOrgScopeType(Integer orgScopeType) {
        return Objects.equals(orgScopeType, SystemConstants.RoleOrgScope.CUSTOM)
                ? SystemConstants.RoleOrgScope.CUSTOM
                : SystemConstants.RoleOrgScope.GLOBAL;
    }

    private void updateRoleOrgScopeType(SysRole role, Integer orgScopeType) {
        SysRole update = new SysRole();
        update.setId(role.getId());
        update.setTenantId(role.getTenantId());
        update.setOrgScopeType(resolveOrgScopeType(orgScopeType));
        TenantContextHolder.executeIgnore(() -> {
            roleMapper.updateById(update);
        });
        role.setOrgScopeType(update.getOrgScopeType());
    }

    @Override
    public List<Long> selectCurrentUserRoleIds() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return new ArrayList<>();
        }
        if (loginUser.isAdmin()) {
            return TenantContextHolder.executeIgnore(() -> roleMapper.selectList(new LambdaQueryWrapper<SysRole>().select(SysRole::getId)))
                    .stream().map(SysRole::getId).collect(Collectors.toList());
        }
        return loginUser.getRoleIds();
    }

    @Override
    public RoleDataScopeSettingsVO getRoleDataScopeSettings(Long roleId) {
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        List<SysDataScopeConfig> configs = listEnabledDataScopeConfigs(role.getTenantId());
        List<SysRoleModuleDataScope> overrides = listRoleModuleDataScopes(role);
        Map<String, Integer> overrideMap = overrides.stream()
                .collect(Collectors.toMap(SysRoleModuleDataScope::getModuleCode,
                        SysRoleModuleDataScope::getDataScope, (left, right) -> right));

        List<SysResource> resources = TenantContextHolder.executeIgnore(() ->
                resourceMapper.selectModuleNamingResources(role.getTenantId()));
        Map<Long, SysResource> resourceMap = resources.stream()
                .collect(Collectors.toMap(SysResource::getId, resource -> resource, (left, right) -> left));

        Map<String, List<SysDataScopeConfig>> groupedConfigs = configs.stream()
                .filter(config -> StringUtils.isNotBlank(config.getResourceCode()))
                .collect(Collectors.groupingBy(SysDataScopeConfig::getResourceCode,
                        LinkedHashMap::new, Collectors.toList()));

        List<RoleModuleDataScopeVO> modules = groupedConfigs.entrySet().stream()
                .map(entry -> buildModuleDataScope(entry.getKey(), entry.getValue(), role.getDataScope(),
                        overrideMap.get(entry.getKey()), resources, resourceMap))
                .sorted(Comparator.comparing(RoleModuleDataScopeVO::getModuleName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        RoleDataScopeSettingsVO result = new RoleDataScopeSettingsVO();
        result.setDefaultDataScope(role.getDataScope());
        result.setModules(modules);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleDataScopeSettings(Long roleId, RoleDataScopeSettingsDTO settings) {
        if (settings == null) {
            throw new RuntimeException("数据权限设置不能为空");
        }
        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);
        Integer defaultDataScope = settings.getDefaultDataScope();
        accessPolicy().validateSupportedDataScope(defaultDataScope);
        accessPolicy().validateDataScopeAllowedForCurrentUser(defaultDataScope);
        accessPolicy().validateDataScopeAllowedForBoundUsers(role, defaultDataScope);

        Set<String> availableModuleCodes = listEnabledDataScopeConfigs(role.getTenantId()).stream()
                .map(SysDataScopeConfig::getResourceCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, Integer> requestedOverrides = normalizeModuleDataScopes(
                settings.getModuleScopes(), availableModuleCodes, role);

        boolean saved = TenantContextHolder.executeIgnore(() -> {
            SysRole update = new SysRole();
            update.setId(role.getId());
            update.setTenantId(role.getTenantId());
            update.setDataScope(defaultDataScope);
            if (roleMapper.updateById(update) <= 0) {
                return false;
            }

            if (!availableModuleCodes.isEmpty()) {
                roleModuleDataScopeMapper.deleteByRoleAndModules(
                        role.getTenantId(), role.getId(), availableModuleCodes);
            }

            requestedOverrides.forEach((moduleCode, dataScope) -> {
                SysRoleModuleDataScope override = new SysRoleModuleDataScope();
                override.setTenantId(role.getTenantId());
                override.setRoleId(role.getId());
                override.setModuleCode(moduleCode);
                override.setDataScope(dataScope);
                roleModuleDataScopeMapper.insert(override);
            });
            return true;
        });
        if (saved) {
            refreshDataScopeCacheAfterCommit();
        }
        return saved;
    }

    private void refreshDataScopeCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dataScopeService.refreshDataScopeCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dataScopeService.refreshDataScopeCache();
            }
        });
    }

    private boolean refreshDataScopeCacheAfterScopedSave() {
        refreshDataScopeCacheAfterCommit();
        return true;
    }

    private Set<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return new LinkedHashSet<>();
        }
        return ids.stream().filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeModuleCodes(Collection<String> moduleCodes) {
        if (moduleCodes == null) {
            return new LinkedHashSet<>();
        }
        return moduleCodes.stream().map(StringUtils::trimToNull).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateScopedResources(String clientCode,
                                         Set<Long> scopeResourceIds,
                                         Map<Long, SysResource> resourceMap) {
        if (StringUtils.isNotBlank(clientCode)) {
            boolean incompatible = scopeResourceIds.stream()
                    .map(resourceMap::get)
                    .anyMatch(resource -> !isClientCompatibleResource(resource, clientCode));
            if (incompatible) {
                throw new RuntimeException("权限溢出：授权范围包含其他客户端资源");
            }
        }
        validateCurrentUserCanAssign(scopeResourceIds);
    }

    private void validateCurrentUserCanAssign(Set<Long> resourceIds) {
        LoginUser loginUser = accessPolicy().requireLoginUser();
        if (loginUser.isAdmin() || resourceIds.isEmpty()) {
            return;
        }
        Set<Long> currentUserResourceIds = new HashSet<>(resourceService.selectCurrentUserResourceIds());
        if (!currentUserResourceIds.containsAll(resourceIds)) {
            throw new RuntimeException("权限溢出：不能分配自己没有的资源权限");
        }
    }

    private Set<Long> withResourceAncestors(Set<Long> selectedResourceIds,
                                            Map<Long, SysResource> resourceMap,
                                            String clientCode) {
        Set<Long> result = new LinkedHashSet<>(selectedResourceIds);
        for (Long resourceId : selectedResourceIds) {
            SysResource resource = resourceMap.get(resourceId);
            Long parentId = resource == null ? null : normalizeParentId(resource.getParentId());
            while (parentId != null && parentId != 0L && result.add(parentId)) {
                SysResource parent = resourceMap.get(parentId);
                if (parent == null) {
                    break;
                }
                if (StringUtils.isNotBlank(clientCode) && !isClientCompatibleParent(parent, clientCode)) {
                    throw new RuntimeException("权限溢出：不能分配其他客户端的父级资源权限");
                }
                parentId = normalizeParentId(parent.getParentId());
            }
        }
        return RoleResourceSelectionNormalizer.normalize(result, selectedResourceIds, resourceMap);
    }

    private Map<String, Integer> normalizeScopedModuleDataScopes(
            List<RoleModuleDataScopeDTO> moduleScopes,
            Set<String> scopeModuleCodes,
            SysRole role) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (moduleScopes == null) {
            return result;
        }
        for (RoleModuleDataScopeDTO moduleScope : moduleScopes) {
            if (moduleScope == null || StringUtils.isBlank(moduleScope.getModuleCode())) {
                throw new RuntimeException("业务模块编码不能为空");
            }
            String moduleCode = moduleScope.getModuleCode().trim();
            if (!scopeModuleCodes.contains(moduleCode)) {
                throw new RuntimeException("业务模块不属于当前授权范围：" + moduleCode);
            }
            if (result.containsKey(moduleCode)) {
                throw new RuntimeException("业务模块重复配置：" + moduleCode);
            }
            Integer dataScope = moduleScope.getDataScope();
            if (dataScope == null) {
                continue;
            }
            accessPolicy().validateSupportedDataScope(dataScope);
            accessPolicy().validateDataScopeAllowedForCurrentUser(dataScope);
            result.put(moduleCode, dataScope);
        }
        return result;
    }

    private SysRoleResource roleResource(SysRole role, Long resourceId) {
        SysRoleResource binding = new SysRoleResource();
        binding.setTenantId(role.getTenantId());
        binding.setRoleId(role.getId());
        binding.setResourceId(resourceId);
        return binding;
    }

    private SysRoleModuleDataScope roleModuleDataScope(SysRole role, String moduleCode, Integer dataScope) {
        SysRoleModuleDataScope binding = new SysRoleModuleDataScope();
        binding.setTenantId(role.getTenantId());
        binding.setRoleId(role.getId());
        binding.setModuleCode(moduleCode);
        binding.setDataScope(dataScope);
        return binding;
    }

    @Override
    public IPage<SysUser> selectRoleUsers(RoleUserQuery query) {
        if (query.getRoleId() == null) {
            return new Page<>();
        }
        SysRole role = accessPolicy().loadRoleForAccess(query.getRoleId());
        query.setTenantId(role.getTenantId());
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return TenantContextHolder.executeIgnore(() -> roleMapper.selectRoleUsers(page, query));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserRole(Long roleId, Long userId) {
        if (roleId == null || userId == null) {
            return false;
        }

        SysRole role = accessPolicy().loadRoleForAccess(roleId);
        accessPolicy().assertCanMaintainRole(role);
        return TenantContextHolder.executeIgnore(() -> {
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getRoleId, roleId)
                    .eq(SysUserOrgRole::getTenantId, role.getTenantId())
                    .eq(SysUserOrgRole::getUserId, userId));
            return userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, roleId)
                    .eq(SysUserRole::getTenantId, role.getTenantId())
                    .eq(SysUserRole::getUserId, userId)) > 0;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUsersToRole(Long roleId, List<Long> userIds) {
        throw new RuntimeException("旧角色加人接口已废弃，请选择授权组织后使用组织角色授权");
    }

    private SysRoleAccessPolicy accessPolicy() {
        return new SysRoleAccessPolicy(roleMapper, tenantMapper, orgMapper);
    }

    private List<SysDataScopeConfig> listEnabledDataScopeConfigs(Long tenantId) {
        return TenantContextHolder.executeIgnore(() ->
                dataScopeConfigMapper.selectEnabledModuleConfigs(tenantId));
    }

    private List<SysRoleModuleDataScope> listRoleModuleDataScopes(SysRole role) {
        return TenantContextHolder.executeIgnore(() ->
                roleModuleDataScopeMapper.selectByRole(role.getTenantId(), role.getId()));
    }

    private RoleModuleDataScopeVO buildModuleDataScope(String moduleCode,
                                                       List<SysDataScopeConfig> configs,
                                                       Integer defaultDataScope,
                                                       Integer overrideDataScope,
                                                       List<SysResource> resources,
                                                       Map<Long, SysResource> resourceMap) {
        RoleModuleDataScopeVO module = new RoleModuleDataScopeVO();
        module.setModuleCode(moduleCode);
        module.setModuleName(resolveModuleName(moduleCode, configs, resources, resourceMap));
        module.setRuleCount(configs.size());
        module.setDataScope(overrideDataScope);
        module.setEffectiveDataScope(overrideDataScope != null ? overrideDataScope : defaultDataScope);
        return module;
    }

    private String resolveModuleName(String moduleCode,
                                     List<SysDataScopeConfig> configs,
                                     List<SysResource> resources,
                                     Map<Long, SysResource> resourceMap) {
        Map<String, Long> menuNameCounts = resources.stream()
                .filter(resource -> resourceMatchesModule(resource, moduleCode))
                .map(resource -> findMenuAncestor(resource, resourceMap))
                .filter(Objects::nonNull)
                .map(SysResource::getResourceName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        if (!menuNameCounts.isEmpty()) {
            return menuNameCounts.entrySet().stream()
                    .max(Map.Entry.<String, Long>comparingByValue()
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey)
                    .orElse(moduleCode);
        }
        return configs.stream()
                .map(SysDataScopeConfig::getResourceName)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(moduleCode);
    }

    private boolean resourceMatchesModule(SysResource resource, String moduleCode) {
        String permission = resource.getPerms();
        return StringUtils.isNotBlank(permission)
                && (permission.equals(moduleCode) || permission.startsWith(moduleCode + ":"));
    }

    private SysResource findMenuAncestor(SysResource resource, Map<Long, SysResource> resourceMap) {
        SysResource current = resource;
        Set<Long> visited = new HashSet<>();
        while (current != null && current.getId() != null && visited.add(current.getId())) {
            if (Integer.valueOf(2).equals(current.getResourceType())) {
                return current;
            }
            current = resourceMap.get(current.getParentId());
        }
        return null;
    }

    private Map<String, Integer> normalizeModuleDataScopes(List<RoleModuleDataScopeDTO> moduleScopes,
                                                           Set<String> availableModuleCodes,
                                                           SysRole role) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (moduleScopes == null) {
            return result;
        }
        Set<String> seenModuleCodes = new HashSet<>();
        for (RoleModuleDataScopeDTO moduleScope : moduleScopes) {
            if (moduleScope == null || StringUtils.isBlank(moduleScope.getModuleCode())) {
                throw new RuntimeException("业务模块编码不能为空");
            }
            String moduleCode = moduleScope.getModuleCode().trim();
            if (!availableModuleCodes.contains(moduleCode)) {
                throw new RuntimeException("业务模块不存在或未启用：" + moduleCode);
            }
            if (!seenModuleCodes.add(moduleCode)) {
                throw new RuntimeException("业务模块重复配置：" + moduleCode);
            }
            Integer dataScope = moduleScope.getDataScope();
            if (dataScope == null) {
                continue;
            }
            accessPolicy().validateSupportedDataScope(dataScope);
            accessPolicy().validateDataScopeAllowedForCurrentUser(dataScope);
            result.put(moduleCode, dataScope);
        }
        return result;
    }

    private List<Long> normalizeOrgIds(List<Long> orgIds) {
        if (orgIds == null) {
            return new ArrayList<>();
        }
        return orgIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

}
