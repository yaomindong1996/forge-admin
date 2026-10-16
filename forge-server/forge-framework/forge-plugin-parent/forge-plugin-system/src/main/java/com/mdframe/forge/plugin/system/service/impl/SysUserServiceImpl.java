package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 用户Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

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
        Long tenantId = resolveWriteTenantId(null);
        List<Long> tenantIds = List.of(tenantId);
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
        assertNotSelfManagementUnlessAdmin(dto.getId());
        validateUserTypeForWrite(dto);
        if (dto.getUserStatus() != null) {
            accessPolicy().assertCanChangeUserStatus(dto.getId(), dto.getUserStatus());
        }
        if (loginUser.isAdmin() && dto.getUserType() != null) {
            accessPolicy().assertCanChangeUserType(dto.getId(), dto.getUserType());
        }
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);
        // 修改时不更新密码
        user.setPassword(null);

        Long tenantId = resolveWriteTenantId(null);
        // 未显式传入用户类型时保持原值，避免编辑时将类型意外重置为普通用户。
        if (loginUser.isAdmin() && dto.getUserType() != null) {
            user.setUserType(resolveWriteUserType(dto.getUserType()));
        } else {
            user.setUserType(null);
        }
        // 普通编辑不改默认租户和跨租户绑定；多租户绑定只走 bindUserTenants。
        user.setTenantId(null);

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        // 用户类型变更时同步各租户成员类型，保证角色数据范围校验按最新身份执行。
        if (updated && user.getUserType() != null) {
            syncMemberTypeFromUserType(user.getId(), user.getUserType());
        }
        // 同步绑定组织。orgIds 为空时保持原组织不变，避免误清空用户归属。
        if (updated && dto.getOrgIds() != null && !dto.getOrgIds().isEmpty()) {
            Long mainOrgId = dto.getMainOrgId() != null ? dto.getMainOrgId() : dto.getOrgIds().get(0);
            bindUserOrgs(user.getId(), dto.getOrgIds(), mainOrgId, tenantId);
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
        accessPolicy().assertCanDeleteUser(id);
        LoginUser loginUser = requireLoginUser();
        Long currentTenantId = loginUser.getTenantId();
        // 非超管：只从当前租户解绑
        if (!loginUser.isAdmin()) {
            return removeUserFromTenant(id, currentTenantId);
        }
        // 超管切到非默认租户后：只解除该租户成员关系，禁止全局物理删除（避免误删账号导致无法切回）
        if (currentTenantId != null && !Objects.equals(currentTenantId, 1L)) {
            return removeUserFromTenant(id, currentTenantId);
        }
        return deleteUserGlobally(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return false;
        }
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
        return relationCoordinator().bindUserRoles(userId, roleIds, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserRoles(BatchUserRoleBindDTO dto) {
        return relationCoordinator().batchBindUserRoles(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserRoles(Long userId, Long[] roleIds) {
        return relationCoordinator().unbindUserRoles(userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        return relationCoordinator().bindUserOrg(userId, orgId, isMain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserOrg(Long userId, Long orgId) {
        return relationCoordinator().unbindUserOrg(userId, orgId);
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId) {
        return selectUserRoleIds(userId, null);
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId, Long tenantId) {
        return relationCoordinator().selectUserRoleIds(userId, tenantId);
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId) {
        return selectUserOrgIds(userId, null);
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId, Long tenantId) {
        return relationCoordinator().selectUserOrgIds(userId, tenantId);
    }

    @Override
    public List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId) {
        return relationCoordinator().selectUserOrgBindings(userId, tenantId);
    }

    @Override
    public List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId) {
        return relationCoordinator().selectUserOrgRoleIds(userId, orgId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId) {
        return relationCoordinator().bindUserOrgRoles(userId, orgId, roleIds, tenantId);
    }

    @Override
    public List<SysUserTenantVO> selectUserTenants(Long userId) {
        return relationCoordinator().selectUserTenants(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        return relationCoordinator().bindUserTenants(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserTenant(BatchUserTenantBindDTO dto) {
        return relationCoordinator().batchBindUserTenant(dto);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId) {
        return bindUserOrgs(userId, orgIds, mainOrgId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId) {
        return relationCoordinator().bindUserOrgs(userId, orgIds, mainOrgId, requestedTenantId);
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
        assertNotSelfManagementUnlessAdmin(userId);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(PasswordUtil.encrypt(newPassword));
        user.setForcePasswordChange(true);
        return TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
    }

    @Override
    public boolean updateUserStatus(Long userId, Integer status) {
        assertCanManageUser(userId);
        assertNotSelfManagementUnlessAdmin(userId);
        accessPolicy().assertCanChangeUserStatus(userId, status);
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
        accessPolicy().normalizeUserQueryTenant(query);
    }

    private LoginUser requireLoginUser() {
        return accessPolicy().requireLoginUser();
    }

    private Long resolveWriteTenantId(Long requestedTenantId) {
        return accessPolicy().resolveWriteTenantId(requestedTenantId);
    }

    private void validateUserTypeForWrite(SysUserDTO dto) {
        accessPolicy().validateUserTypeForWrite(dto);
    }

    private Integer resolveWriteUserType(Integer requestedUserType) {
        return accessPolicy().resolveWriteUserType(requestedUserType);
    }

    private void assertCanManageUser(Long userId) {
        accessPolicy().assertCanManageUser(userId);
    }

    private void assertCanReadUser(Long userId) {
        accessPolicy().assertCanReadUser(userId);
    }

    private void assertUserManagementAllowed() {
        accessPolicy().assertUserManagementAllowed();
    }

    private void assertNotSelfManagementUnlessAdmin(Long userId) {
        accessPolicy().assertNotSelfManagementUnlessAdmin(userId);
    }

    private boolean isCurrentLoginUser(Long userId, LoginUser loginUser) {
        return accessPolicy().isCurrentLoginUser(userId, loginUser);
    }

    private SysUserAccessPolicy accessPolicy() {
        return new SysUserAccessPolicy(
                userMapper, userTenantMapper, tenantMapper, assignmentPolicy());
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

    private SysUserAssignmentPolicy assignmentPolicy() {
        return new SysUserAssignmentPolicy(userMapper, userTenantMapper, roleMapper, roleOrgMapper,
                userOrgMapper, orgMapper, postMapper);
    }

    private SysUserRelationCoordinator relationCoordinator() {
        return new SysUserRelationCoordinator(
                userMapper, userRoleMapper, userOrgRoleMapper, userOrgMapper,
                userPostMapper, userTenantMapper, roleMapper, orgMapper,
                accessPolicy(), assignmentPolicy(),
                this::syncCurrentUserOrgSession, this::syncUserRegionFromMainOrg);
    }

    private boolean syncUserRoles(Long userId, List<Long> roleIds, Long requestedTenantId) {
        return relationCoordinator().syncUserRoles(userId, roleIds, requestedTenantId);
    }

    private void upsertUserTenant(Long userId, Long tenantId, Integer memberType, boolean defaultTenant) {
        relationCoordinator().upsertUserTenant(userId, tenantId, memberType, defaultTenant);
    }

    /**
     * 用户类型变更后，同步该用户在各租户的成员类型，保持 user_type 与 member_type 口径一致。
     */
    private void syncMemberTypeFromUserType(Long userId, Integer userType) {
        relationCoordinator().syncMemberTypeFromUserType(userId, userType);
    }

    private boolean removeUserFromTenant(Long userId, Long tenantId) {
        return relationCoordinator().removeUserFromTenant(userId, tenantId);
    }

    private boolean deleteUserGlobally(Long userId) {
        return relationCoordinator().deleteUserGlobally(userId);
    }

    @Override
    public List<Long> selectUserPostIds(Long userId) {
        return selectUserPostIds(userId, null);
    }

    @Override
    public List<Long> selectUserPostIds(Long userId, Long tenantId) {
        return relationCoordinator().selectUserPostIds(userId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId) {
        return bindUserPosts(userId, postIds, mainPostId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long requestedTenantId) {
        return relationCoordinator().bindUserPosts(userId, postIds, mainPostId, requestedTenantId);
    }
}
