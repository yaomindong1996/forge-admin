package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.SysResourceDTO;
import com.mdframe.forge.plugin.system.dto.SysResourceQuery;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.entity.SysRoleResource;
import com.mdframe.forge.plugin.system.mapper.SysResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleResourceMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.auth.domain.UserResourceTreeVO;
import com.mdframe.forge.starter.auth.service.IMenuService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资源Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements ISysResourceService, IMenuService {

    private final SysResourceMapper resourceMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final PermissionServiceImpl permissionService;

    @Override
    public boolean save(SysResource entity) {
        boolean saved = super.save(entity);
        clearApiPermissionCacheIfChanged(saved);
        return saved;
    }

    @Override
    public boolean updateById(SysResource entity) {
        boolean updated = super.updateById(entity);
        clearApiPermissionCacheIfChanged(updated);
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        clearApiPermissionCacheIfChanged(removed);
        return removed;
    }

    @Override
    public IPage<SysResource> selectResourcePage(SysResourceQuery query) {
        assertSystemAdmin();
        LambdaQueryWrapper<SysResource> wrapper = buildQueryWrapper(query);
        Page<SysResource> page = new Page<>(query.getPageNum(), query.getPageSize());
        return resourceMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysResource> selectResourceTree(SysResourceQuery query) {
        assertSystemAdmin();
        List<SysResource> list = list(buildQueryWrapper(query));
        return buildEntityTree(list, 0L);
    }

    @Override
    public List<SysResource> selectAssignableResourceTree(SysResourceQuery query) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        LambdaQueryWrapper<SysResource> wrapper = buildQueryWrapper(query);
        // 授权树剔除「彻底停用」的资源（visible=0 且 menu_status=0，如废弃目录、demo 菜单）；
        // 仅 visible=0 的隐藏功能页（menu_status=1，如应用工作台/运行页/详情跳转页）必须保留，
        // 否则角色无法勾选其入口权限，授权后访问会被拦 403（visible 不得一刀切进权限链路）。
        wrapper.and(w -> w.ne(SysResource::getVisible, 0).or().ne(SysResource::getMenuStatus, 0));
        applyUserTypeScope(wrapper, loginUser);
        if (!loginUser.isAdmin()) {
            List<Long> resourceIds = selectCurrentUserResourceIds();
            if (CollUtil.isEmpty(resourceIds)) {
                return new ArrayList<>();
            }
            wrapper.in(SysResource::getId, resourceIds);
        }

        List<SysResource> list = list(wrapper);
        return buildEntityTree(list, 0L);
    }

    @Override
    public SysResource selectResourceById(Long id) {
        assertSystemAdmin();
        return resourceMapper.selectById(id);
    }

    @Override
    public boolean insertResource(SysResourceDTO dto) {
        assertSystemAdmin();
        SysResource resource = new SysResource();
        BeanUtil.copyProperties(dto, resource);
        resource.setMinUserType(normalizeMinUserType(resource.getMinUserType()));
        validateParentUserTypeBoundary(resource.getParentId(), resource.getMinUserType());
        boolean inserted = resourceMapper.insert(resource) > 0;
        clearApiPermissionCacheIfChanged(inserted);
        return inserted;
    }

    @Override
    public boolean updateResource(SysResourceDTO dto) {
        assertSystemAdmin();
        SysResource existing = resourceMapper.selectById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("资源不存在");
        }
        SysResource resource = new SysResource();
        BeanUtil.copyProperties(dto, resource);
        Integer minUserType = dto.getMinUserType() != null
                ? normalizeMinUserType(dto.getMinUserType())
                : normalizeMinUserType(existing.getMinUserType());
        resource.setMinUserType(dto.getMinUserType() != null ? minUserType : null);
        Long parentId = dto.getParentId() != null ? dto.getParentId() : existing.getParentId();
        validateParentCycle(dto.getId(), parentId);
        validateParentUserTypeBoundary(parentId, minUserType);
        boolean updated = resourceMapper.updateById(resource) > 0;
        clearApiPermissionCacheIfChanged(updated);
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteResourceById(Long id) {
        return deleteResourceByIds(id == null ? new Long[0] : new Long[]{id});
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteResourceByIds(Long[] ids) {
        assertSystemAdmin();
        List<Long> resourceIds = normalizeResourceIds(ids == null ? Collections.emptyList() : Arrays.asList(ids));
        if (CollUtil.isEmpty(resourceIds)) {
            throw new RuntimeException("请选择要删除的资源");
        }

        List<SysResource> selectedResources = loadSelectedResources(resourceIds);
        validateSelectedResourcesExist(resourceIds, selectedResources);
        validateNoUnselectedDescendants(resourceIds);
        deleteRoleResourceBindings(resourceIds);

        int deletedCount = resourceMapper.deleteBatchIds(resourceIds);
        if (deletedCount != resourceIds.size()) {
            throw new RuntimeException("部分资源删除失败，请刷新后重试");
        }
        boolean deleted = deletedCount > 0;
        clearApiPermissionCacheIfChanged(deleted);
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchMigrateResources(List<Long> ids, Long parentId) {
        assertSystemAdmin();
        List<Long> resourceIds = normalizeResourceIds(ids);
        if (CollUtil.isEmpty(resourceIds)) {
            throw new RuntimeException("请选择要迁移的资源");
        }

        Long targetParentId = normalizeParentId(parentId);
        List<SysResource> allResources = resourceMapper.selectList(new LambdaQueryWrapper<SysResource>()
                .select(SysResource::getId, SysResource::getParentId, SysResource::getResourceName, SysResource::getMinUserType));
        Map<Long, SysResource> resourceMap = allResources.stream()
                .collect(Collectors.toMap(SysResource::getId, item -> item, (left, right) -> left));

        List<SysResource> selectedResources = resourceIds.stream()
                .map(resourceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        validateSelectedResourcesExist(resourceIds, selectedResources);
        validateBatchMigrateTarget(resourceIds, targetParentId, allResources, resourceMap);

        Set<Long> selectedIdSet = new HashSet<>(resourceIds);
        List<SysResource> movableResources = selectedResources.stream()
                .filter(resource -> !hasSelectedAncestor(resource, selectedIdSet, resourceMap))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(movableResources)) {
            return true;
        }

        for (SysResource resource : movableResources) {
            validateParentUserTypeBoundary(targetParentId, normalizeMinUserType(resource.getMinUserType()));
        }

        List<SysResource> updateList = movableResources.stream()
                .map(resource -> {
                    SysResource update = new SysResource();
                    update.setId(resource.getId());
                    update.setParentId(targetParentId);
                    return update;
                })
                .collect(Collectors.toList());
        boolean updated = updateBatchById(updateList);
        clearApiPermissionCacheIfChanged(updated);
        return updated;
    }

    private List<Long> normalizeResourceIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<SysResource> loadSelectedResources(List<Long> resourceIds) {
        if (CollUtil.isEmpty(resourceIds)) {
            return new ArrayList<>();
        }
        return resourceMapper.selectList(new LambdaQueryWrapper<SysResource>().in(SysResource::getId, resourceIds));
    }

    private void validateSelectedResourcesExist(List<Long> resourceIds, List<SysResource> selectedResources) {
        Set<Long> existingIds = selectedResources.stream()
                .map(SysResource::getId)
                .collect(Collectors.toSet());
        if (!existingIds.containsAll(resourceIds)) {
            throw new RuntimeException("部分资源不存在或已被删除");
        }
    }

    private void validateNoUnselectedDescendants(List<Long> resourceIds) {
        Set<Long> selectedIdSet = new HashSet<>(resourceIds);
        List<SysResource> allResources = resourceMapper.selectList(new LambdaQueryWrapper<SysResource>()
                .select(SysResource::getId, SysResource::getParentId, SysResource::getResourceName));
        Map<Long, List<SysResource>> childrenMap = groupResourcesByParent(allResources);
        Map<Long, SysResource> resourceMap = allResources.stream()
                .collect(Collectors.toMap(SysResource::getId, item -> item, (left, right) -> left));
        List<String> blockers = new ArrayList<>();

        for (Long resourceId : resourceIds) {
            Set<Long> descendantIds = collectDescendantIds(resourceId, childrenMap);
            boolean hasUnselectedDescendant = descendantIds.stream().anyMatch(id -> !selectedIdSet.contains(id));
            if (hasUnselectedDescendant) {
                SysResource resource = resourceMap.get(resourceId);
                blockers.add(resource == null ? String.valueOf(resourceId) : resource.getResourceName());
            }
        }

        if (CollUtil.isNotEmpty(blockers)) {
            String resourceNames = blockers.stream().limit(3).collect(Collectors.joining("、"));
            throw new RuntimeException("资源「" + resourceNames + "」下还有未选择的子资源，请勾选整棵子树后再删除");
        }
    }

    private void deleteRoleResourceBindings(List<Long> resourceIds) {
        if (CollUtil.isEmpty(resourceIds)) {
            return;
        }
        TenantContextHolder.executeIgnore(() -> {
            LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(SysRoleResource::getResourceId, resourceIds);
            roleResourceMapper.delete(wrapper);
        });
    }

    private void validateBatchMigrateTarget(List<Long> resourceIds,
                                            Long targetParentId,
                                            List<SysResource> allResources,
                                            Map<Long, SysResource> resourceMap) {
        if (targetParentId == null || targetParentId == 0L) {
            return;
        }
        if (!resourceMap.containsKey(targetParentId)) {
            throw new RuntimeException("目标上级资源不存在");
        }

        Set<Long> selectedIdSet = new HashSet<>(resourceIds);
        if (selectedIdSet.contains(targetParentId)) {
            throw new RuntimeException("不能迁移到选中的资源自身下面");
        }

        Map<Long, List<SysResource>> childrenMap = groupResourcesByParent(allResources);
        for (Long resourceId : resourceIds) {
            if (collectDescendantIds(resourceId, childrenMap).contains(targetParentId)) {
                throw new RuntimeException("不能迁移到选中资源的下级资源下面");
            }
        }
    }

    private boolean hasSelectedAncestor(SysResource resource, Set<Long> selectedIdSet, Map<Long, SysResource> resourceMap) {
        Long parentId = normalizeParentId(resource.getParentId());
        Set<Long> visitedIds = new HashSet<>();
        while (parentId != null && parentId != 0L && visitedIds.add(parentId)) {
            if (selectedIdSet.contains(parentId)) {
                return true;
            }
            SysResource parent = resourceMap.get(parentId);
            if (parent == null) {
                break;
            }
            parentId = normalizeParentId(parent.getParentId());
        }
        return false;
    }

    private void validateParentCycle(Long resourceId, Long parentId) {
        if (resourceId == null || parentId == null || parentId == 0L) {
            return;
        }
        if (Objects.equals(resourceId, parentId)) {
            throw new RuntimeException("上级资源不能选择自身");
        }

        List<SysResource> allResources = resourceMapper.selectList(new LambdaQueryWrapper<SysResource>()
                .select(SysResource::getId, SysResource::getParentId));
        Map<Long, List<SysResource>> childrenMap = groupResourcesByParent(allResources);
        if (collectDescendantIds(resourceId, childrenMap).contains(parentId)) {
            throw new RuntimeException("上级资源不能选择当前资源的下级");
        }
    }

    private Map<Long, List<SysResource>> groupResourcesByParent(List<SysResource> resources) {
        if (CollUtil.isEmpty(resources)) {
            return Collections.emptyMap();
        }
        return resources.stream()
                .collect(Collectors.groupingBy(resource -> normalizeParentId(resource.getParentId())));
    }

    private Set<Long> collectDescendantIds(Long parentId, Map<Long, List<SysResource>> childrenMap) {
        Set<Long> descendantIds = new HashSet<>();
        collectDescendantIds(parentId, childrenMap, descendantIds);
        return descendantIds;
    }

    private void collectDescendantIds(Long parentId, Map<Long, List<SysResource>> childrenMap, Set<Long> descendantIds) {
        List<SysResource> children = childrenMap.getOrDefault(parentId, Collections.emptyList());
        for (SysResource child : children) {
            if (child.getId() != null && descendantIds.add(child.getId())) {
                collectDescendantIds(child.getId(), childrenMap, descendantIds);
            }
        }
    }

    @Override
    public List<UserResourceTreeVO> selectCurrentUserResourceTree() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<SysResource> userResources = getUserResources(loginUser);
        List<UserResourceTreeVO> voList = userResources.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return buildVOTree(voList, 0L);
    }

    @Override
    public List<UserResourceTreeVO> selectCurrentUserMenuTree() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        List<SysResource> userResources = getUserResources(loginUser);
        List<SysResource> menuResources = userResources.stream()
                .filter(resource -> resource.getResourceType() != null
                        && (resource.getResourceType() == 1 || resource.getResourceType() == 2))
                .collect(Collectors.toList());

        List<UserResourceTreeVO> voList = menuResources.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return buildVOTree(voList, 0L);
    }

    @Override
    public List<String> selectCurrentUserPermissions() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }

        if (loginUser.isAdmin()) {
            List<String> allPermissions = new ArrayList<>();
            allPermissions.add("*");
            allPermissions.add("*:*:*");
            return allPermissions;
        }

        List<SysResource> userResources = getUserResources(loginUser);
        return userResources.stream()
                .filter(resource -> resource.getResourceType() != null && resource.getResourceType() == 3)
                .filter(resource -> StrUtil.isNotBlank(resource.getPerms()))
                .map(SysResource::getPerms)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> selectCurrentUserResourceIds() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return new ArrayList<>();
        }

        if (loginUser.isAdmin()) {
            return resourceMapper.selectList(new LambdaQueryWrapper<SysResource>().select(SysResource::getId))
                    .stream().map(SysResource::getId).collect(Collectors.toList());
        }

        return getUserResources(loginUser).stream()
                .map(SysResource::getId)
                .collect(Collectors.toList());
    }

    private List<SysResource> getUserResources(LoginUser loginUser) {
        String clientCode = loginUser.getUserClient() != null ? loginUser.getUserClient() : "pc";
        
        if (loginUser.isAdmin()) {
            LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
            applyClientScope(wrapper, clientCode)
                    .orderByAsc(SysResource::getSort)
                    .orderByDesc(SysResource::getCreateTime);
            return resourceMapper.selectList(wrapper);
        }

        List<Long> roleIds = loginUser.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysRoleResource> roleResourceWrapper = new LambdaQueryWrapper<>();
        roleResourceWrapper.in(SysRoleResource::getRoleId, roleIds)
                .eq(SysRoleResource::getTenantId, loginUser.getTenantId());
        List<SysRoleResource> roleResources = roleResourceMapper.selectList(roleResourceWrapper);

        if (CollUtil.isEmpty(roleResources)) {
            return new ArrayList<>();
        }

        List<Long> resourceIds = roleResources.stream()
                .map(SysRoleResource::getResourceId)
                .distinct()
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(resourceIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysResource> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.in(SysResource::getId, resourceIds)
                .orderByAsc(SysResource::getSort)
                .orderByDesc(SysResource::getCreateTime);
        applyClientScope(resourceWrapper, clientCode);
        applyUserTypeScope(resourceWrapper, loginUser);
        return resourceMapper.selectList(resourceWrapper);
    }

    private List<SysResource> buildEntityTree(List<SysResource> list, Long parentId) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Map<Long, List<SysResource>> groupMap = list.stream()
                .collect(Collectors.groupingBy(SysResource::getParentId));

        List<SysResource> children = groupMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        children.forEach(node -> {
            List<SysResource> subChildren = buildEntityTree(list, node.getId());
            if (CollUtil.isNotEmpty(subChildren)) {
                node.setChildren(subChildren);
            }
        });

        return children;
    }

    private List<UserResourceTreeVO> buildVOTree(List<UserResourceTreeVO> list, Long parentId) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }

        Map<Long, List<UserResourceTreeVO>> groupMap = list.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        List<UserResourceTreeVO> children = groupMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        children.forEach(node -> {
            List<UserResourceTreeVO> subChildren = buildVOTree(list, node.getId());
            if (CollUtil.isNotEmpty(subChildren)) {
                node.setChildren(subChildren);
            }
        });

        return children;
    }

    private UserResourceTreeVO convertToVO(SysResource resource) {
        UserResourceTreeVO vo = new UserResourceTreeVO();
        vo.setId(resource.getId());
        vo.setParentId(resource.getParentId());
        vo.setResourceName(resource.getResourceName());
        vo.setResourceType(resource.getResourceType());
        vo.setPath(resource.getPath());
        vo.setComponent(resource.getComponent());
        vo.setIsExternal(resource.getIsExternal());
        vo.setSsoEnabled(resource.getSsoEnabled());
        vo.setSsoTargetClient(resource.getSsoTargetClient());
        vo.setOpenTarget(resource.getOpenTarget());
        vo.setIsPublic(resource.getIsPublic());
        vo.setMenuStatus(resource.getMenuStatus());
        vo.setVisible(resource.getVisible());
        vo.setPerms(resource.getPerms());
        vo.setIcon(resource.getIcon());
        vo.setClientCode(resource.getClientCode());
        vo.setApiMethod(resource.getApiMethod());
        vo.setApiUrl(resource.getApiUrl());
        vo.setKeepAlive(resource.getKeepAlive());
        vo.setAlwaysShow(resource.getAlwaysShow());
        vo.setRedirect(resource.getRedirect());
        vo.setRemark(resource.getRemark());
        vo.setSort(resource.getSort());
        return vo;
    }

    private LambdaQueryWrapper<SysResource> buildQueryWrapper(SysResourceQuery query) {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SysResource::getTenantId, query.getTenantId())
                .like(StringUtils.isNotBlank(query.getResourceName()), SysResource::getResourceName, query.getResourceName())
                .eq(query.getParentId() != null, SysResource::getParentId, query.getParentId())
                .eq(query.getResourceType() != null, SysResource::getResourceType, query.getResourceType())
                .eq(query.getVisible() != null, SysResource::getVisible, query.getVisible())
                .eq(query.getMinUserType() != null, SysResource::getMinUserType, query.getMinUserType())
                .orderByAsc(SysResource::getSort)
                .orderByDesc(SysResource::getCreateTime);
        applyClientScope(wrapper, query.getClientCode());
        return wrapper;
    }

    private void assertSystemAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以维护菜单和资源配置");
    }

    private void clearApiPermissionCacheIfChanged(boolean changed) {
        if (changed) {
            permissionService.clearConfiguredApiUrlCache();
        }
    }

    private void applyUserTypeScope(LambdaQueryWrapper<SysResource> wrapper, LoginUser loginUser) {
        int userType = normalizeUserType(loginUser == null ? null : loginUser.getUserType());
        wrapper.and(item -> item
                .isNull(SysResource::getMinUserType)
                .or()
                .ge(SysResource::getMinUserType, userType));
    }

    private int normalizeUserType(Integer userType) {
        if (userType == null) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        if (userType < SystemConstants.UserType.SYSTEM_ADMIN || userType > SystemConstants.UserType.NORMAL_USER) {
            return SystemConstants.UserType.NORMAL_USER;
        }
        return userType;
    }

    private Integer normalizeMinUserType(Integer minUserType) {
        return normalizeUserType(minUserType);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void validateParentUserTypeBoundary(Long parentId, Integer minUserType) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        SysResource parent = resourceMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("上级资源不存在");
        }
        int parentMinUserType = normalizeMinUserType(parent.getMinUserType());
        if (minUserType > parentMinUserType) {
            throw new RuntimeException("子资源开放范围不能高于上级资源");
        }
    }

    private LambdaQueryWrapper<SysResource> applyClientScope(LambdaQueryWrapper<SysResource> wrapper, String clientCode) {
        if (StringUtils.isBlank(clientCode)) {
            return wrapper;
        }
        return wrapper.and(item -> item
                .eq(SysResource::getClientCode, clientCode)
                .or()
                .isNull(SysResource::getClientCode)
                .or()
                .eq(SysResource::getClientCode, ""));
    }
}
