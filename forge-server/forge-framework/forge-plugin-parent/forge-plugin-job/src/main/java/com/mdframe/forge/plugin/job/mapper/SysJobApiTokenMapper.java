package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.entity.SysJobApiToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SysJobApiTokenMapper extends BaseMapper<SysJobApiToken> {

    Page<SysJobApiToken> selectTokenPage(
            Page<SysJobApiToken> page,
            @Param("tenantId") Long tenantId,
            @Param("callerName") String callerName,
            @Param("status") String status,
            @Param("now") LocalDateTime now);

    SysJobApiToken selectTenantById(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    SysJobApiToken selectActiveByTokenKeyId(@Param("tokenKeyId") String tokenKeyId);

    @InterceptorIgnore(tenantLine = "true")
    int touchLastUsed(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("lastUsedAt") LocalDateTime lastUsedAt);

    int revoke(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id,
            @Param("revokedAt") LocalDateTime revokedAt);
}
