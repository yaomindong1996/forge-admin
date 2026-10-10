package com.mdframe.forge.plugin.system.service.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.dto.PluginReleaseCheckDTO;
import com.mdframe.forge.plugin.system.enums.PluginBuildPhase;
import com.mdframe.forge.plugin.system.enums.PluginReviewDecision;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PluginReleaseApprovalValidator {
    private final ObjectMapper json;

    public void validate(PluginReleaseApprovalSnapshot snapshot, PluginReleaseCheckDTO command) {
        require(snapshot != null, "当前任务审批不可核验");
        validateApproval(snapshot, command);
        validateReport(snapshot, command);
    }

    private void validateApproval(PluginReleaseApprovalSnapshot row, PluginReleaseCheckDTO command) {
        require(PluginTaskStatus.RELEASE_READY.matches(row.getTaskStatus())
                && PluginTaskStatus.RELEASE_READY.matches(row.getTargetStatus())
                && PluginTaskStatus.BUILT.matches(row.getPreviousStatus())
                && PluginReviewDecision.APPROVE_BUILD.getCode().equals(row.getDecision())
                && Objects.equals(row.getReviewId(), command.getReviewId())
                && Objects.equals(row.getRevision(), command.getRevision())
                && row.getExpectedRevision() != null && row.getRevision() != null
                && (long) row.getExpectedRevision() + 1 == row.getRevision(), "任务审批或版本已变化");
        require(Boolean.TRUE.equals(row.getExecutorStopped()) && Boolean.TRUE.equals(row.getNotDeployed())
                && Boolean.TRUE.equals(row.getArtifactsReviewed()) && Boolean.TRUE.equals(row.getMigrationsReviewed())
                && row.getFinishedTime() != null, "缺少一致的人工审查");
        require(Objects.equals(row.getPluginId(), row.getActivePluginId())
                && Objects.equals(row.getPluginId(), command.getPluginId())
                && Objects.equals(row.getPluginVersion(), command.getPluginVersion())
                && Objects.equals(row.getOperationType(), command.getOperation())
                && Objects.equals(row.getArchiveSha256(), row.getReviewPackageSha256())
                && Objects.equals(row.getResultSha256(), row.getReviewResultSha256())
                && Objects.equals(row.getResultSha256(), command.getServerResultSha256()), "审批绑定摘要不一致");
    }

    private void validateReport(PluginReleaseApprovalSnapshot row, PluginReleaseCheckDTO command) {
        String value = row.getResultJson();
        require(value != null && value.length() <= 65536
                && PackageDigests.sha256(value.getBytes(StandardCharsets.UTF_8)).equals(row.getResultSha256()),
                "构建报告摘要异常");
        try {
            var result = json.readValue(value, PluginBuildResultDTO.class);
            require(result != null && Boolean.TRUE.equals(result.getSuccess()) && result.getFailureCode() == null
                    && PluginBuildPhase.ARTIFACT_VERIFICATION.getCode().equals(result.getPhase())
                    && PluginBuildPhase.ARTIFACT_VERIFICATION.getCode().equals(row.getBuildPhase())
                    && Objects.equals(result.getPackageSha256(), row.getArchiveSha256())
                    && Objects.equals(result.getSourceCommit(), row.getSourceCommit())
                    && Objects.equals(result.getImage(), row.getImage())
                    && result.equals(command.getResult()), "候选制品与已审查构建不一致");
            String preview = row.getPreviewJson();
            require(preview != null && preview.length() <= 1048576, "任务预览异常");
            var tree = json.readTree(preview);
            require(tree != null && tree.isObject(), "任务预览异常");
            var core = tree.get("coreVersion");
            require(core != null && core.isTextual() && core.asText().equals(command.getCoreVersion())
                    && ForgeVersion.CURRENT.equals(command.getCoreVersion()), "核心版本已变化或与构建不一致");
        } catch (JsonProcessingException exception) {
            throw new BusinessException(409, "构建报告或任务预览异常");
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(409, message);
        }
    }
}
