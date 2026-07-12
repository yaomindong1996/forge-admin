package com.mdframe.forge.plugin.ai.routing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.ai.routing.domain.AiModelRouteTarget;
import com.mdframe.forge.plugin.ai.routing.vo.AiModelRoutePolicyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiModelRouteTargetMapper extends BaseMapper<AiModelRouteTarget> {
    List<AiModelRoutePolicyVO.Target> selectByPolicyId(@Param("policyId") Long policyId);

    List<AiModelRoutePolicyVO.Target> selectByPolicyIds(@Param("policyIds") List<Long> policyIds);

    int logicallyDeleteByPolicyId(@Param("policyId") Long policyId);
}
