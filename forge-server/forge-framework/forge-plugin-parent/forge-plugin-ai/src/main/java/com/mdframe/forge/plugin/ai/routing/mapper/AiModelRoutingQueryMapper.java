package com.mdframe.forge.plugin.ai.routing.mapper;
import com.mdframe.forge.plugin.ai.routing.AiModelRouteCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiModelRoutingQueryMapper {
    List<AiModelRouteCandidate> selectPolicyCandidates(@Param("policyId") Long policyId);
}
