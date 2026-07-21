package com.mdframe.forge.starter.outbound.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistQuery;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysOutboundWhitelistMapper extends BaseMapper<SysOutboundWhitelist> {

    @InterceptorIgnore(tenantLine = "true")
    Page<SysOutboundWhitelist> selectWhitelistPage(
            Page<SysOutboundWhitelist> page,
            @Param("tenantId") Long tenantId,
            @Param("query") OutboundWhitelistQuery query);

    @InterceptorIgnore(tenantLine = "true")
    SysOutboundWhitelist selectByIdForTenant(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    Long countOverlappingRules(
            @Param("tenantId") Long tenantId,
            @Param("scene") String scene,
            @Param("protocol") String protocol,
            @Param("host") String host,
            @Param("portStart") Integer portStart,
            @Param("portEnd") Integer portEnd,
            @Param("excludeId") Long excludeId);

    @InterceptorIgnore(tenantLine = "true")
    int updateRule(@Param("entity") SysOutboundWhitelist entity);

    @InterceptorIgnore(tenantLine = "true")
    int logicDelete(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    List<SysOutboundWhitelist> selectActiveRules(
            @Param("tenantId") Long tenantId,
            @Param("scene") String scene,
            @Param("protocol") String protocol,
            @Param("host") String host,
            @Param("port") Integer port);
}
