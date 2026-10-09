package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.dto.PluginArtifactMetadataDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginArtifactRegistration;
import com.mdframe.forge.plugin.system.mapper.SysPluginArtifactMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.plugin.system.vo.PluginArtifactRegistrationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PluginArtifactViews {
    private final SysPluginArtifactMapper artifacts;
    private final SysPluginTaskReviewMapper reviews;
    private final PluginArtifactCodec codec;
    private final PluginReleaseApprovalValidator approvals;

    public List<PluginArtifactRegistrationVO> recent(Long tenantId, String taskId) {
        var rows = artifacts.selectTask(tenantId, taskId);
        if (rows.isEmpty()) {
            return List.of();
        }
        // 当前任务只允许一条候选登记；复用一次审批查询，禁止随记录数逐条访问数据库。
        var snapshot = reviews.selectApprovalSnapshot(tenantId, taskId, rows.get(0).getReviewId());
        return rows.stream().map(row -> view(row, snapshot)).toList();
    }

    private PluginArtifactRegistrationVO view(SysPluginArtifactRegistration row,
                                               PluginReleaseApprovalSnapshot snapshot) {
        var metadata = codec.parse(row.getMetadataJson());
        boolean matches = false;
        try {
            if (metadataMatches(row, metadata)) {
                approvals.validate(snapshot, metadata.approvalCommand());
                matches = true;
            }
        } catch (BusinessException stale) {
            // 只标记查询时点的审批匹配；关闭/核心漂移不删除记录、不永久缓存有效标记。
            matches = false;
        }
        return new PluginArtifactRegistrationVO(row.getId(), row.getRequestId(), metadata, row.getCreateBy(),
                row.getCreateTime(), row.getNote(), matches, Instant.now().toString(), false, false);
    }

    // 审计列与保存的元数据也必须一致，不能只以报告相同猜测它属于当前任务/审批。
    private boolean metadataMatches(SysPluginArtifactRegistration row, PluginArtifactMetadataDTO metadata) {
        return Objects.equals(row.getTaskId(), metadata.getTaskId())
                && Objects.equals(row.getReviewId(), metadata.getReviewId())
                && Objects.equals(row.getExpectedRevision(), metadata.getRevision())
                && Objects.equals(row.getReleaseId(), metadata.getReleaseId())
                && Objects.equals(row.getManifestSha256(), metadata.getManifestSha256())
                && Objects.equals(row.getRepositoryId(), metadata.getRepositoryId())
                && Objects.equals(row.getServerResultSha256(), metadata.getServerResultSha256());
    }
}
