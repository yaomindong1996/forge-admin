package com.mdframe.forge.plugin.capability.identity.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityAccessToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AiCapabilityAccessTokenMapper extends BaseMapper<AiCapabilityAccessToken> {

    @InterceptorIgnore(tenantLine = "true")
    AiCapabilityAccessToken selectActiveByTokenKeyId(@Param("tokenKeyId") String tokenKeyId);

    int touchLastUsed(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("lastUsedAt") LocalDateTime lastUsedAt);

    int revoke(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("revokedAt") LocalDateTime revokedAt);

    int revokeByClientVersion(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId,
            @Param("credentialVersion") Integer credentialVersion,
            @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * 仅供短期令牌历史留存任务物理清理，不能用于普通行级删除。
     */
    @InterceptorIgnore(tenantLine = "true")
    int purgeExpiredHistoryBefore(@Param("retentionCutoff") LocalDateTime retentionCutoff);
}
