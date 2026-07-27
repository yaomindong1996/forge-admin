package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysTenantDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantQuery;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.service.ISysTenantService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 租户Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenant> implements ISysTenantService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserTenantMapper userTenantMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgRoleMapper userOrgRoleMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final SysUserPostMapper userPostMapper;
    private final IUserLoadService userLoadService;

    @Override
    public IPage<SysTenant> selectTenantPage(SysTenantQuery query) {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getTenantName()), SysTenant::getTenantName, query.getTenantName())
                .like(StringUtils.isNotBlank(query.getContactPerson()), SysTenant::getContactPerson, query.getContactPerson())
                .eq(query.getTenantStatus() != null, SysTenant::getTenantStatus, query.getTenantStatus())
                .orderByDesc(SysTenant::getCreateTime);
        if (!loginUser.isAdmin()) {
            wrapper.eq(SysTenant::getId, loginUser.getTenantId());
        }

        Page<SysTenant> page = new Page<>(query.getPageNum(), query.getPageSize());
        return tenantMapper.selectPage(page, wrapper);
    }

    @Override
    public SysTenant selectTenantById(Long id) {
        assertCanAccessTenant(id);
        return tenantMapper.selectById(id);
    }
    
    @Override
    public SysTenant selectUserTenantConfig(Long tenantId) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        Long targetTenantId = tenantId != null ? tenantId : loginUser.getTenantId();
        assertCanAccessTenant(targetTenantId);
        return tenantMapper.selectById(targetTenantId);
    }

    @Override
    public SysTenant selectEnabledTenantForLogin(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return TenantContextHolder.executeIgnore(() -> tenantMapper.selectEnabledTenantById(tenantId));
    }

    @Override
    public List<SysUserTenantVO> selectCurrentUserTenants() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return TenantContextHolder.executeIgnore(() -> tenantMapper.selectList(
                            new LambdaQueryWrapper<SysTenant>()
                                    .eq(SysTenant::getTenantStatus, 1)
                                    .orderByAsc(SysTenant::getId)))
                    .stream()
                    .map(tenant -> {
                        SysUserTenantVO vo = new SysUserTenantVO();
                        vo.setUserId(loginUser.getUserId());
                        vo.setTenantId(tenant.getId());
                        vo.setTenantName(tenant.getTenantName());
                        vo.setTenantStatus(tenant.getTenantStatus());
                        vo.setExpireTime(tenant.getExpireTime());
                        vo.setMemberType(1);
                        vo.setIsDefault(tenant.getId().equals(loginUser.getTenantId()) ? 1 : 0);
                        vo.setStatus(1);
                        return vo;
                    })
                    .toList();
        }
        return TenantContextHolder.executeIgnore(() -> userTenantMapper.selectUserTenants(loginUser.getUserId(), true));
    }

    @Override
    public List<SysTenant> selectAssignableTenantOptions() {
        LoginUser loginUser = requireLoginUser();
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysTenant::getTenantStatus, 1)
                .orderByAsc(SysTenant::getId);
        if (!loginUser.isAdmin()) {
            wrapper.eq(SysTenant::getId, loginUser.getTenantId());
        }
        return TenantContextHolder.executeIgnore(() -> tenantMapper.selectList(wrapper));
    }

    @Override
    public IPage<SysUser> selectTenantUsers(Long tenantId, SysUserQuery query) {
        assertCanAccessTenant(tenantId);
        SysUserQuery userQuery = query == null ? new SysUserQuery() : query;
        userQuery.setTenantId(tenantId);
        Page<SysUser> page = new Page<>(userQuery.getPageNum(), userQuery.getPageSize());
        return TenantContextHolder.executeIgnore(() -> userMapper.selectUserPage(page, userQuery));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTenantUser(Long tenantId, Long userId) {
        if (tenantId == null) {
            throw new RuntimeException("租户ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        assertCanAccessTenant(tenantId);

        LoginUser loginUser = requireLoginUser();
        if (Objects.equals(loginUser.getUserId(), userId) && Objects.equals(loginUser.getTenantId(), tenantId)) {
            throw new RuntimeException("不能将当前登录用户移出当前租户");
        }

        SysUser user = TenantContextHolder.executeIgnore(() -> userMapper.selectById(userId));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        SysUserTenant binding = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectOne(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .last("LIMIT 1")));
        if (binding == null) {
            throw new RuntimeException("用户未绑定该租户");
        }
        assertTenantUserRemovable(loginUser, user, binding, tenantId);

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
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                    .eq(SysUserPost::getUserId, userId)
                    .eq(SysUserPost::getTenantId, tenantId));

            int deleted = userTenantMapper.delete(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getTenantId, tenantId));
            if (deleted <= 0) {
                return false;
            }

            List<SysUserTenant> remainingBindings = userTenantMapper.selectList(new LambdaQueryWrapper<SysUserTenant>()
                    .eq(SysUserTenant::getUserId, userId)
                    .eq(SysUserTenant::getStatus, 1)
                    .orderByDesc(SysUserTenant::getIsDefault)
                    .orderByAsc(SysUserTenant::getTenantId));
            if (remainingBindings.isEmpty()) {
                userMapper.deleteById(userId);
                return true;
            }

            SysUserTenant nextDefault = remainingBindings.stream()
                    .filter(item -> Objects.equals(item.getIsDefault(), 1))
                    .findFirst()
                    .orElse(remainingBindings.get(0));
            if (!Objects.equals(nextDefault.getIsDefault(), 1)) {
                SysUserTenant resetDefault = new SysUserTenant();
                resetDefault.setIsDefault(0);
                userTenantMapper.update(resetDefault, new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId));

                nextDefault.setIsDefault(1);
                userTenantMapper.updateById(nextDefault);
            }

            if (Objects.equals(binding.getIsDefault(), 1) || Objects.equals(user.getTenantId(), tenantId)) {
                SysUser updateUser = new SysUser();
                updateUser.setId(userId);
                updateUser.setTenantId(nextDefault.getTenantId());
                userMapper.updateById(updateUser);
            }
            return true;
        });
    }

    @Override
    public LoginUser switchTenant(Long tenantId) {
        LoginUser currentUser = requireLoginUser();
        validateTenantAvailable(tenantId);
        if (!currentUser.isAdmin() && !isUserBoundTenant(currentUser.getUserId(), tenantId)) {
            throw new RuntimeException("用户未绑定该租户");
        }
        LoginUser switchedUser = userLoadService.loadUserByUserId(currentUser.getUserId(), tenantId);
        switchedUser.setLoginTime(currentUser.getLoginTime());
        switchedUser.setLoginIp(currentUser.getLoginIp());
        switchedUser.setUserClient(currentUser.getUserClient());
        SessionHelper.setLoginUser(switchedUser);
        TenantContextHolder.setTenantId(tenantId);
        return switchedUser;
    }
    
    @Override
    public boolean insertTenant(SysTenantDTO dto) {
        assertSuperAdmin();
        SysTenant tenant = new SysTenant();
        BeanUtil.copyProperties(dto, tenant);
        return tenantMapper.insert(tenant) > 0;
    }

    @Override
    public boolean updateTenant(SysTenantDTO dto) {
        assertCanAccessTenant(dto.getId());
        SysTenant tenant = new SysTenant();
        BeanUtil.copyProperties(dto, tenant);
        return tenantMapper.updateById(tenant) > 0;
    }

    @Override
    public boolean deleteTenantById(Long id) {
        assertSuperAdmin();
        validateTenantDeletable(id);
        return tenantMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteTenantByIds(Long[] ids) {
        assertSuperAdmin();
        if (ids == null || ids.length == 0) {
            return false;
        }
        Arrays.stream(ids).forEach(this::validateTenantDeletable);
        return tenantMapper.deleteBatchIds(Arrays.asList(ids)) > 0;
    }

    private void validateTenantDeletable(Long id) {
        if (id == null) {
            throw new RuntimeException("租户ID不能为空");
        }
        if (DEFAULT_TENANT_ID.equals(id)) {
            throw new RuntimeException("默认租户不能删除");
        }
        Long userCount = TenantContextHolder.executeIgnore(() -> tenantMapper.countUsersByTenant(id));
        if (userCount != null && userCount > 0) {
            throw new RuntimeException("租户下已存在用户，不能删除");
        }
        Long bindingCount = TenantContextHolder.executeIgnore(() -> tenantMapper.countUserTenantBindings(id));
        if (bindingCount != null && bindingCount > 0) {
            throw new RuntimeException("租户下已绑定用户，不能删除");
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private void assertSuperAdmin() {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin()) {
            throw new RuntimeException("只有超级管理员可以操作租户");
        }
    }

    private void assertCanAccessTenant(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户ID不能为空");
        }
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        if (!tenantId.equals(loginUser.getTenantId())) {
            throw new RuntimeException("无权访问该租户");
        }
    }

    private void validateTenantAvailable(Long tenantId) {
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
    }

    private boolean isUserBoundTenant(Long userId, Long tenantId) {
        Long count = TenantContextHolder.executeIgnore(() ->
                userTenantMapper.selectCount(new LambdaQueryWrapper<SysUserTenant>()
                        .eq(SysUserTenant::getUserId, userId)
                        .eq(SysUserTenant::getTenantId, tenantId)
                        .eq(SysUserTenant::getStatus, 1)));
        return count != null && count > 0;
    }

    private void assertTenantUserRemovable(LoginUser loginUser, SysUser user, SysUserTenant binding, Long tenantId) {
        if (loginUser == null || loginUser.isAdmin()) {
            return;
        }
        if (user.getUserType() != null && user.getUserType() == 0) {
            throw new RuntimeException("无权操作超级管理员");
        }
        if (!Objects.equals(loginUser.getTenantId(), tenantId)) {
            throw new RuntimeException("无权访问该租户");
        }
        if (binding.getMemberType() != null && binding.getMemberType() != 2) {
            throw new RuntimeException("租户管理员只能维护普通用户");
        }
    }
}
