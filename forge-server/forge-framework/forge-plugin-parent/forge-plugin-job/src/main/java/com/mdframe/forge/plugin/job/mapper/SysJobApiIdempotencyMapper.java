package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.job.entity.SysJobApiIdempotency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface SysJobApiIdempotencyMapper extends BaseMapper<SysJobApiIdempotency> {

    @InterceptorIgnore(tenantLine = "true")
    SysJobApiIdempotency selectEffective(
            @Param("tenantId") Long tenantId,
            @Param("tokenId") Long tokenId,
            @Param("jobConfigId") Long jobConfigId,
            @Param("keyHash") String keyHash,
            @Param("now") LocalDateTime now);

    @InterceptorIgnore(tenantLine = "true")
    int expireMatching(
            @Param("tenantId") Long tenantId,
            @Param("tokenId") Long tokenId,
            @Param("jobConfigId") Long jobConfigId,
            @Param("keyHash") String keyHash,
            @Param("now") LocalDateTime now);

    @InterceptorIgnore(tenantLine = "true")
    int insertReservation(SysJobApiIdempotency entity);
}
