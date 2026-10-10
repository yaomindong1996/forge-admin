package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.plugin.system.vo.PluginTaskReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PluginTaskReviewViews {
    private final SysPluginTaskReviewMapper mapper;

    public List<PluginTaskReviewVO> recent(Long tenantId, String id) {
        return mapper.selectRecent(tenantId, id).stream().map(review -> new PluginTaskReviewVO(review.getId(),
                review.getDecision(), review.getPreviousStatus(), review.getTargetStatus(), review.getCreateBy(),
                review.getCreateTime(), review.getExecutorStopped(), review.getNotDeployed(),
                review.getArtifactsReviewed(), review.getMigrationsReviewed(), review.getNote())).toList();
    }
}
