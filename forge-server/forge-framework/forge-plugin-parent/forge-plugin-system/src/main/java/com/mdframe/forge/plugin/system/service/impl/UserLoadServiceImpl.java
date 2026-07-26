package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.auth.util.PasswordUtil;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户加载服务实现
 * 专门用于认证策略调用，避免循环依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoadServiceImpl implements IUserLoadService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysTenantMapper tenantMapper;
    private final SysRoleResourceMapper roleResourceMapper;
    private final SysResourceMapper resourceMapper;
    private final ICaptchaService captchaService;
    
    private final SysOrgMapper sysOrgMapper;
    
    private final SysRegionMapper regionMapper;
    
    @Override
    public LoginUser loadUserByUsername(String username, Long tenantId) {
        return loadUserByUsername(username, tenantId, null);
    }

    @Override
    public LoginUser loadUserByUsername(String username, Long tenantId, Long preferredActiveOrgId) {
        SysUser user = TenantContextHolder.executeIgnore(() ->
                userMapper.selectByUsernameForLogin(username, tenantId));
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return buildLoginUser(user, resolveEffectiveTenantId(user, tenantId), preferredActiveOrgId);
    }

    @Override
    public LoginUser loadUserByPhone(String phone, Long tenantId) {
        return loadUserByPhone(phone, tenantId, null);
    }

    @Override
    public LoginUser loadUserByPhone(String phone, Long tenantId, Long preferredActiveOrgId) {
        SysUser user = TenantContextHolder.executeIgnore(() ->
                userMapper.selectByPhoneForLogin(phone, tenantId));
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return buildLoginUser(user, resolveEffectiveTenantId(user, tenantId), preferredActiveOrgId);
    }

    @Override
    public LoginUser loadUserByEmail(String email, Long tenantId) {
        return loadUserByEmail(email, tenantId, null);
    }

    @Override
    public LoginUser loadUserByEmail(String email, Long tenantId, Long preferredActiveOrgId) {
        SysUser user = TenantContextHolder.executeIgnore(() ->
                userMapper.selectByEmailForLogin(email, tenantId));
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return buildLoginUser(user, resolveEffectiveTenantId(user, tenantId), preferredActiveOrgId);
    }

    @Override
    public LoginUser loadUserByUserId(Long userId, Long tenantId) {
        return loadUserByUserId(userId, tenantId, null);
    }

    @Override
    public LoginUser loadUserByUserId(Long userId, Long tenantId, Long preferredActiveOrgId) {
        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return buildLoginUser(user, resolveEffectiveTenantId(user, tenantId), preferredActiveOrgId);
    }

    @Override
    public String getUserPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user.getPassword();
    }

    @Override
    public boolean matchPassword(String rawPassword, String encodedPassword) {
        return PasswordUtil.matches(rawPassword, encodedPassword);
    }

    @Override
    public boolean validateCode(String codeKey, String code) {
        return captchaService.validateAndDelete(codeKey, code);
    }

    @Override
    public boolean validatePhoneCode(String phone, String code) {
        return captchaService.validateAndDeleteSmsCaptcha(phone, code);
    }

    /**
     * 构建LoginUser（包含角色、权限、组织）
     */
    private LoginUser buildLoginUser(SysUser user, Long effectiveTenantId, Long preferredActiveOrgId) {
        SysUserTenant tenantMember = validateTenantMembership(user, effectiveTenantId);
        SysTenant tenant = tenantMapper.selectById(effectiveTenantId);

        // 1. 构建基本信息
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setTenantId(effectiveTenantId);
        loginUser.setTenantName(tenant != null ? tenant.getTenantName() : null);
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setUserType(resolveEffectiveUserType(user, tenantMember));
        loginUser.setPhone(user.getPhone());
        loginUser.setEmail(user.getEmail());
        loginUser.setAvatar(user.getAvatar());
        loginUser.setUserStatus(user.getUserStatus());
        loginUser.setForcePasswordChange(Boolean.TRUE.equals(user.getForcePasswordChange()));
        loginUser.setCreateTime(user.getCreateTime());
        loginUser.setTenantIds(loadAvailableTenantIds(user));

        TenantContextHolder.executeWithTenant(effectiveTenantId, () -> {
            // 2. 加载用户组织并确定当前组织
            loadUserOrgs(loginUser, preferredActiveOrgId);

            // 3. 加载当前组织下的用户角色
            loadUserRoles(loginUser);

            // 4. 加载用户权限（按钮权限）
            loadUserPermissions(loginUser);

            // 5. 加载API接口权限（缓存到Session）
            loadApiPermissions(loginUser);

            // 6. 加载用户行政区划
            loadUserRegion(loginUser);
        });

        return loginUser;
    }

    /**
     * 加载用户角色
     */
    private void loadUserRoles(LoginUser loginUser) {
        if (loginUser.getActiveOrgId() == null) {
            loginUser.setRoleIds(new ArrayList<>());
            loginUser.setRoleKeys(new HashSet<>());
            log.warn("用户没有当前组织，无法加载组织内角色: userId={}, tenantId={}",
                    loginUser.getUserId(), loginUser.getTenantId());
            return;
        }

        List<Long> roleIds = userOrgRoleMapper.selectActiveRoleIdsByUserOrg(
                loginUser.getTenantId(), loginUser.getUserId(), loginUser.getActiveOrgId());
        if (CollUtil.isEmpty(roleIds)) {
            loginUser.setRoleIds(new ArrayList<>());
            loginUser.setRoleKeys(new HashSet<>());
            log.warn("用户当前组织没有分配角色: userId={}, tenantId={}, activeOrgId={}",
                    loginUser.getUserId(), loginUser.getTenantId(), loginUser.getActiveOrgId());
            return;
        }

        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getTenantId, loginUser.getTenantId())
                .eq(SysRole::getRoleStatus, 1));

        if (CollUtil.isEmpty(roles)) {
            loginUser.setRoleIds(new ArrayList<>());
            loginUser.setRoleKeys(new HashSet<>());
            log.warn("用户当前组织没有启用角色: userId={}, tenantId={}, activeOrgId={}",
                    loginUser.getUserId(), loginUser.getTenantId(), loginUser.getActiveOrgId());
            return;
        }

        List<Long> activeRoleIds = roles.stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());
        Set<String> roleKeys = roles.stream()
                .map(SysRole::getRoleKey)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        loginUser.setRoleIds(activeRoleIds);
        loginUser.setRoleKeys(roleKeys);

        log.debug("加载用户当前组织角色: userId={}, tenantId={}, activeOrgId={}, roleIds={}, roleKeys={}",
                loginUser.getUserId(), loginUser.getTenantId(), loginUser.getActiveOrgId(), activeRoleIds, roleKeys);
    }

    /**
     * 加载用户组织
     */
    private void loadUserOrgs(LoginUser loginUser, Long preferredActiveOrgId) {
        LambdaQueryWrapper<SysUserOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserOrg::getUserId, loginUser.getUserId())
                .eq(SysUserOrg::getTenantId, loginUser.getTenantId());
        List<SysUserOrg> userOrgs = userOrgMapper.selectList(wrapper);

        if (loginUser.isAdmin() && CollUtil.isEmpty(userOrgs)) {
            loadAdminTenantOrgs(loginUser, preferredActiveOrgId);
            return;
        }

        if (CollUtil.isNotEmpty(userOrgs)) {
            List<Long> orgIds = userOrgs.stream()
                    .map(SysUserOrg::getOrgId)
                    .collect(Collectors.toList());
            loginUser.setOrgIds(orgIds);

            Long mainOrgId = userOrgs.stream()
                    .filter(uo -> uo.getIsMain() != null && uo.getIsMain() == 1)
                    .findFirst()
                    .map(SysUserOrg::getOrgId)
                    .orElse(null);
            loginUser.setMainOrgId(mainOrgId);

            Long activeOrgId = resolveActiveOrgId(orgIds, mainOrgId, preferredActiveOrgId);
            loginUser.setActiveOrgId(activeOrgId);
            if (activeOrgId != null) {
                SysOrg sysOrg = sysOrgMapper.selectById(activeOrgId);
                if (sysOrg != null) {
                    loginUser.setActiveOrgName(sysOrg.getOrgName());
                    loginUser.setDeptName(sysOrg.getOrgName());
                }
            }
        } else {
            loginUser.setOrgIds(new ArrayList<>());
        }
    }

    private void loadAdminTenantOrgs(LoginUser loginUser, Long preferredActiveOrgId) {
        List<SysOrg> orgs = sysOrgMapper.selectList(new LambdaQueryWrapper<SysOrg>()
                .eq(SysOrg::getTenantId, loginUser.getTenantId())
                .eq(SysOrg::getOrgStatus, 1)
                .orderByAsc(SysOrg::getSort)
                .orderByAsc(SysOrg::getId));
        if (CollUtil.isEmpty(orgs)) {
            loginUser.setOrgIds(new ArrayList<>());
            return;
        }

        List<Long> orgIds = orgs.stream().map(SysOrg::getId).collect(Collectors.toList());
        Long activeOrgId = resolveActiveOrgId(orgIds, null, preferredActiveOrgId);
        loginUser.setOrgIds(orgIds);
        loginUser.setMainOrgId(activeOrgId);
        loginUser.setActiveOrgId(activeOrgId);
        orgs.stream()
                .filter(org -> org.getId().equals(activeOrgId))
                .findFirst()
                .ifPresent(org -> {
                    loginUser.setActiveOrgName(org.getOrgName());
                    loginUser.setDeptName(org.getOrgName());
                });
    }

    private Long resolveActiveOrgId(List<Long> orgIds, Long mainOrgId, Long preferredActiveOrgId) {
        if (CollUtil.isEmpty(orgIds)) {
            return null;
        }
        if (preferredActiveOrgId != null && orgIds.contains(preferredActiveOrgId)) {
            return preferredActiveOrgId;
        }
        if (mainOrgId != null && orgIds.contains(mainOrgId)) {
            return mainOrgId;
        }
        return orgIds.get(0);
    }

    /**
     * 加载用户权限（按钮权限）
     */
    private void loadUserPermissions(LoginUser loginUser) {
        if (loginUser.isAdmin()) {
            Set<String> permissions = new HashSet<>();
            permissions.add("*:*:*");
            loginUser.setPermissions(permissions);
            log.debug("超级管理员拥有所有权限: userId={}", loginUser.getUserId());
            return;
        }
        
        List<Long> roleIds = loginUser.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            log.warn("用户没有角色，无法加载权限: userId={}", loginUser.getUserId());
            return;
        }

        LambdaQueryWrapper<SysRoleResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysRoleResource::getRoleId, roleIds)
                .eq(SysRoleResource::getTenantId, loginUser.getTenantId());
        List<SysRoleResource> roleResources = roleResourceMapper.selectList(wrapper);

        if (CollUtil.isNotEmpty(roleResources)) {
            List<Long> resourceIds = roleResources.stream()
                    .map(SysRoleResource::getResourceId)
                    .distinct()
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(resourceIds)) {
                LambdaQueryWrapper<SysResource> resourceWrapper = new LambdaQueryWrapper<>();
                resourceWrapper.in(SysResource::getId, resourceIds)
                        .eq(SysResource::getVisible, 1)
                        .isNotNull(SysResource::getPerms);
                applyUserTypeScope(resourceWrapper, loginUser);
                List<SysResource> resources = resourceMapper.selectList(resourceWrapper);

                if (CollUtil.isNotEmpty(resources)) {
                    Set<String> permissions = resources.stream()
                            .map(SysResource::getPerms)
                            .filter(StrUtil::isNotBlank)
                            .collect(Collectors.toSet());
                    loginUser.setPermissions(permissions);
                    
                    log.debug("加载用户权限: userId={}, permissionCount={}, permissions={}",
                            loginUser.getUserId(), permissions.size(), permissions);
                } else {
                    log.warn("用户角色没有分配有效权限: userId={}", loginUser.getUserId());
                }
            }
        } else {
            log.warn("用户角色没有分配资源: userId={}", loginUser.getUserId());
        }
    }

    /**
     * 加载API接口权限（缓存到LoginUser中）
     */
    private void loadApiPermissions(LoginUser loginUser) {
        // 1. 超级管理员拥有所有API权限
        if (loginUser.isAdmin()) {
            List<String> apiPermissions = new ArrayList<>();
            apiPermissions.add("/**");  // 匹配所有接口
            loginUser.setApiPermissions(apiPermissions);
            log.debug("超级管理员拥有所有API权限: userId={}", loginUser.getUserId());
            return;
        }

        // 2. 获取用户的角色ID列表
        List<Long> roleIds = loginUser.getRoleIds();
        if (CollUtil.isEmpty(roleIds)) {
            log.warn("获取API权限失败: 用户没有角色, userId={}", loginUser.getUserId());
            loginUser.setApiPermissions(new ArrayList<>());
            return;
        }

        // 3. 查询角色关联的资源ID列表
        LambdaQueryWrapper<SysRoleResource> roleResourceWrapper = new LambdaQueryWrapper<>();
        roleResourceWrapper.in(SysRoleResource::getRoleId, roleIds)
                .eq(SysRoleResource::getTenantId, loginUser.getTenantId());
        List<SysRoleResource> roleResources = roleResourceMapper.selectList(roleResourceWrapper);

        if (CollUtil.isEmpty(roleResources)) {
            log.warn("获取API权限失败: 角色没有分配资源, userId={}, roleIds={}",
                    loginUser.getUserId(), roleIds);
            loginUser.setApiPermissions(new ArrayList<>());
            return;
        }

        List<Long> resourceIds = roleResources.stream()
                .map(SysRoleResource::getResourceId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 查询当前用户类型可访问的API资源（resourceType=4）
        List<SysResource> apiResources = resourceMapper.selectList(new LambdaQueryWrapper<SysResource>()
                .in(SysResource::getId, resourceIds)
                .eq(SysResource::getVisible, 1)
                .eq(SysResource::getResourceType, 4)
                .isNotNull(SysResource::getApiUrl));
        apiResources = apiResources.stream()
                .filter(resource -> canAccessByUserType(loginUser, resource.getMinUserType()))
                .collect(Collectors.toList());
        List<String> apiPermissions = CollUtil.isEmpty(apiResources)
                ? new ArrayList<>()
                : resourceMapper.selectApiPermissionPatternsByResourceIds(apiResources.stream()
                        .map(SysResource::getId)
                        .collect(Collectors.toList()));

        if (CollUtil.isEmpty(apiPermissions)) {
            log.debug("用户没有API权限: userId={}", loginUser.getUserId());
            loginUser.setApiPermissions(new ArrayList<>());
            return;
        }

        // 5. 提取apiUrl列表（支持通配符；方法明确时格式为 METHOD path）
        List<String> apiUrls = apiPermissions.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        loginUser.setApiPermissions(apiUrls);

        log.debug("加载用户API权限: userId={}, apiCount={}, apis={}",
                loginUser.getUserId(), apiUrls.size(), apiUrls);
    }

    private void applyUserTypeScope(LambdaQueryWrapper<SysResource> wrapper, LoginUser loginUser) {
        int userType = normalizeUserType(loginUser == null ? null : loginUser.getUserType());
        wrapper.and(item -> item
                .isNull(SysResource::getMinUserType)
                .or()
                .ge(SysResource::getMinUserType, userType));
    }

    private boolean canAccessByUserType(LoginUser loginUser, Integer minUserType) {
        int userType = normalizeUserType(loginUser == null ? null : loginUser.getUserType());
        return minUserType == null || minUserType >= userType;
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

    private Long resolveEffectiveTenantId(SysUser user, Long requestedTenantId) {
        if (requestedTenantId != null) {
            return requestedTenantId;
        }
        if (user.getTenantId() != null) {
            return user.getTenantId();
        }
        return 1L;
    }

    private SysUserTenant validateTenantMembership(SysUser user, Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        SysTenant tenant = TenantContextHolder.executeIgnore(() -> tenantMapper.selectById(tenantId));
        if (tenant == null) {
            throw new RuntimeException("租户不存在");
        }
        if (tenant.getTenantStatus() == null || tenant.getTenantStatus() != 1) {
            throw new RuntimeException("租户已禁用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("租户已过期");
        }

        if (user.getUserType() != null && user.getUserType() == 0) {
            return null;
        }

        LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserTenant::getUserId, user.getId())
                .eq(SysUserTenant::getTenantId, tenantId)
                .eq(SysUserTenant::getStatus, 1);
        SysUserTenant member = TenantContextHolder.executeIgnore(() -> userTenantMapper.selectOne(wrapper));
        if (member == null) {
            throw new RuntimeException("用户未绑定该租户");
        }
        return member;
    }

    private Integer resolveEffectiveUserType(SysUser user, SysUserTenant tenantMember) {
        if (user.getUserType() != null && user.getUserType() == 0) {
            return 0;
        }
        if (tenantMember != null && tenantMember.getMemberType() != null && tenantMember.getMemberType() == 1) {
            return 1;
        }
        return 2;
    }

    private List<Long> loadAvailableTenantIds(SysUser user) {
        if (user.getUserType() != null && user.getUserType() == 0) {
            return TenantContextHolder.executeIgnore(() -> tenantMapper.selectList(
                            new LambdaQueryWrapper<SysTenant>()
                                    .eq(SysTenant::getTenantStatus, 1)
                                    .select(SysTenant::getId)))
                    .stream()
                    .map(SysTenant::getId)
                    .collect(Collectors.toList());
        }
        LambdaQueryWrapper<SysUserTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserTenant::getUserId, user.getId())
                .eq(SysUserTenant::getStatus, 1)
                .select(SysUserTenant::getTenantId);
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectList(wrapper))
                .stream()
                .map(SysUserTenant::getTenantId)
                .collect(Collectors.toList());
    }

    /**
     * 加载用户行政区划信息
     * 优先级：用户有组织且组织有regionCode -> 取组织的regionCode
     * 用户有组织但组织无regionCode -> 取用户的regionCode（fallback）
     * 用户无组织 -> 取用户的regionCode
     */
    private void loadUserRegion(LoginUser loginUser) {
        String regionCode = null;
        
        // 优先从当前组织获取
        Long regionOrgId = loginUser.getActiveOrgId() != null ? loginUser.getActiveOrgId() : loginUser.getMainOrgId();
        if (regionOrgId != null) {
            SysOrg org = sysOrgMapper.selectById(regionOrgId);
            if (org != null && StrUtil.isNotBlank(org.getRegionCode())) {
                regionCode = org.getRegionCode();
            }
        }
        
        // 组织无regionCode或无组织，从用户自身获取
        if (StrUtil.isBlank(regionCode)) {
            SysUser user = userMapper.selectById(loginUser.getUserId());
            if (user != null && StrUtil.isNotBlank(user.getRegionCode())) {
                regionCode = user.getRegionCode();
            }
        }
        
        // 根据regionCode查询完整信息
        if (StrUtil.isNotBlank(regionCode)) {
            SysRegion region = regionMapper.selectById(regionCode);
            if (region != null) {
                loginUser.setRegionCode(region.getCode());
                loginUser.setRegionName(region.getName());
                loginUser.setRegionLevel(region.getLevel());
                loginUser.setRegionFullName(region.getFullName());
                loginUser.setRegionAncestors(buildRegionAncestors(region));
                
                log.debug("加载用户行政区划: userId={}, regionCode={}, regionName={}",
                        loginUser.getUserId(), regionCode, region.getName());
            }
        }
    }

    /**
     * 构建行政区划祖级编码
     */
    private String buildRegionAncestors(SysRegion region) {
        StringBuilder ancestors = new StringBuilder();
        String currentCode = region.getCode();
        while (StrUtil.isNotBlank(currentCode)) {
            SysRegion currentRegion = regionMapper.selectById(currentCode);
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
}
