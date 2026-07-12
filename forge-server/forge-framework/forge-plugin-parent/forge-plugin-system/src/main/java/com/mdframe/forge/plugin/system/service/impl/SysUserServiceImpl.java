package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysPost;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysRoleOrg;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private static final String[] USER_MANAGEMENT_PERMISSIONS = {
            "system:user:list", "system:user:query", "system:user:add", "system:user:edit", "system:user:remove",
            "system:org:list", "system:org:query",
            "system:role:list", "system:role:query"
    };

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleOrgMapper roleOrgMapper;
    private final SysOrgMapper orgMapper;
    private final SysPostMapper postMapper;
    private final SysRegionMapper regionMapper;
    private final IUserLoadService userLoadService;

    @Override
    public IPage<SysUser> selectUserPage(SysUserQuery query) {
        normalizeUserQueryTenant(query);
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        return TenantContextHolder.executeIgnore(() -> userMapper.selectUserPage(page, query));
    }

    @Override
    public List<SysUser> selectExportList(SysUserQuery query) {
        normalizeUserQueryTenant(query);
        return TenantContextHolder.executeIgnore(() -> userMapper.selectExportList(query));
    }

    @Override
    public SysUser selectUserById(Long id) {
        assertCanReadUser(id);
        return TenantContextHolder.executeIgnore(() -> userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO dto) {
        assertUserManagementAllowed();
        LoginUser loginUser = requireLoginUser();
        List<Long> tenantIds = dto.getTenantIds() == null
                ? List.of(resolveWriteTenantId(dto.getTenantId()))
                : resolveWriteTenantIds(dto.getTenantIds(), dto.getTenantId(), loginUser);
        Long tenantId = resolveDefaultTenantId(tenantIds, dto.getTenantId());
        validateUserTypeForWrite(dto);
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        user.setTenantId(tenantId);
        user.setUserType(resolveWriteUserType(dto.getUserType()));
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setForcePasswordChange(true);
        boolean inserted = userMapper.insert(user) > 0;
        if (inserted) {
            for (Long bindTenantId : tenantIds) {
                upsertUserTenant(user.getId(), bindTenantId, user.getUserType(), Objects.equals(bindTenantId, tenantId));
            }
            List<Long> orgIds = dto.getOrgIds() == null ? List.of() : dto.getOrgIds();
            if (!orgIds.isEmpty()) {
                Long mainOrgId = dto.getMainOrgId() != null ? dto.getMainOrgId() : orgIds.get(0);
                bindUserOrgs(user.getId(), orgIds, mainOrgId, tenantId);
            } else if (!loginUser.isAdmin() && loginUser.getActiveOrgId() != null) {
                bindUserOrgs(user.getId(), List.of(loginUser.getActiveOrgId()), loginUser.getActiveOrgId(), tenantId);
            }
            // 同步绑定角色
            if (dto.getRoleIds() != null) {
                syncUserRoles(user.getId(), dto.getRoleIds(), tenantId);
            }
            // 同步绑定岗位
            if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) {
                bindUserPosts(user.getId(), dto.getPostIds(), dto.getPostIds().get(0), tenantId);
            }
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUserDTO dto) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && isCurrentLoginUser(dto.getId(), loginUser)) {
            return updateCurrentUserFromManagement(dto);
        }
        assertCanManageUser(dto.getId());
        assertNotSelfManagement(dto.getId());
        validateUserTypeForWrite(dto);
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        // 修改时不更新密码
        user.setPassword(null);

        Long tenantId = null;
        List<Long> tenantIds = null;
        if (loginUser.isAdmin()) {
            if (dto.getTenantIds() != null) {
                tenantIds = resolveWriteTenantIds(dto.getTenantIds(), dto.getTenantId(), loginUser);
                tenantId = resolveDefaultTenantId(tenantIds, dto.getTenantId());
            } else {
                tenantId = resolveWriteTenantId(dto.getTenantId());
            }
            user.setTenantId(tenantId);
            user.setUserType(resolveWriteUserType(dto.getUserType()));
        } else {
            // 租户管理员只维护当前租户成员资料，不改变用户默认租户和全局用户类型。
            user.setTenantId(null);
            user.setUserType(null);
        }

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated && loginUser.isAdmin()) {
            if (tenantIds != null) {
                UserTenantBindDTO tenantBindDTO = new UserTenantBindDTO();
                tenantBindDTO.setTenantIds(tenantIds);
                tenantBindDTO.setDefaultTenantId(tenantId);
                tenantBindDTO.setMemberType(user.getUserType());
                bindUserTenants(user.getId(), tenantBindDTO);
            } else {
                upsertUserTenant(user.getId(), tenantId, user.getUserType(), true);
            }
        }
        // 同步绑定角色。roleIds 传空数组表示清空当前可管理范围内的角色。
        if (updated && dto.getRoleIds() != null) {
            syncUserRoles(user.getId(), dto.getRoleIds(), tenantId);
        }
        // 同步绑定岗位
        if (updated && dto.getPostIds() != null) {
            bindUserPosts(user.getId(), dto.getPostIds(), !dto.getPostIds().isEmpty() ? dto.getPostIds().get(0) : null, tenantId);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long id) {
        assertCanManageUser(id);
        assertNotSelfManagement(id);
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            return removeUserFromTenant(id, loginUser.getTenantId());
        }
        return deleteUserGlobally(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(Long[] ids) {
        for (Long id : ids) {
            deleteUserById(id);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserRoles(Long userId, Long[] roleIds) {
        return bindUserRoles(userId, roleIds, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserRoles(Long userId, Long[] roleIds, Long tenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }
        return syncUserRoles(userId, Arrays.asList(roleIds), tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserRoles(BatchUserRoleBindDTO dto) {
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty()
                || dto.getRoleIds() == null || dto.getRoleIds().isEmpty()) {
            return false;
        }
        List<Long> userIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> roleIds = dto.getRoleIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty() || roleIds.isEmpty()) {
            return false;
        }

        Long tenantId = dto.getTenantId();
        for (Long userId : userIds) {
            assertNotSelfManagement(userId);
            Set<Long> mergedRoleIds = new HashSet<>(selectUserRoleIds(userId, tenantId));
            mergedRoleIds.addAll(roleIds);
            syncUserRoles(userId, new ArrayList<>(mergedRoleIds), tenantId);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null || roleIds.length == 0) {
            return false;
        }
        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        Long tenantId = resolveTenantScopedOperationTenantId(userId);
        
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId)
                .in(SysUserRole::getRoleId, Arrays.asList(roleIds));
        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .in(SysUserOrgRole::getRoleId, Arrays.asList(roleIds)));
        boolean deleted = userRoleMapper.delete(wrapper) > 0;
        if (deleted) {
            syncCurrentUserOrgSession(userId, tenantId);
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        if (userId == null || orgId == null) {
            return false;
        }
        
        // 获取用户信息以获取租户ID
        assertCanManageUser(userId);
        assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validateOrgTenant(List.of(orgId), tenantId);
        
        // 检查是否已存在
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getOrgId, orgId);
        Long count = userOrgMapper.selectCount(wrapper);
        
        if (count > 0) {
            return false;
        }
        
        // 如果是主组织，先取消其他主组织
        if (isMain != null && isMain == 1) {
            LambdaQueryWrapper<SysUserOrg> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .eq(SysUserOrg::getIsMain, 1);
            SysUserOrg updateOrg = new SysUserOrg();
            updateOrg.setIsMain(0);
            userOrgMapper.update(updateOrg, updateWrapper);
        }
        
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setTenantId(tenantId);
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsMain(isMain != null ? isMain : 0);
        boolean inserted = userOrgMapper.insert(userOrg) > 0;
        if (inserted && isMain != null && isMain == 1) {
            syncUserRegionFromMainOrg(userId, orgId);
            syncCurrentUserOrgSession(userId, tenantId);
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserOrg(Long userId, Long orgId) {
        if (userId == null || orgId == null) {
            return false;
        }
        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        Long tenantId = resolveTenantScopedOperationTenantId(userId);
        
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId)
                .eq(SysUserOrg::getOrgId, orgId);
        userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                .eq(SysUserOrgRole::getUserId, userId)
                .eq(SysUserOrgRole::getTenantId, tenantId)
                .eq(SysUserOrgRole::getOrgId, orgId));
        boolean deleted = userOrgMapper.delete(wrapper) > 0;
        if (deleted) {
            syncCurrentUserOrgSession(userId, tenantId);
        }
        return deleted;
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId) {
        return selectUserRoleIds(userId, null);
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanReadUser(userId);
        
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserRole::getRoleId);
        return userRoleMapper.selectList(wrapper)
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(this::isRoleVisibleForCurrentUser)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId) {
        return selectUserOrgIds(userId, null);
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanReadUser(userId);
        
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserOrg::getOrgId);
        return userOrgMapper.selectList(wrapper)
                .stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanReadUser(userId);

        Long operationTenantId = resolveTenantScopedOperationTenantId(userId, tenantId);
        List<SysUserOrg> userOrgs = TenantContextHolder.executeIgnore(() ->
                userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getUserId, userId)
                        .eq(SysUserOrg::getTenantId, operationTenantId)));
        if (userOrgs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orgIds = userOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
        List<SysOrg> orgs = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                        .eq(SysOrg::getTenantId, operationTenantId)
                        .in(SysOrg::getId, orgIds)));
        Map<Long, SysOrg> orgMap = orgs.stream()
                .collect(Collectors.toMap(SysOrg::getId, item -> item, (left, right) -> left));

        return userOrgs.stream()
                .map(userOrg -> {
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
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId) {
        if (userId == null || orgId == null) {
            return new ArrayList<>();
        }
        assertCanReadUser(userId);
        Long operationTenantId = resolveTenantScopedOperationTenantId(userId, tenantId);
        validateUserOrgMembership(userId, orgId, operationTenantId);
        return userOrgRoleMapper.selectUserOrgRoleIds(operationTenantId, userId, orgId)
                .stream()
                .filter(this::isRoleVisibleForCurrentUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId) {
        if (userId == null || orgId == null || roleIds == null) {
            return false;
        }
        assertCanManageUser(userId);
        assertNotSelfManagement(userId);

        Long operationTenantId = resolveTenantScopedOperationTenantId(userId, tenantId);
        validateUserOrgMembership(userId, orgId, operationTenantId);

        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        LoginUser loginUser = requireLoginUser();
        Set<Long> manageableRoleIds = resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin()) {
            if (!Objects.equals(orgId, loginUser.getActiveOrgId())) {
                throw new RuntimeException("只能维护当前组织下的用户角色");
            }
            if (!manageableRoleIds.containsAll(normalizedRoleIds)) {
                throw new RuntimeException("权限溢出：不能分配自己没有的角色");
            }
        }

        validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), operationTenantId);
        validateRoleStatus(normalizedRoleIds, operationTenantId);
        validateRolesApplicableToOrg(normalizedRoleIds, orgId, operationTenantId);
        validateRoleDataScopeForTarget(normalizedRoleIds, userId, operationTenantId);

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
                if (existingRoleIds.contains(roleId)) {
                    continue;
                }
                SysUserOrgRole userOrgRole = new SysUserOrgRole();
                userOrgRole.setTenantId(operationTenantId);
                userOrgRole.setUserId(userId);
                userOrgRole.setOrgId(orgId);
                userOrgRole.setRoleId(roleId);
                userOrgRoleMapper.insert(userOrgRole);
            }
        }

        syncCurrentUserOrgSession(userId, operationTenantId);
        return true;
    }

    @Override
    public List<SysUserTenantVO> selectUserTenants(Long userId) {
        assertCanManageUser(userId);
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(userId, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以绑定用户租户");
        }
        assertNotSelfManagement(userId);
        if (userId == null || dto == null || dto.getTenantIds() == null || dto.getTenantIds().isEmpty()) {
            return false;
        }
        List<Long> tenantIds = normalizeTenantIdList(dto.getTenantIds());
        if (tenantIds.isEmpty()) {
            return false;
        }
        Long defaultTenantId = dto.getDefaultTenantId();
        if (defaultTenantId == null || !tenantIds.contains(defaultTenantId)) {
            defaultTenantId = tenantIds.get(0);
        }
        Integer memberType = normalizeMemberType(dto.getMemberType());
        tenantIds.forEach(this::validateTenantEnabled);

        LambdaQueryWrapper<SysUserTenant> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserTenant::getUserId, userId)
                .notIn(SysUserTenant::getTenantId, tenantIds);
        List<Long> removedTenantIds = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectList(deleteWrapper))
                .stream()
                .map(SysUserTenant::getTenantId)
                .collect(Collectors.toList());
        if (!removedTenantIds.isEmpty()) {
            TenantContextHolder.executeIgnore(() -> {
                userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .in(SysUserRole::getTenantId, removedTenantIds));
                userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                        .eq(SysUserOrgRole::getUserId, userId)
                        .in(SysUserOrgRole::getTenantId, removedTenantIds));
                userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getUserId, userId)
                        .in(SysUserOrg::getTenantId, removedTenantIds));
                userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                        .eq(SysUserPost::getUserId, userId)
                        .in(SysUserPost::getTenantId, removedTenantIds));
            });
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserTenant(BatchUserTenantBindDTO dto) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || !loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以批量加入用户租户");
        }
        if (dto == null || dto.getUserIds() == null || dto.getUserIds().isEmpty() || dto.getTenantId() == null) {
            return false;
        }
        Long tenantId = dto.getTenantId();
        validateTenantEnabled(tenantId);
        Integer memberType = normalizeMemberType(dto.getMemberType());
        List<Long> userIds = dto.getUserIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return false;
        }

        for (Long userId : userIds) {
            assertNotSelfManagement(userId);
            SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            boolean defaultTenant = user.getTenantId() == null || !hasEnabledTenantMembership(userId);
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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId) {
        return bindUserOrgs(userId, orgIds, mainOrgId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId) {
        if (userId == null || orgIds == null || orgIds.isEmpty()) {
            return false;
        }
        
        // 获取用户信息以获取租户ID
        assertCanManageUser(userId);
        assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validateOrgTenant(orgIds, tenantId);
        
        // 验证主组织是否在组织列表中
        if (mainOrgId != null && !orgIds.contains(mainOrgId)) {
            mainOrgId = null;
        }
        
        // 获取用户当前的所有组织
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId);
        List<SysUserOrg> existingOrgs = userOrgMapper.selectList(wrapper);
        List<Long> existingOrgIds = existingOrgs.stream()
                .map(SysUserOrg::getOrgId)
                .collect(Collectors.toList());
        
        // 删除不再需要的组织
        List<Long> toDelete = existingOrgIds.stream()
                .filter(orgId -> !orgIds.contains(orgId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserOrg> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId)
                    .in(SysUserOrg::getOrgId, toDelete);
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getUserId, userId)
                    .eq(SysUserOrgRole::getTenantId, tenantId)
                    .in(SysUserOrgRole::getOrgId, toDelete));
            userOrgMapper.delete(deleteWrapper);
        }
        
        // 添加新的组织或更新现有组织
        for (Long orgId : orgIds) {
            SysUserOrg userOrg = existingOrgs.stream()
                    .filter(org -> org.getOrgId().equals(orgId))
                    .findFirst()
                    .orElse(null);
            
            if (userOrg == null) {
                // 插入新组织
                userOrg = new SysUserOrg();
                userOrg.setTenantId(tenantId);
                userOrg.setUserId(userId);
                userOrg.setOrgId(orgId);
                userOrg.setIsMain(orgId.equals(mainOrgId) ? 1 : 0);
                userOrgMapper.insert(userOrg);
            } else {
                // 更新现有组织的主组织标记
                int newIsMain = orgId.equals(mainOrgId) ? 1 : 0;
                if (!userOrg.getIsMain().equals(newIsMain)) {
                    userOrg.setIsMain(newIsMain);
                    userOrgMapper.updateById(userOrg);
                }
            }
        }

        if (mainOrgId != null) {
            syncUserRegionFromMainOrg(userId, mainOrgId);
        }
        syncCurrentUserOrgSession(userId, tenantId);
        return true;
    }
    
    @Override
    public void doUntieDisable(Long userId) {
        StpUtil.untieDisable(userId);
        // 同时更新数据库状态为正常
        this.updateUserStatus(userId, 1);
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(PasswordUtil.encrypt(newPassword));
        user.setForcePasswordChange(true);
        return TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            SysUserTenant member = new SysUserTenant();
            member.setStatus(status != null && status == 1 ? 1 : 0);
            LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, loginUser.getTenantId());
            return TenantContextHolder.executeIgnore(() -> userTenantMapper.update(member, wrapper) > 0);
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUserStatus(status);
        return TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
    }

    @Override
    public boolean updateUserProfile(SysUserDTO dto) {
        Long currentUserId = SessionHelper.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }

        SysUser user = new SysUser();
        user.setId(currentUserId);
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);

        // 同步更新 Session 中的 LoginUser，确保 /auth/userInfo 返回最新数据
        if (updated) {
            syncSessionUserProfile(dto);
        }

        return updated;
    }

    private boolean updateCurrentUserFromManagement(SysUserDTO dto) {
        SysUser user = new SysUser();
        user.setId(requireLoginUser().getUserId());
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setIdCard(dto.getIdCard());
        user.setGender(dto.getGender());
        user.setAvatar(dto.getAvatar());
        user.setRegionCode(dto.getRegionCode());
        user.setRemark(dto.getRemark());

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated) {
            syncSessionUserProfile(dto);
        }
        return updated;
    }

    private void normalizeUserQueryTenant(SysUserQuery query) {
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (!loginUser.isAdmin()) {
            query.setTenantId(loginUser.getTenantId());
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private Long resolveWriteTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.isAdmin()
                ? (requestedTenantId != null ? requestedTenantId : loginUser.getTenantId())
                : loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    private Long resolveCurrentTenantIdForNonAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.getTenantId() == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser.getTenantId();
    }

    private List<Long> resolveWriteTenantIds(List<Long> requestedTenantIds, Long defaultTenantId, LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (!loginUser.isAdmin()) {
            return List.of(resolveCurrentTenantIdForNonAdmin());
        }
        List<Long> tenantIds = normalizeTenantIdList(requestedTenantIds);
        if (tenantIds.isEmpty()) {
            tenantIds.add(defaultTenantId != null ? defaultTenantId : loginUser.getTenantId());
        }
        if (defaultTenantId != null && !tenantIds.contains(defaultTenantId)) {
            throw new RuntimeException("默认租户必须包含在所属租户中");
        }
        tenantIds.forEach(this::validateTenantEnabled);
        return tenantIds;
    }

    private List<Long> normalizeTenantIdList(List<Long> tenantIds) {
        if (tenantIds == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tenantIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private Long resolveDefaultTenantId(List<Long> tenantIds, Long requestedDefaultTenantId) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            throw new RuntimeException("租户不能为空");
        }
        if (requestedDefaultTenantId != null) {
            if (!tenantIds.contains(requestedDefaultTenantId)) {
                throw new RuntimeException("默认租户必须包含在所属租户中");
            }
            return requestedDefaultTenantId;
        }
        return tenantIds.get(0);
    }

    private Long resolveRoleBindTenantId(SysUser user) {
        return resolveRoleBindTenantId(user, null);
    }

    private Long resolveRoleBindTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                    ? requestedTenantId
                    : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    private void validateUserTypeForWrite(SysUserDTO dto) {
        LoginUser loginUser = requireLoginUser();
        Integer userType = dto.getUserType();
        if (loginUser.isAdmin()) {
            return;
        }
        if (userType != null && userType != 2) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    private Integer resolveWriteUserType(Integer requestedUserType) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return requestedUserType != null ? requestedUserType : 2;
        }
        return 2;
    }

    private void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<com.mdframe.forge.plugin.system.entity.SysTenant>()
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getId, tenantId)
                        .eq(com.mdframe.forge.plugin.system.entity.SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }

    private void assertCanManageUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户用户");
        }
        if (resolveEffectiveUserType(userId, loginUser.getTenantId()) != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }

    private void assertCanReadUser(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        assertUserManagementAllowed(loginUser);
        if (loginUser.isAdmin()) {
            return;
        }
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!isUserInTenant(userId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户用户");
        }
        if (isCurrentLoginUser(userId, loginUser)) {
            return;
        }
        if (resolveEffectiveUserType(userId, loginUser.getTenantId()) != SystemConstants.UserType.NORMAL_USER) {
            throw new RuntimeException("租户管理员只能查看普通用户");
        }
    }

    private Long resolveTenantScopedOperationTenantId(Long userId) {
        return resolveTenantScopedOperationTenantId(userId, null);
    }

    private Long resolveTenantScopedOperationTenantId(Long userId, Long requestedTenantId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return resolveTenantScopedOperationTenantId(user, requestedTenantId);
    }

    private Long resolveTenantScopedOperationTenantId(SysUser user) {
        return resolveTenantScopedOperationTenantId(user, null);
    }

    private Long resolveTenantScopedOperationTenantId(SysUser user, Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            Long tenantId = requestedTenantId != null
                    ? requestedTenantId
                    : (user.getTenantId() != null ? user.getTenantId() : loginUser.getTenantId());
            validateTenantEnabled(tenantId);
            return tenantId;
        }
        return resolveCurrentTenantIdForNonAdmin();
    }

    private boolean isUserInTenant(Long userId, Long tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    private boolean hasEnabledTenantMembership(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    private void assertUserManagementAllowed() {
        assertUserManagementAllowed(requireLoginUser());
    }

    private void assertUserManagementAllowed(LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, USER_MANAGEMENT_PERMISSIONS)) {
            throw new RuntimeException("无权访问用户组织管理功能");
        }
    }

    private boolean hasAnyPermission(LoginUser loginUser, String... permissions) {
        Set<String> userPermissions = loginUser == null ? null : loginUser.getPermissions();
        if (userPermissions == null || userPermissions.isEmpty() || permissions == null) {
            return false;
        }
        if (userPermissions.contains("*") || userPermissions.contains("*:*:*")) {
            return true;
        }
        for (String permission : permissions) {
            if (hasPermission(userPermissions, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(Set<String> userPermissions, String permission) {
        if (permission == null || userPermissions.contains(permission)) {
            return permission != null;
        }
        int splitIndex = permission.lastIndexOf(':');
        while (splitIndex > 0) {
            String wildcardPermission = permission.substring(0, splitIndex) + ":*";
            if (userPermissions.contains(wildcardPermission)) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private void assertNotSelfManagement(Long userId) {
        LoginUser loginUser = requireLoginUser();
        if (userId != null && Objects.equals(userId, loginUser.getUserId())) {
            throw new RuntimeException("不能在用户管理中维护当前登录用户");
        }
    }

    private void assertNotSelfManagementUnlessAdmin(Long userId) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (userId != null && Objects.equals(userId, loginUser.getUserId())) {
            throw new RuntimeException("不能在用户管理中维护当前登录用户");
        }
    }

    private boolean isCurrentLoginUser(Long userId, LoginUser loginUser) {
        return userId != null && loginUser != null && Objects.equals(userId, loginUser.getUserId());
    }

    private void syncSessionUserProfile(SysUserDTO dto) {
        com.mdframe.forge.starter.core.session.LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return;
        }
        if (dto.getUsername() != null) loginUser.setUsername(dto.getUsername());
        if (dto.getRealName() != null) loginUser.setRealName(dto.getRealName());
        if (dto.getPhone() != null) loginUser.setPhone(dto.getPhone());
        if (dto.getEmail() != null) loginUser.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) loginUser.setAvatar(dto.getAvatar());
        SessionHelper.setLoginUser(loginUser);
    }

    private void syncUserRegionFromMainOrg(Long userId, Long mainOrgId) {
        if (userId == null || mainOrgId == null) {
            return;
        }
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(mainOrgId));
        if (org == null || StrUtil.isBlank(org.getRegionCode())) {
            return;
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRegionCode(org.getRegionCode());
        TenantContextHolder.executeIgnore(() -> userMapper.updateById(user));
    }

    private void syncCurrentUserOrgSession(Long userId, Long tenantId) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null
                || !Objects.equals(loginUser.getUserId(), userId)
                || !Objects.equals(loginUser.getTenantId(), tenantId)) {
            return;
        }

        LoginUser fresh = userLoadService.loadUserByUserId(userId, tenantId, loginUser.getActiveOrgId());
        fresh.setLoginTime(loginUser.getLoginTime());
        fresh.setLoginIp(loginUser.getLoginIp());
        fresh.setUserClient(loginUser.getUserClient());
        SessionHelper.setLoginUser(fresh);
    }

    private void applyRegionToSession(LoginUser loginUser, String regionCode) {
        if (loginUser == null || StrUtil.isBlank(regionCode)) {
            return;
        }
        SysRegion region = TenantContextHolder.executeIgnore(() -> regionMapper.selectById(regionCode));
        if (region == null) {
            return;
        }
        loginUser.setRegionCode(region.getCode());
        loginUser.setRegionName(region.getName());
        loginUser.setRegionLevel(region.getLevel());
        loginUser.setRegionFullName(region.getFullName());
        loginUser.setRegionAncestors(buildRegionAncestors(region));
    }

    private String buildRegionAncestors(SysRegion region) {
        if (region == null || StrUtil.isBlank(region.getCode())) {
            return null;
        }
        StringBuilder ancestors = new StringBuilder();
        String currentCode = region.getCode();
        while (StrUtil.isNotBlank(currentCode)) {
            String lookupCode = currentCode;
            SysRegion currentRegion = TenantContextHolder.executeIgnore(() -> regionMapper.selectById(lookupCode));
            if (currentRegion == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = currentRegion.getParentCode();
        }
        return ancestors.toString();
    }

    private int resolveEffectiveUserType(Long userId, Long tenantId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (Objects.equals(user.getUserType(), SystemConstants.UserType.SYSTEM_ADMIN)) {
            return SystemConstants.UserType.SYSTEM_ADMIN;
        }
        SysUserTenant member = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectOne(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)
                        .last("LIMIT 1")));
        if (member == null) {
            throw new RuntimeException("目标用户不属于当前租户");
        }
        return Objects.equals(member.getMemberType(), SystemConstants.UserType.TENANT_ADMIN)
                ? SystemConstants.UserType.TENANT_ADMIN
                : SystemConstants.UserType.NORMAL_USER;
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

    private void validateRoleTenant(Long[] roleIds, Long tenantId) {
        if (roleIds == null || roleIds.length == 0) {
            return;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, Arrays.asList(roleIds))
                .eq(SysRole::getTenantId, tenantId));
        if (count == null || count != roleIds.length) {
            throw new RuntimeException("角色不属于当前操作租户");
        }
    }

    private List<Long> normalizeRoleIds(List<Long> roleIds) {
        if (roleIds == null) {
            return new ArrayList<>();
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void validateUserOrgMembership(Long userId, Long orgId, Long tenantId) {
        Long count = TenantContextHolder.executeIgnore(() ->
                userOrgMapper.selectCount(new LambdaQueryWrapper<SysUserOrg>()
                        .eq(SysUserOrg::getTenantId, tenantId)
                        .eq(SysUserOrg::getUserId, userId)
                        .eq(SysUserOrg::getOrgId, orgId)));
        if (count == null || count == 0) {
            throw new RuntimeException("用户未加入目标组织");
        }
    }

    private void validateRoleStatus(List<Long> roleIds, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleStatus, 1));
        if (count == null || count != roleIds.size()) {
            throw new RuntimeException("角色不存在或已禁用");
        }
    }

    private void validateRolesApplicableToOrg(List<Long> roleIds, Long orgId, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .in(SysRole::getId, roleIds)
                .select(SysRole::getId, SysRole::getOrgScopeType));
        List<Long> customRoleIds = roles.stream()
                .filter(role -> !isGlobalRoleScope(role))
                .map(SysRole::getId)
                .collect(Collectors.toList());
        if (customRoleIds.isEmpty()) {
            return;
        }
        Long count = roleOrgMapper.selectCount(new LambdaQueryWrapper<SysRoleOrg>()
                .eq(SysRoleOrg::getTenantId, tenantId)
                .eq(SysRoleOrg::getOrgId, orgId)
                .in(SysRoleOrg::getRoleId, customRoleIds));
        if (count == null || count != customRoleIds.size()) {
            throw new RuntimeException("角色不适用于目标组织");
        }
    }

    private boolean isRoleApplicableToOrg(Long roleId, Long orgId, Long tenantId) {
        if (roleId == null || orgId == null || tenantId == null) {
            return false;
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getId, roleId)
                .select(SysRole::getId, SysRole::getOrgScopeType));
        if (isGlobalRoleScope(role)) {
            return true;
        }
        Long count = roleOrgMapper.selectCount(new LambdaQueryWrapper<SysRoleOrg>()
                .eq(SysRoleOrg::getTenantId, tenantId)
                .eq(SysRoleOrg::getRoleId, roleId)
                .eq(SysRoleOrg::getOrgId, orgId));
        return count != null && count > 0;
    }

    private boolean isGlobalRoleScope(SysRole role) {
        return role != null && Objects.equals(role.getOrgScopeType(), SystemConstants.RoleOrgScope.GLOBAL);
    }

    private void validateOrgTenant(List<Long> orgIds, Long tenantId) {
        List<Long> normalizedOrgIds = orgIds == null ? new ArrayList<>() : orgIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedOrgIds.isEmpty()) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectCount(new LambdaQueryWrapper<SysOrg>()
                        .in(SysOrg::getId, normalizedOrgIds)
                        .eq(SysOrg::getTenantId, tenantId)));
        if (count == null || count != normalizedOrgIds.size()) {
            throw new RuntimeException("组织不属于当前操作租户");
        }
    }

    private void validatePostTenant(List<Long> postIds, Long tenantId) {
        List<Long> normalizedPostIds = postIds == null ? new ArrayList<>() : postIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedPostIds.isEmpty()) {
            return;
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                postMapper.selectCount(new LambdaQueryWrapper<SysPost>()
                        .in(SysPost::getId, normalizedPostIds)
                        .eq(SysPost::getTenantId, tenantId)));
        if (count == null || count != normalizedPostIds.size()) {
            throw new RuntimeException("岗位不属于当前操作租户");
        }
    }

    private void validateRoleDataScopeForTarget(List<Long> roleIds, Long userId, Long tenantId) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        int targetUserType = resolveEffectiveUserType(userId, tenantId);
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, tenantId));
        for (SysRole role : roles) {
            if (!isDataScopeAllowedForUserType(role.getDataScope(), targetUserType)) {
                throw new RuntimeException("角色数据范围超过目标用户类型上限");
            }
        }
    }

    private boolean isDataScopeAllowedForUserType(Integer dataScope, int userType) {
        if (dataScope == null || userType == SystemConstants.UserType.SYSTEM_ADMIN) {
            return true;
        }
        if (userType == SystemConstants.UserType.TENANT_ADMIN) {
            return dataScope != SystemConstants.RoleDataScope.ALL;
        }
        return dataScope != SystemConstants.RoleDataScope.ALL
                && dataScope != SystemConstants.RoleDataScope.TENANT;
    }

    private boolean syncUserRoles(Long userId, List<Long> roleIds) {
        return syncUserRoles(userId, roleIds, null);
    }

    private boolean syncUserRoles(Long userId, List<Long> roleIds, Long requestedTenantId) {
        if (userId == null || roleIds == null) {
            return false;
        }

        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }

        Long tenantId = resolveRoleBindTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));

        List<Long> normalizedRoleIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        LoginUser loginUser = requireLoginUser();
        Set<Long> manageableRoleIds = resolveManageableRoleIds(loginUser);
        if (!loginUser.isAdmin() && !manageableRoleIds.containsAll(normalizedRoleIds)) {
            throw new RuntimeException("权限溢出：不能分配自己没有的角色");
        }
        validateRoleTenant(normalizedRoleIds.toArray(new Long[0]), tenantId);
        validateRoleDataScopeForTarget(normalizedRoleIds, userId, tenantId);

        LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getTenantId, tenantId);
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

        if (normalizedRoleIds.isEmpty()) {
            syncLegacyRolesToUserOrgs(userId, tenantId, normalizedRoleIds, loginUser, manageableRoleIds);
            return true;
        }

        List<Long> existingRoleIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getTenantId, tenantId)
                        .in(SysUserRole::getRoleId, normalizedRoleIds))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        Set<Long> existingRoleIdSet = new HashSet<>(existingRoleIds);

        for (Long roleId : normalizedRoleIds) {
            if (existingRoleIdSet.contains(roleId)) {
                continue;
            }
            SysUserRole userRole = new SysUserRole();
            userRole.setTenantId(tenantId);
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }

        syncLegacyRolesToUserOrgs(userId, tenantId, normalizedRoleIds, loginUser, manageableRoleIds);
        return true;
    }

    private void syncLegacyRolesToUserOrgs(Long userId,
                                           Long tenantId,
                                           List<Long> roleIds,
                                           LoginUser loginUser,
                                           Set<Long> manageableRoleIds) {
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, userId)
                .eq(SysUserOrg::getTenantId, tenantId));
        if (userOrgs.isEmpty()) {
            return;
        }

        for (SysUserOrg userOrg : userOrgs) {
            List<Long> applicableRoleIds = roleIds.stream()
                    .filter(roleId -> isRoleApplicableToOrg(roleId, userOrg.getOrgId(), tenantId))
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
                if (existingRoleIds.contains(roleId)) {
                    continue;
                }
                SysUserOrgRole userOrgRole = new SysUserOrgRole();
                userOrgRole.setTenantId(tenantId);
                userOrgRole.setUserId(userId);
                userOrgRole.setOrgId(userOrg.getOrgId());
                userOrgRole.setRoleId(roleId);
                userOrgRoleMapper.insert(userOrgRole);
            }
        }
        syncCurrentUserOrgSession(userId, tenantId);
    }

    private Set<Long> resolveManageableRoleIds(LoginUser loginUser) {
        if (loginUser == null || loginUser.isAdmin()) {
            return new HashSet<>();
        }
        List<Long> roleIds = loginUser.getRoleIds();
        return roleIds == null ? new HashSet<>() : new HashSet<>(roleIds);
    }

    private boolean isRoleVisibleForCurrentUser(Long roleId) {
        LoginUser loginUser = requireLoginUser();
        return loginUser.isAdmin()
                || (loginUser.getRoleIds() != null && loginUser.getRoleIds().contains(roleId));
    }

    private void upsertUserTenant(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        if (userId == null || tenantId == null) {
            return;
        }
        if (defaultTenant) {
            SysUserTenant update = new SysUserTenant();
            update.setIsDefault(0);
            LambdaQueryWrapper<SysUserTenant> defaultWrapper = new LambdaQueryWrapper<>();
            defaultWrapper.eq(SysUserTenant::getUserId, userId);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.update(update, defaultWrapper));
        }

        LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserTenant::getUserId, userId)
                .eq(SysUserTenant::getTenantId, tenantId);
        SysUserTenant existing = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectOne(wrapper));
        if (existing == null) {
            SysUserTenant member = new SysUserTenant();
            member.setUserId(userId);
            member.setTenantId(tenantId);
            member.setMemberType(normalizeMemberType(memberType));
            member.setIsDefault(defaultTenant ? 1 : 0);
            member.setStatus(1);
            TenantContextHolder.executeIgnore(() -> userTenantMapper.insert(member));
            return;
        }

        existing.setMemberType(normalizeMemberType(memberType));
        existing.setIsDefault(defaultTenant ? 1 : 0);
        existing.setStatus(1);
        TenantContextHolder.executeIgnore(() -> userTenantMapper.updateById(existing));
    }

    private void ensureUserTenantBound(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
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

    private Integer normalizeMemberType(Integer memberType) {
        return memberType != null && memberType == 1 ? 1 : 2;
    }

    private boolean removeUserFromTenant(Long userId, Long tenantId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getTenantId, tenantId));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>()
                    .eq(SysUserOrgRole::getUserId, userId)
                    .eq(SysUserOrgRole::getTenantId, tenantId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>()
                    .eq(SysUserOrg::getUserId, userId)
                    .eq(SysUserOrg::getTenantId, tenantId));

            int deleted = userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, tenantId));

            Long remainingTenants = userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getStatus, 1));
            if (remainingTenants == null || remainingTenants == 0) {
                userMapper.deleteById(userId);
            }
            return deleted > 0;
        });
    }

    private boolean deleteUserGlobally(Long userId) {
        return TenantContextHolder.executeIgnore(() -> {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
            userOrgRoleMapper.delete(new LambdaQueryWrapper<SysUserOrgRole>().eq(SysUserOrgRole::getUserId, userId));
            userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getUserId, userId));
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
            userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>().eq(SysUserTenant::getUserId, userId));
            return userMapper.deleteById(userId) > 0;
        });
    }

    @Override
    public List<Long> selectUserPostIds(Long userId) {
        return selectUserPostIds(userId, null);
    }

    @Override
    public List<Long> selectUserPostIds(Long userId, Long tenantId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        assertCanReadUser(userId);

        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, resolveTenantScopedOperationTenantId(userId, tenantId))
                .select(SysUserPost::getPostId);
        return userPostMapper.selectList(wrapper)
                .stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId) {
        return bindUserPosts(userId, postIds, mainPostId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long requestedTenantId) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return false;
        }

        assertCanManageUser(userId);
        assertNotSelfManagement(userId);
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            return false;
        }
        Long tenantId = resolveTenantScopedOperationTenantId(user, requestedTenantId);
        ensureUserTenantBound(user.getId(), tenantId, user.getUserType(), Objects.equals(user.getTenantId(), tenantId));
        validatePostTenant(postIds, tenantId);

        // 验证主岗位是否在岗位列表中
        if (mainPostId != null && !postIds.contains(mainPostId)) {
            mainPostId = null;
        }

        // 获取当前所有岗位
        LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserPost::getUserId, userId)
                .eq(SysUserPost::getTenantId, tenantId);
        List<SysUserPost> existingPosts = userPostMapper.selectList(wrapper);
        List<Long> existingPostIds = existingPosts.stream()
                .map(SysUserPost::getPostId)
                .collect(Collectors.toList());

        // 删除不再需要的岗位
        List<Long> toDelete = existingPostIds.stream()
                .filter(postId -> !postIds.contains(postId))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            LambdaQueryWrapper<SysUserPost> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserPost::getUserId, userId)
                    .eq(SysUserPost::getTenantId, tenantId)
                    .in(SysUserPost::getPostId, toDelete);
            userPostMapper.delete(deleteWrapper);
        }

        // 添加新岗位或更新现有岗位
        for (Long postId : postIds) {
            SysUserPost userPost = existingPosts.stream()
                    .filter(p -> p.getPostId().equals(postId))
                    .findFirst()
                    .orElse(null);

            if (userPost == null) {
                userPost = new SysUserPost();
                userPost.setTenantId(tenantId);
                userPost.setUserId(userId);
                userPost.setPostId(postId);
                userPost.setIsMain(postId.equals(mainPostId) ? 1 : 0);
                userPostMapper.insert(userPost);
            } else {
                int newIsMain = postId.equals(mainPostId) ? 1 : 0;
                if (!userPost.getIsMain().equals(newIsMain)) {
                    userPost.setIsMain(newIsMain);
                    userPostMapper.updateById(userPost);
                }
            }
        }

        return true;
    }
}
