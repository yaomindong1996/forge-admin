package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户角色、组织、租户和岗位关系的生命周期协调器。
 */
@RequiredArgsConstructor
final class SysUserRelationCoordinator {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysOrgMapper orgMapper;
    private final SysUserAccessPolicy accessPolicy;
    private final SysUserAssignmentPolicy assignmentPolicy;
    private final BiConsumer<Long, Long> sessionSynchronizer;
    private final BiConsumer<Long, Long> regionSynchronizer;

    boolean bindUserRoles(Long userId, Long[] roleIds, Long tenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }
        return syncUserRoles(userId, Arrays.asList(roleIds), tenantId);
    }

    boolean batchBindUserRoles(BatchUserRoleBindDTO dto) {
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty()
            || dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            return false;
        }
        List<Long> userIds = distinctIds(dto.getUserIds());
        List<Long> roleIds = distinctIds(dto.getRoleIds());
        if (userIds.isEmpty() || roleIds.isEmpty()) {
            return false;
        }

        Long tenantId = dto.getTenantId();
        for (Long userId : userIds) {
            accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
            Set<Long> mergedRoleIds = new HashSet<>(selectUserRoleIds(userId, tenantId));
            mergedRoleIds.addAll(roleIds);
            syncUserRoles(userId, new ArrayList<>(mergedRoleIds), tenantId);
        }
        return true;
    }

    boolean unbindUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null || roleIds.length == 0) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        Long tenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, null);

        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
            .eq(SysUserOrgRole::getUserId, userId)
            .eq(SysUserOrgRole::getTenantId, tenantId)
            .in(SysUserOrgRole::getRoleId, Arrays.asList(roleIds)));
        boolean deleted = userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getUserId, userId)
            .eq(SysUserRole::getTenantId, tenantId)
            .in(SysUserRole::getRoleId, Arrays.asList(roleIds))) > 0;
        if (deleted) {
            sessionSynchronizer.accept(userId, tenantId);
        }
        return deleted;
    }

    boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        if (userId == null || orgId == null) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = accessPolicy.resolveTenantScopedOperationTenantId(user, null);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(),
            Objects.equals(user.getTenantId(), tenantId));
        assignmentPolicy.validateOrgTenant(List.of(orgId), tenantId);

        Long count = userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrg>()
            .eq(SysUserOrg::getUserId, userId)
            .eq(SysUserOrg::getTenantId, tenantId)
            .eq(SysUserOrg::getOrgId, orgId));
        if (count > 0) {
            return false;
        }

        if (Objects.equals(isMain, 1)) {
            SysUserOrg updateOrg = new SysUserOrg();
            updateOrg.setIsMain(0);
            userOrgMapper.update(updateOrg, new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getIsMain, 1));
        }

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setTenantId(tenantId);
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain != null ? isMain : 0);
        boolean inserted = userOrgMapper.insert(userOrg) > 0;
        if (inserted && Objects.equals(isMain, 1)) {
            regionSynchronizer.accept(userId, orgId);
            sessionSynchronizer.accept(userId, tenantId);
        }
        return inserted;
    }

    boolean unbindUserOrg(Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        Long tenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, null);

        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
            .eq(SysUserOrgRole::getUserId, userId)
            .eq(SysUserOrgRole::getTenantId, tenantId)
            .eq(SysUserOrgRole::getOrgId, orgId));
        boolean deleted = userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
            .eq(SysUserOrg::getUserId, userId)
            .eq(SysUserOrg::getTenantId, tenantId)
            .eq(SysUserOrg::getOrgId, orgId)) > 0;
        if (deleted) {
            sessionSynchronizer.accept(userId, tenantId);
        }
        return deleted;
    }

    List<Long> selectUserRoleIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        accessPolicy.assertCanReadUser(userId);
        Long operationTenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId);
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, operationTenantId)
                .select(SysUserRole::getRoleId))
            .stream()
            .map(SysUserRole::getRoleId)
            .filter(this::isRoleVisibleForCurrentUser)
            .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return roleIds;
        }
        Set<Long> activeRoleIds = TenantContextHolder.executeIgnore(() -> roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                    .in(SysRole::getId, roleIds)
                    .select(SysRole::getId)))
            .stream()
            .map(SysRole::getId)
            .collect(Collectors.toSet());
        return roleIds.stream().filter(activeRoleIds::contains).collect(Collectors.toList());
    }

    List<Long> selectUserOrgIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        accessPolicy.assertCanReadUser(userId);
        return userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId,
                    accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserOrg::getOrgId))
            .stream()
            .map(SysUserOrg::getOrgId)
            .collect(Collectors.toList());
    }

    List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        accessPolicy.assertCanReadUser(userId);
        Long operationTenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId);
        List<SysUserOrg> userOrgs = TenantContextHolder.executeIgnore(() ->
            userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, operationTenantId)));
        if (userOrgs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orgIds = userOrgs.stream().map(SysUserOrg::getOrgId).collect(Collectors.toList());
        List<SysOrg> orgs = TenantContextHolder.executeIgnore(() ->
            orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, operationTenantId)
                .in(SysOrg::getId, orgIds)));
        Map<Long, SysOrg> orgMap = orgs.stream()
            .collect(Collectors.toMap(SysOrg::getId, item -> item, (left, right) -> left));

        return userOrgs.stream().map(userOrg -> {
            SysOrg org = orgMap.get(userOrg.getOrgId());
            List<String> roleNames = userOrgRoleMapper.selectRoleNamesByUserOrg(
                operationTenantId, userId, userOrg.getOrgId());
            UserOrgBindingVO vo = new UserOrgBindingVO();
            vo.setTenantId(operationTenantId);
            vo.setUserId(userId);
            vo.setOrgId(userOrg.getOrgId());
            vo.setOrgName(org == null ? null : org.getOrgName());
            vo.setParentId(org == null ? null : org.getParentId());
            vo.setAncestors(org == null ? null : org.getAncestors());
            vo.setIsMain(userOrg.getIsMain());
            vo.setRoleNames(roleNames);
            vo.setRoleCount(roleNames == null ? 0 : roleNames.size());
            return vo;
        }).collect(Collectors.toList());
    }

    List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId) {
        if (userId == null || orgId == null) {
            return new ArrayList<>();
        }
        accessPolicy.assertCanReadUser(userId);
        Long operationTenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId);
        assignmentPolicy.validateUserOrgMembership(userId, orgId, operationTenantId);
        return userOrgRoleMapper.selectUserOrgRoleIds(operationTenantId, userId, orgId)
            .stream()
            .filter(this::isRoleVisibleForCurrentUser)
            .collect(Collectors.toList());
    }

    boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId) {
        if (userId == null || orgId == null || roleIds == null) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);

        Long operationTenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId);
        assignmentPolicy.validateUserOrgMembership(userId, orgId, operationTenantId);
        List<Long> normalizedRoleIds = assignmentPolicy.normalizeRoleIds(roleIds);
        LoginUser loginUser = accessPolicy.requireLoginUser();
        Set<Long> manageableRoleIds = resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin()) {
            if (!Objects.equals(orgId, loginUser.getActiveOrgId())) {
                throw new RuntimeException("只能维护当前组织下的用户角色");
            }
            if (!manageableRoleIds.containsAll(normalizedRoleIds)) {
                throw new RuntimeException("权限溢出：不能分配自己没有的角色");
            }
        }

        assignmentPolicy.validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), operationTenantId);
        assignmentPolicy.validateRoleStatus(normalizedRoleIds, operationTenantId);
        assignmentPolicy.validateRolesApplicableToOrg(normalizedRoleIds, orgId, operationTenantId);
        assignmentPolicy.validateRoleDataScopeForTarget(normalizedRoleIds, userId, operationTenantId);

        LambdaQueryWrapper<SysUserOrgRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserOrgRole::getTenantId, operationTenantId)
            .eq(SysUserOrgRole::getUserId, userId)
            .eq(SysUserOrgRole::getOrgId, orgId);
        if (!loginUser.isAdmin()) {
            if (manageableRoleIds.isEmpty()) {
                return normalizedRoleIds.isEmpty();
            }
            deleteWrapper.in(SysUserOrgRole::getRoleId, manageableRoleIds);
        }
        if (!normalizedRoleIds.isEmpty()) {
            deleteWrapper.notIn(SysUserOrgRole::getRoleId, normalizedRoleIds);
        }
        userOrgRoleMapper.delete(deleteWrapper);

        if (!normalizedRoleIds.isEmpty()) {
            Set<Long> existingRoleIds = new HashSet<>(userOrgRoleMapper.selectUserOrgRoleIds(
                operationTenantId, userId, orgId));
            for (Long roleId : normalizedRoleIds) {
                if (!existingRoleIds.contains(roleId)) {
                    userOrgRoleMapper.insert(userOrgRole(operationTenantId, userId, orgId, roleId));
                }
            }
        }
        sessionSynchronizer.accept(userId, operationTenantId);
        return true;
    }

    List<SysUserTenantVO> selectUserTenants(Long userId) {
        accessPolicy.assertCanManageUser(userId);
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(userId, false));
    }

    boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以绑定用户租户");
        }
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        if (userId == null || dto == null || dto.getTenantIds() == null || dto.getTenantIds().isEmpty()) {
            return false;
        }
        List<Long> tenantIds = accessPolicy.normalizeTenantIdList(dto.getTenantIds());
        if (tenantIds.isEmpty()) {
            return false;
        }
        Long defaultTenantId = dto.getDefaultTenantId();
        if (defaultTenantId == null || !tenantIds.contains(defaultTenantId)) {
            defaultTenantId = tenantIds.get(0);
        }
        Integer memberType = normalizeMemberType(dto.getMemberType());
        tenantIds.forEach(accessPolicy::validateTenantEnabled);

        LambdaQueryWrapper<SysUserTenant> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserTenant::getUserId, userId)
            .notIn(SysUserTenant::getTenantId, tenantIds);
        List<Long> removedTenantIds = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectList(deleteWrapper))
            .stream()
            .map(SysUserTenant::getTenantId)
            .collect(Collectors.toList());
        if (!removedTenantIds.isEmpty()) {
            deleteTenantRelations(userId, removedTenantIds);
        }
        TenantContextHolder.executeIgnore(() -> userTenantMapper.delete(deleteWrapper));

        for (Long tenantId : tenantIds) {
            upsertUserTenant(userId, tenantId, memberType, Objects.equals(tenantId, defaultTenantId));
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setTenantId(defaultTenantId);
        TenantContextHolder.executeIgnore(() -> userMapper.updateById(user));
        return true;
    }

    boolean batchBindUserTenant(BatchUserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以批量加入用户租户");
        }
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty()
            || dto.getTenantId() == null) {
            return false;
        }
        Long tenantId = dto.getTenantId();
        accessPolicy.validateTenantEnabled(tenantId);
        Integer memberType = normalizeMemberType(dto.getMemberType());
        List<Long> userIds = distinctIds(dto.getUserIds());
        if (userIds.isEmpty()) {
            return false;
        }

        List<SysUser> users = TenantContextHolder.executeIgnore(() -> userMapper.selectBatchIds(userIds));
        Map<Long, SysUser> userMap = users.stream()
            .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
        Set<Long> usersWithMembership = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectList(new LambdaQueryWrapper<SysUserTenant>()
                    .in(SysUserTenant::getUserId, userIds)
                    .eq(SysUserTenant::getStatus, 1)
                    .select(SysUserTenant::getUserId)))
            .stream()
            .map(SysUserTenant::getUserId)
            .collect(Collectors.toSet());

        for (Long userId : userIds) {
            accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
            SysUser user = userMap.get(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            boolean defaultTenant = user.getTenantId() == null || !usersWithMembership.contains(userId);
            upsertUserTenant(userId, tenantId, memberType, defaultTenant);
            if (defaultTenant) {
                SysUser updateUser = new SysUser();
                updateUser.setId(userId);
                updateUser.setTenantId(tenantId);
                TenantContextHolder.executeIgnore(() -> userMapper.updateById(updateUser));
            }
        }
        return true;
    }

    boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId) {
        if (userId == null || orgIds == null || orgIds.isEmpty()) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = accessPolicy.resolveTenantScopedOperationTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(),
            Objects.equals(user.getTenantId(), tenantId));
        assignmentPolicy.validateOrgTenant(orgIds, tenantId);

        Long effectiveMainOrgId = mainOrgId != null && orgIds.contains(mainOrgId) ? mainOrgId : null;
        List<SysUserOrg> existingOrgs = userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
            .eq(SysUserOrg::getUserId, userId)
            .eq(SysUserOrg::getTenantId, tenantId));
        List<Long> toDelete = existingOrgs.stream()
            .map(SysUserOrg::getOrgId)
            .filter(orgId -> !orgIds.contains(orgId))
            .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .in(SysUserOrgRole::getOrgId, toDelete));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .in(SysUserOrg::getOrgId, toDelete));
        }

        for (Long orgId : orgIds) {
            SysUserOrg userOrg = existingOrgs.stream()
                .filter(org -> org.getOrgId().equals(orgId))
                .findFirst()
                .orElse(null);
            int isMain = orgId.equals(effectiveMainOrgId) ? 1 : 0;
            if (userOrg == null) {
                userOrgMapper.insert(userOrg(tenantId, userId, orgId, isMain));
            } else if (!Objects.equals(userOrg.getIsMain(), isMain)) {
                userOrg.setIsMain(isMain);
                userOrgMapper.updateById(userOrg);
            }
        }

        if (effectiveMainOrgId != null) {
            regionSynchronizer.accept(userId, effectiveMainOrgId);
        }
        sessionSynchronizer.accept(userId, tenantId);
        return true;
    }

    boolean syncUserRoles(Long userId, List<Long> roleIds, Long requestedTenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }

        Long tenantId = accessPolicy.resolveRoleBindTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(),
            Objects.equals(user.getTenantId(), tenantId));
        List<Long> normalizedRoleIds = distinctIds(roleIds);
        LoginUser loginUser = accessPolicy.requireLoginUser();
        Set<Long> manageableRoleIds = resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin() && !manageableRoleIds.containsAll(normalizedRoleIds)) {
            throw new RuntimeException("权限溢出：不能分配自己没有的角色");
        }
        assignmentPolicy.validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), tenantId);
        assignmentPolicy.validateRoleDataScopeForTarget(normalizedRoleIds, userId, tenantId);

        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId).eq(SysUserRole::getTenantId, tenantId);
        if (!loginUser.isAdmin()) {
            if (manageableRoleIds.isEmpty()) {
                return normalizedRoleIds.isEmpty();
            }
            deleteWrapper.in(SysUserRole::getRoleId, manageableRoleIds);
        }
        if (!normalizedRoleIds.isEmpty()) {
            deleteWrapper.notIn(SysUserRole::getRoleId, normalizedRoleIds);
        }
        userRoleMapper.delete(deleteWrapper);

        if (!normalizedRoleIds.isEmpty()) {
            Set<Long> existingRoleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId)
                    .in(SysUserRole::getRoleId, normalizedRoleIds))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toSet());
            for (Long roleId : normalizedRoleIds) {
                if (!existingRoleIds.contains(roleId)) {
                    userRoleMapper.insert(userRole(tenantId, userId, roleId));
                }
            }
        }
        syncLegacyRolesToUserOrgs(userId, tenantId, normalizedRoleIds, loginUser, manageableRoleIds);
        return true;
    }

    void upsertUserTenant(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        if (defaultTenant) {
            SysUserTenant update = new SysUserTenant();
            update.setIsDefault(0);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.update(update,
                new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId)));
        }

        SysUserTenant existing = TenantContextHolder.executeIgnore(() ->
            userTenantMapper.selectOne(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId)));
        if (existing == null) {
            SysUserTenant member = new SysUserTenant();
            member.setUserId(userId);
            member.setTenantId(tenantId);
            member.setMemberType(normalizeMemberType(memberType));
            member.setIsDefault(defaultTenant ? 1 : 0);
            member.setStatus(EnableStatus.ENABLED.getCode());
            TenantContextHolder.executeIgnore(() -> userTenantMapper.insert(member));
            return;
        }
        existing.setMemberType(normalizeMemberType(memberType));
        existing.setIsDefault(defaultTenant ? 1 : 0);
        existing.setStatus(EnableStatus.ENABLED.getCode());
        TenantContextHolder.executeIgnore(() -> userTenantMapper.updateById(existing));
    }

    void syncMemberTypeFromUserType(Long userId, Integer userType) {
        Integer memberType = normalizeMemberType(userType);
        TenantContextHolder.executeIgnore(() -> userTenantMapper.update(null,
            new LambdaUpdateWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .set(SysUserTenant::getMemberType, memberType)));
    }

    boolean removeUserFromTenant(Long userId, Long tenantId) {
        return TenantContextHolder.executeIgnore(() -> {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            deleteTenantRelations(userId, List.of(tenantId));
            int deleted = userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId));
            Long remainingTenants = userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getStatus, 1));
            // 系统管理员账号永不随「移出租户」被物理删除
            if ((remainingTenants == null || remainingTenants == 0)
                    && !Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
                userMapper.deleteById(userId);
            }
            return deleted > 0;
        });
    }

    boolean deleteUserGlobally(Long userId) {
        return TenantContextHolder.executeIgnore(() -> {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                return false;
            }
            if (Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
                throw new RuntimeException("不能删除系统管理员");
            }
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>().eq(SysUserOrgRole::getUserId, userId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getUserId, userId));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
            userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId));
            return userMapper.deleteById(userId) > 0;
        });
    }

    List<Long> selectUserPostIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        accessPolicy.assertCanReadUser(userId);
        Long operationTenantId = accessPolicy.resolveTenantScopedOperationTenantId(userId, tenantId);
        return userPostMapper.selectActivePostIdsByUser(userId, operationTenantId);
    }

    boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long requestedTenantId) {
        if (userId == null || postIds == null) {
            return false;
        }
        accessPolicy.assertCanManageUser(userId);
        accessPolicy.assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = accessPolicy.resolveTenantScopedOperationTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(),
            Objects.equals(user.getTenantId(), tenantId));
        List<Long> normalizedPostIds = distinctIds(postIds);
        if (!normalizedPostIds.isEmpty()) {
            assignmentPolicy.validatePostTenant(normalizedPostIds, tenantId);
        }
        Long effectiveMainPostId = mainPostId != null && normalizedPostIds.contains(mainPostId)
            ? mainPostId : null;

        List<SysUserPost> existingPosts = userPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
            .eq(SysUserPost::getUserId, userId)
            .eq(SysUserPost::getTenantId, tenantId));
        List<Long> toDelete = existingPosts.stream()
            .map(SysUserPost::getPostId)
            .filter(postId -> !normalizedPostIds.contains(postId))
            .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                .eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, tenantId)
                .in(SysUserPost::getPostId, toDelete));
        }

        for (Long postId : normalizedPostIds) {
            SysUserPost userPost = existingPosts.stream()
                .filter(item -> item.getPostId().equals(postId))
                .findFirst()
                .orElse(null);
            int isMain = postId.equals(effectiveMainPostId) ? 1 : 0;
            if (userPost == null) {
                userPostMapper.insert(userPost(tenantId, userId, postId, isMain));
            } else if (!Objects.equals(userPost.getIsMain(), isMain)) {
                userPost.setIsMain(isMain);
                userPostMapper.updateById(userPost);
            }
        }
        return true;
    }

    Integer normalizeMemberType(Integer memberType) {
        return Objects.equals(memberType, 1) ? 1 : 2;
    }

    Set<Long> resolveManageableRoleIds(LoginUser loginUser) {
        if (loginUser == null || loginUser.isAdmin()) {
            return new HashSet<>();
        }
        return loginUser.getRoleIds() == null
            ? new HashSet<>() : new HashSet<>(loginUser.getRoleIds());
    }

    private void ensureUserTenantBound(Long userId, Long tenantId, Integer memberType,
                                       boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
            userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                .eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId)
                .eq(SysUserTenant::getStatus, 1)));
        if (count == null || count == 0) {
            upsertUserTenant(userId, tenantId, memberType, defaultTenant);
        }
    }

    private void syncLegacyRolesToUserOrgs(Long userId, Long tenantId, List<Long> roleIds,
                                            LoginUser loginUser, Set<Long> manageableRoleIds) {
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
            .eq(SysUserOrg::getUserId, userId)
            .eq(SysUserOrg::getTenantId, tenantId));
        if (userOrgs.isEmpty()) {
            return;
        }
        for (SysUserOrg userOrg : userOrgs) {
            List<Long> applicableRoleIds = roleIds.stream()
                .filter(roleId -> assignmentPolicy.isRoleApplicableToOrg(
                    roleId, userOrg.getOrgId(), tenantId))
                .collect(Collectors.toList());
            LambdaQueryWrapper<SysUserOrgRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .eq(SysUserOrgRole::getOrgId, userOrg.getOrgId());
            if (!loginUser.isAdmin()) {
                if (manageableRoleIds.isEmpty()) {
                    continue;
                }
                deleteWrapper.in(SysUserOrgRole::getRoleId, manageableRoleIds);
            }
            if (!applicableRoleIds.isEmpty()) {
                deleteWrapper.notIn(SysUserOrgRole::getRoleId, applicableRoleIds);
            }
            userOrgRoleMapper.delete(deleteWrapper);

            Set<Long> existingRoleIds = new HashSet<>(userOrgRoleMapper.selectUserOrgRoleIds(
                tenantId, userId, userOrg.getOrgId()));
            for (Long roleId : applicableRoleIds) {
                if (!existingRoleIds.contains(roleId)) {
                    userOrgRoleMapper.insert(userOrgRole(tenantId, userId, userOrg.getOrgId(), roleId));
                }
            }
        }
        sessionSynchronizer.accept(userId, tenantId);
    }

    private void deleteTenantRelations(Long userId, List<Long> tenantIds) {
        TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId).in(SysUserRole::getTenantId, tenantIds));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId).in(SysUserOrgRole::getTenantId, tenantIds));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId).in(SysUserOrg::getTenantId, tenantIds));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                .eq(SysUserPost::getUserId, userId).in(SysUserPost::getTenantId, tenantIds));
        });
    }

    private boolean isRoleVisibleForCurrentUser(Long roleId) {
        LoginUser loginUser = accessPolicy.requireLoginUser();
        return loginUser.isAdmin()
            || (loginUser.getRoleIds() != null && loginUser.getRoleIds().contains(roleId));
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private SysUserRole userRole(Long tenantId, Long userId, Long roleId) {
        SysUserRole value = new SysUserRole();
        value.setTenantId(tenantId);
        value.setUserId(userId);
        value.setRoleId(roleId);
        return value;
    }

    private SysUserOrgRole userOrgRole(Long tenantId, Long userId, Long orgId, Long roleId) {
        SysUserOrgRole value = new SysUserOrgRole();
        value.setTenantId(tenantId);
        value.setUserId(userId);
        value.setOrgId(orgId);
        value.setRoleId(roleId);
        return value;
    }

    private SysUserOrg userOrg(Long tenantId, Long userId, Long orgId, int isMain) {
        SysUserOrg value = new SysUserOrg();
        value.setTenantId(tenantId);
        value.setUserId(userId);
        value.setOrgId(orgId);
        value.setIsMain(isMain);
        return value;
    }

    private SysUserPost userPost(Long tenantId, Long userId, Long postId, int isMain) {
        SysUserPost value = new SysUserPost();
        value.setTenantId(tenantId);
        value.setUserId(userId);
        value.setPostId(postId);
        value.setIsMain(isMain);
        return value;
    }
}
