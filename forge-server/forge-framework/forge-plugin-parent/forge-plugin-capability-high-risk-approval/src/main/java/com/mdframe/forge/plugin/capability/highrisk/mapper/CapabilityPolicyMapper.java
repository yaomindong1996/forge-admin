package com.mdframe.forge.plugin.capability.highrisk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.capability.highrisk.domain.AiCapabilityPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CapabilityPolicyMapper extends BaseMapper<AiCapabilityPolicy> {
    AiCapabilityPolicy selectActive(@Param("tenantId") Long tenantId,
                                    @Param("capabilityId") Long capabilityId,
                                    @Param("version") String version);
}
