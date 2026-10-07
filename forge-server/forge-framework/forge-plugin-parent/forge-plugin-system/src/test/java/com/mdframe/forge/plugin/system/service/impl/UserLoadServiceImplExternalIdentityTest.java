package com.mdframe.forge.plugin.system.service.impl;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoadServiceImplExternalIdentityTest {

    @Test
    void shouldRejectVerifiedPhoneWhenMultipleEligibleUsersExist() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserLoadServiceImpl service = new UserLoadServiceImpl(
                userMapper, null, null, null, null, null, null, null, null,
                null, null, null, null);
        when(userMapper.selectEligibleUsersByVerifiedPhone("13800000000", 1L))
                .thenReturn(List.of(user(101L), user(102L)));

        assertThatThrownBy(() -> service.loadUniqueUserByVerifiedPhone(
                "13800000000", 1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("手机号未唯一匹配有效用户");
        verify(userMapper).selectEligibleUsersByVerifiedPhone("13800000000", 1L);
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        return user;
    }
}
