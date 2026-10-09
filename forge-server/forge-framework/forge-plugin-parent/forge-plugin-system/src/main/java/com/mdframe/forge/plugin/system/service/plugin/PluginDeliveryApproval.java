package com.mdframe.forge.plugin.system.service.plugin;
import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import com.mdframe.forge.plugin.system.mapper.SysPluginDeliveryMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PluginDeliveryApproval {
    private final SysPluginDeliveryMapper deliveries;
    private final SysPluginTaskReviewMapper reviews;
    private final PluginArtifactCodec codec;
    private final PluginReleaseApprovalValidator approvals;
    public PluginArtifactMetadataDTO validate(Long tenant, String task, String release) {
        var candidate = deliveries.candidate(tenant, task, release);
        if (candidate == null) { throw new BusinessException(409, "候选制品不存在或无权访问"); }
        var metadata = codec.parse(candidate.getMetadataJson());
        approvals.validate(reviews.selectApprovalSnapshot(tenant, task, metadata.getReviewId()),
                metadata.approvalCommand());
        return metadata;
    }
}
