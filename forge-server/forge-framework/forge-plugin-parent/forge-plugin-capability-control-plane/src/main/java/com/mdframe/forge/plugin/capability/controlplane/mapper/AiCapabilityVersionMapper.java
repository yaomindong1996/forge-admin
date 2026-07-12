package com.mdframe.forge.plugin.capability.controlplane.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiCapabilityVersionMapper extends BaseMapper<AiCapabilityVersion> {

    AiCapabilityVersion selectVersion(@Param("tenantId") Long tenantId,
                                      @Param("capabilityId") Long capabilityId,
                                      @Param("version") String version);
}
