package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.constant.SystemConstants;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.entity.SysUserOrgRole;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import com.mdframe.forge.plugin.system.entity.SysUserTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.starter.core.session.LoginUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserRelationCoordinatorTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        // 只初始化 Mock Mapper 的列缓存，不连接数据库，保留真实 Wrapper/权限断言。
        ResourceFeatureTestSupport.initializeMetadata(SysUserPost.class, SysUserRole.class,
                SysUserOrgRole.class, SysUserOrg.class, SysUserTenant.class);
    }

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
    private final SysUserOrgRoleMapper userOrgRoleMapper = mock(SysUserOrgRoleMapper.class);
    private final SysUserOrgMapper userOrgMapper = mock(SysUserOrgMapper.class);
    private final SysUserPostMapper userPostMapper = mock(SysUserPostMapper.class);
    private final SysUserTenantMapper userTenantMapper = mock(SysUserTenantMapper.class);
    private final SysUserAccessPolicy accessPolicy = mock(SysUserAccessPolicy.class);
    private final SysUserAssignmentPolicy assignmentPolicy = mock(SysUserAssignmentPolicy.class);
    private final SysUserRelationCoordinator coordinator = new SysUserRelationCoordinator(
        userMapper, userRoleMapper, userOrgRoleMapper, userOrgMapper, userPostMapper,
        userTenantMapper, mock(SysRoleMapper.class), mock(SysOrgMapper.class),
        accessPolicy, assignmentPolicy, (userId, tenantId) -> { }, (userId, orgId) -> { });

    @Test
    void rejectsRoleAssignmentOutsideOrdinaryOperatorsOwnRoles() {
        SysUser target = user(7L, 10L);
        when(userMapper.selectById(7L)).thenReturn(target);
        when(accessPolicy.resolveRoleBindTenantId(target, 10L)).thenReturn(10L);
        when(userTenantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        LoginUser operator = loginUser(SystemConstants.UserType.NORMAL_USER, List.of(100L));
        when(accessPolicy.requireLoginUser()).thenReturn(operator);

        RuntimeException error = assertThrows(RuntimeException.class,
            () -> coordinator.syncUserRoles(7L, List.of(200L), 10L));

        assertEquals("权限溢出：不能分配自己没有的角色", error.getMessage());
    }

    @Test
    void postBindingAppliesRequestedDifferenceAndMainPost() {
        SysUser target = user(7L, 10L);
        when(userMapper.selectById(7L)).thenReturn(target);
        when(accessPolicy.resolveTenantScopedOperationTenantId(target, 10L)).thenReturn(10L);
        when(userTenantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        SysUserPost existingMain = userPost(10L, 1);
        SysUserPost removed = userPost(30L, 0);
        when(userPostMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(existingMain, removed));

        assertTrue(coordinator.bindUserPosts(7L, List.of(20L, 20L, 10L), 20L, 10L));

        verify(assignmentPolicy).validatePostTenant(List.of(20L, 10L), 10L);
        verify(userPostMapper).delete(any(LambdaQueryWrapper.class));
        verify(userPostMapper).updateById(existingMain);
        assertEquals(0, existingMain.getIsMain());
        ArgumentCaptor<SysUserPost> inserted = ArgumentCaptor.forClass(SysUserPost.class);
        verify(userPostMapper).insert(inserted.capture());
        assertEquals(20L, inserted.getValue().getPostId());
        assertEquals(1, inserted.getValue().getIsMain());
    }

    @Test
    void tenantRemovalDeletesUserAfterLastEnabledMembership() {
        when(userTenantMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userTenantMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.deleteById(7L)).thenReturn(1);

        assertTrue(coordinator.removeUserFromTenant(7L, 10L));

        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userOrgRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userOrgMapper).delete(any(LambdaQueryWrapper.class));
        verify(userPostMapper).delete(any(LambdaQueryWrapper.class));
        verify(userMapper).deleteById(7L);
    }

    private SysUser user(Long id, Long tenantId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUserType(SystemConstants.UserType.NORMAL_USER);
        return user;
    }

    private LoginUser loginUser(Integer userType, List<Long> roleIds) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserType(userType);
        loginUser.setRoleIds(roleIds);
        return loginUser;
    }

    private SysUserPost userPost(Long postId, int isMain) {
        SysUserPost value = new SysUserPost();
        value.setPostId(postId);
        value.setIsMain(isMain);
        return value;
    }
}
