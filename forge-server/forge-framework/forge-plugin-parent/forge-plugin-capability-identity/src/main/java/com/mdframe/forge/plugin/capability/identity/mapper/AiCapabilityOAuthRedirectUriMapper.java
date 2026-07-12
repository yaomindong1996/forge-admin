package com.mdframe.forge.plugin.capability.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.identity.domain.AiCapabilityOAuthRedirectUri;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiCapabilityOAuthRedirectUriMapper extends BaseMapper<AiCapabilityOAuthRedirectUri> {

    AiCapabilityOAuthRedirectUri selectExact(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId,
            @Param("redirectUriHash") String redirectUriHash,
            @Param("redirectUri") String redirectUri);

    List<AiCapabilityOAuthRedirectUri> selectEnabledByClient(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId);

    int disableByClient(
            @Param("tenantId") Long tenantId,
            @Param("clientId") Long clientId);
}
