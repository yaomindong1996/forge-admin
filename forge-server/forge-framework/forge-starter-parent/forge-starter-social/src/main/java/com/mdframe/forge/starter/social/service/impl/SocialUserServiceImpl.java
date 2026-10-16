package com.mdframe.forge.starter.social.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.starter.collaboration.model.VerifiedSocialIdentity;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import com.mdframe.forge.starter.social.mapper.SysUserSocialMapper;
import com.mdframe.forge.starter.social.service.ISocialUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 三方用户绑定服务实现
 * <p>
 * 企业身份以（租户 + 连接 + 外部用户ID）为唯一维度；新绑定不落库用户 Token。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUserServiceImpl extends ServiceImpl<SysUserSocialMapper, SysUserSocial>
        implements ISocialUserService {

    @Override
    public SysUserSocial selectBinding(Long tenantId, Long connectionId, String externalUserId) {
        if (connectionId == null || StrUtil.isBlank(externalUserId)) {
            return null;
        }
        return baseMapper.selectBinding(tenantId, connectionId, externalUserId);
    }

    @Override
    public SysUserSocial selectBindingByUser(Long tenantId, Long connectionId, Long userId) {
        if (connectionId == null || userId == null) {
            return null;
        }
        return baseMapper.selectBindingByUser(tenantId, connectionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindVerifiedIdentity(VerifiedSocialIdentity identity, Long forgeUserId) {
        if (identity == null || identity.connectionId() == null
                || StrUtil.isBlank(identity.externalUserId()) || forgeUserId == null) {
            throw new BusinessException("已验证身份与Forge用户ID不能为空");
        }
        Long tenantId = identity.tenantId();
        Long connectionId = identity.connectionId();

        // 同连接 Forge 用户唯一：已绑定其他外部身份时失败关闭
        SysUserSocial byUser = baseMapper.selectBindingByUser(tenantId, connectionId, forgeUserId);
        if (byUser != null && !byUser.getUuid().equals(identity.externalUserId())) {
            throw new BusinessException(StrUtil.format("用户已绑定该连接的其他外部身份[{}]", byUser.getUuid()));
        }

        SysUserSocial existing = baseMapper.selectBinding(tenantId, connectionId, identity.externalUserId());
        if (existing != null) {
            if (existing.getUserId() != null && !existing.getUserId().equals(forgeUserId)) {
                // 外部身份已归属其他 Forge 用户，禁止抢绑
                throw new BusinessException(StrUtil.format("外部身份[{}]已绑定其他用户", identity.externalUserId()));
            }
            // 目录同步预置的未关联记录或重复绑定：CAS 关联，零行即并发冲突
            int rows = baseMapper.bindForgeUserCas(existing.getId(), tenantId, forgeUserId);
            if (rows == 0) {
                throw new BusinessException("外部身份绑定发生并发冲突，请重试");
            }
            refreshProfile(existing.getId(), identity);
            return true;
        }

        SysUserSocial binding = new SysUserSocial();
        binding.setTenantId(tenantId);
        binding.setUserId(forgeUserId);
        binding.setPlatform(identity.platform());
        binding.setConnectionId(connectionId);
        binding.setUuid(identity.externalUserId());
        binding.setNickname(identity.nickname());
        binding.setAvatar(identity.avatar());
        binding.setEmail(identity.email());
        binding.setExternalStatus("ACTIVE");
        binding.setManagedBySync(0);
        if (identity.verifiedAt() != null) {
            binding.setBindTime(LocalDateTime.ofInstant(identity.verifiedAt(), ZoneId.systemDefault()));
        } else {
            binding.setBindTime(LocalDateTime.now());
        }
        // 安全红线：不保存用户级 access/refresh token
        try {
            return this.save(binding);
        } catch (DuplicateKeyException e) {
            // uk_user_social_conn_uuid_active / uk_user_social_conn_user_active 拦截并发重复绑定
            throw new BusinessException("外部身份绑定发生并发冲突，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindByConnection(Long tenantId, Long connectionId, Long userId) {
        SysUserSocial binding = baseMapper.selectBindingByUser(tenantId, connectionId, userId);
        if (binding == null) {
            return true;
        }
        // @TableLogic 墓碑删除：del_flag 写当前行主键，允许之后重绑
        return this.removeById(binding.getId());
    }

    @Override
    public List<SysUserSocial> selectByUserId(Long userId) {
        return baseMapper.selectByUserId(null, userId);
    }

    @Override
    @Deprecated
    public SysUserSocial selectByPlatformAndUuid(String platform, String uuid) {
        List<SysUserSocial> bindings = baseMapper.selectByPlatformAndUuid(platform, uuid);
        if (bindings.isEmpty()) {
            return null;
        }
        if (bindings.size() > 1) {
            // 多企业下 platform+uuid 不唯一，失败关闭防止跨企业身份串用
            throw new BusinessException(StrUtil.format("平台[{}]外部身份[{}]存在{}条绑定，请使用连接维度查询",
                    platform, uuid, bindings.size()));
        }
        return bindings.get(0);
    }

    @Override
    @Deprecated
    public SysUserSocial selectByUserIdAndPlatform(Long userId, String platform) {
        List<SysUserSocial> bindings = baseMapper.selectByUserIdAndPlatform(userId, platform);
        if (bindings.isEmpty()) {
            return null;
        }
        if (bindings.size() > 1) {
            throw new BusinessException(StrUtil.format("用户在平台[{}]存在{}条绑定，请使用连接维度查询",
                    platform, bindings.size()));
        }
        return bindings.get(0);
    }

    @Override
    @Deprecated
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
        // 安全红线：不再保存 access/refresh token 明文
        userSocial.setBindTime(LocalDateTime.now());
        userSocial.setTenantId(tenantId);

        return this.save(userSocial);
    }

    @Override
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindSocialUser(Long userId, String platform) {
        List<SysUserSocial> bindings = baseMapper.selectByUserIdAndPlatform(userId, platform);
        if (bindings.isEmpty()) {
            return true;
        }
        // 逻辑删除（@TableLogic 墓碑），保留审计并允许重绑
        return this.removeBatchByIds(bindings.stream().map(SysUserSocial::getId).toList());
    }

    @Override
    public boolean updateSocialUser(SysUserSocial userSocial) {
        return this.updateById(userSocial);
    }

    /**
     * 绑定命中已有记录时回写最新资料（昵称/头像/邮箱），不触碰 Token 字段
     */
    private void refreshProfile(Long bindingId, VerifiedSocialIdentity identity) {
        SysUserSocial update = new SysUserSocial();
        update.setId(bindingId);
        update.setNickname(identity.nickname());
        update.setAvatar(identity.avatar());
        update.setEmail(identity.email());
        this.updateById(update);
    }
}
