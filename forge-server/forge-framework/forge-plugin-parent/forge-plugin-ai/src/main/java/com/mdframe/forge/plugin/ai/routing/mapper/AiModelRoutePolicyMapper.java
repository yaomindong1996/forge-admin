package com.mdframe.forge.plugin.ai.routing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRoutePolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiModelRoutePolicyMapper extends BaseMapper<AiModelRoutePolicy> {
    Page<AiModelRoutePolicy> selectPolicyPage(Page<AiModelRoutePolicy> page, @Param("keyword") String keyword, @Param("status") String status);
    int countActiveCode(@Param("policyCode") String policyCode, @Param("excludeId") Long excludeId);
    int countPolicyAgents(@Param("policyId") Long policyId);
}
