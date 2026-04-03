package com.mdframe.forge.starter.social.service;

import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import me.zhyd.oauth.model.AuthUser;

import java.util.List;

/**
 * 三方用户绑定服务接口
 */
public interface ISocialUserService {

    /**
     * 根据平台和UUID查询绑定
     */
    SysUserSocial selectByPlatformAndUuid(String platform, String uuid);

    /**
     * 根据用户ID查询所有绑定
     */
    List<SysUserSocial> selectByUserId(Long userId);

    /**
     * 根据用户ID和平台查询绑定
     */
    SysUserSocial selectByUserIdAndPlatform(Long userId, String platform);

    /**
     * 绑定三方用户
     */
    boolean bindSocialUser(Long userId, AuthUser authUser, String platform, Long tenantId);

    /**
     * 解绑三方用户
     */
    boolean unbindSocialUser(Long userId, String platform);

    /**
     * 更新三方用户信息
     */
    boolean updateSocialUser(SysUserSocial userSocial);
}
