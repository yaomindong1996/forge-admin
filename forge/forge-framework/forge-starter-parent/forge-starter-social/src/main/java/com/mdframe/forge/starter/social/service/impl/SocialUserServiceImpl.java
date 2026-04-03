package com.mdframe.forge.starter.social.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import com.mdframe.forge.starter.social.mapper.SysUserSocialMapper;
import com.mdframe.forge.starter.social.service.ISocialUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 三方用户绑定服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserServiceImpl extends ServiceImpl<SysUserSocialMapper, SysUserSocial>
        implements ISocialUserService {

    @Override
    public SysUserSocial selectByPlatformAndUuid(String platform, String uuid) {
        return this.lambdaQuery()
                .eq(SysUserSocial::getPlatform, platform)
                .eq(SysUserSocial::getUuid, uuid).last("limit 1").one();
    }

    @Override
    public List<SysUserSocial> selectByUserId(Long userId) {
        return this.lambdaQuery()
                .eq(SysUserSocial::getUserId, userId).list();
    }

    @Override
    public SysUserSocial selectByUserIdAndPlatform(Long userId, String platform) {
        return this.lambdaQuery()
                .eq(SysUserSocial::getUserId, userId)
                .eq(SysUserSocial::getPlatform, platform).last("limit 1").one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindSocialUser(Long userId, AuthUser authUser, String platform, Long tenantId) {
        SysUserSocial existing = selectByUserIdAndPlatform(userId, platform);
        if (existing != null) {
            log.warn("用户已绑定该平台: userId={}, platform={}", userId, platform);
            return false;
        }

        SysUserSocial userSocial = new SysUserSocial();
        userSocial.setUserId(userId);
        userSocial.setPlatform(platform);
        userSocial.setUuid(authUser.getUuid());
        userSocial.setUsername(authUser.getUsername());
        userSocial.setNickname(authUser.getNickname());
        userSocial.setAvatar(authUser.getAvatar());
        userSocial.setEmail(authUser.getEmail());
        userSocial.setAccessToken(authUser.getToken() != null ? authUser.getToken().getAccessToken() : null);
        userSocial.setRefreshToken(authUser.getToken() != null ? authUser.getToken().getRefreshToken() : null);
        if (authUser.getToken() != null && authUser.getToken().getExpireIn() > 0) {
            userSocial.setExpireTime(LocalDateTime.now().plusSeconds(authUser.getToken().getExpireIn()));
        }
        userSocial.setBindTime(LocalDateTime.now());
        userSocial.setTenantId(tenantId);

        return this.save(userSocial);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindSocialUser(Long userId, String platform) {
        LambdaQueryWrapper<SysUserSocial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserSocial::getUserId, userId);
        wrapper.eq(SysUserSocial::getPlatform, platform);
        return this.remove(wrapper);
    }

    @Override
    public boolean updateSocialUser(SysUserSocial userSocial) {
        return this.updateById(userSocial);
    }
}
