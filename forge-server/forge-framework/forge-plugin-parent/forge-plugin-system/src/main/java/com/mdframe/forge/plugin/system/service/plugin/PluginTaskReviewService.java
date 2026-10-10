package com.mdframe.forge.plugin.system.service.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.PluginTaskReviewDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.entity.SysPluginTaskReview;
import com.mdframe.forge.plugin.system.enums.PluginReviewDecision;
import com.mdframe.forge.plugin.system.enums.PluginBuildPhase;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.plugin.system.mapper.SysPluginBuildMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskReviewMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** 人工审查控制面；不远程停止容器、不执行部署、不伪造 worker 报告。 */
@Service
@RequiredArgsConstructor
public class PluginTaskReviewService {
    private final SysPluginTaskMapper tasks;
    private final SysPluginBuildMapper builds;
    private final SysPluginTaskReviewMapper reviews;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public void review(String id, PluginTaskReviewDTO command, PluginTaskActor actor) {
        normalize(command);
        // 统一按 build→task 顺序锁/CAS，与 finish 保持一致；在途续期不能穿过封存。
        var build = builds.lockSummary(actor.tenantId(), id);
        require(build != null, "任务不存在或尚未产生构建记录");
        String digest = digest(command);
        var previous = reviews.selectRequest(actor.tenantId(), id, actor.userId(), command.getRequestId());
        if (previous != null) {
            require(digest.equals(previous.getCommandSha256()), "同一核查请求不能变更内容");
            return;
        }
        var task = tasks.selectTask(actor.tenantId(), id);
        require(task != null && Objects.equals(task.getArchiveSha256(), command.getSha256())
                && Objects.equals(task.getRevision(), command.getRevision()), "任务摘要或版本已变化，请刷新核查");
        require(Objects.equals(build.getResultSha256(), command.getResultSha256()), "构建结果已变化，请刷新核查");
        var decision = PluginReviewDecision.from(command.getDecision());
        var target = target(task, build, command, decision);
        var now = LocalDateTime.now();
        var audit = audit(task, command, actor, target, now);
        audit.setCommandSha256(digest);
        if (decision == PluginReviewDecision.CLOSE_TASK && build.getFinishedTime() == null) {
            require(builds.seal(actor.tenantId(), id, actor.userId(), now) == 1, "构建已发生变化，请刷新核查");
        }
        String expected = task.getTaskStatus();
        task.setTaskStatus(target.getCode());
        task.setUpdateBy(actor.userId());
        task.setUpdateTime(now);
        if (target == PluginTaskStatus.CLOSED) {
            task.setActivePluginId(null);
        }
        require(tasks.transition(task, expected, command.getRevision()) == 1, "任务已被其它请求修改，请刷新");
        reviews.insert(audit);
    }

    private PluginTaskStatus target(SysPluginTask task, SysPluginBuild build, PluginTaskReviewDTO command,
                                    PluginReviewDecision decision) {
        if (decision == PluginReviewDecision.APPROVE_BUILD) {
            require(PluginTaskStatus.BUILT.matches(task.getTaskStatus()) && build.getFinishedTime() != null
                    && build.getResultJson() != null && build.getResultSha256() != null,
                    "仅构建通过且已回写结果的任务可审查");
            require(Boolean.TRUE.equals(command.getArtifactsReviewed())
                    && Boolean.TRUE.equals(command.getMigrationsReviewed()), "请确认产物及迁移影响已经审查");
            validateSuccess(task, build);
            return PluginTaskStatus.RELEASE_READY;
        }
        boolean terminal = PluginTaskStatus.BUILT.matches(task.getTaskStatus())
                || PluginTaskStatus.BUILD_FAILED.matches(task.getTaskStatus())
                || PluginTaskStatus.RELEASE_READY.matches(task.getTaskStatus());
        var now = LocalDateTime.now();
        boolean expired = PluginTaskStatus.BUILDING.matches(task.getTaskStatus())
                && (!build.getLeaseExpiresTime().isAfter(now) || !build.getDeadlineTime().isAfter(now));
        require(terminal || expired, "活跃构建或当前状态不能关闭，请先核查执行器");
        return PluginTaskStatus.CLOSED;
    }

    private void normalize(PluginTaskReviewDTO command) {
        require(Boolean.TRUE.equals(command.getExecutorStopped()) && Boolean.TRUE.equals(command.getNotDeployed()),
                "必须人工确认执行器已停止且本次产物尚未部署");
        String note = command.getNote() == null ? "" : command.getNote().strip();
        require(note.length() >= 10 && note.length() <= 1000, "请填写10到1000字的核查说明，不要包含凭证");
        command.setNote(note);
    }

    private void validateSuccess(SysPluginTask task, SysPluginBuild build) {
        String value = build.getResultJson();
        require(value.length() <= 65536 && PackageDigests.sha256(value.getBytes(StandardCharsets.UTF_8))
                .equals(build.getResultSha256()), "构建报告损坏，请人工核查");
        try {
            var result = json.readValue(value, PluginBuildResultDTO.class);
            require(Boolean.TRUE.equals(result.getSuccess())
                    && PluginBuildPhase.ARTIFACT_VERIFICATION.getCode().equals(result.getPhase())
                    && result.getFailureCode() == null && validDigest(result.getSourceSha256())
                    && Objects.equals(task.getArchiveSha256(), result.getPackageSha256())
                    && Objects.equals(build.getSourceCommit(), result.getSourceCommit())
                    && Objects.equals(build.getImage(), result.getImage())
                    && result.getArtifactCount() != null && result.getArtifactCount() > 0
                    && result.getArtifactCount() <= 4096
                    && result.getArtifactBytes() != null && result.getArtifactBytes() > 0
                    && result.getArtifactBytes() <= 1073741824L
                    && validDigest(result.getArtifactManifestSha256()), "缺少一致的构建成功报告");
        } catch (JsonProcessingException exception) {
            throw new BusinessException(409, "构建报告损坏，请人工核查");
        }
    }

    private boolean validDigest(String value) {
        return value != null && value.matches("[a-f0-9]{64}");
    }

    private SysPluginTaskReview audit(SysPluginTask task, PluginTaskReviewDTO command, PluginTaskActor actor,
                                     PluginTaskStatus target, LocalDateTime now) {
        var review = new SysPluginTaskReview();
        review.setId(UUID.randomUUID().toString());
        review.setTenantId(actor.tenantId());
        review.setTaskId(task.getId());
        review.setRequestId(command.getRequestId());
        review.setExpectedRevision(command.getRevision());
        review.setPackageSha256(command.getSha256());
        review.setResultSha256(command.getResultSha256());
        review.setDecision(command.getDecision());
        review.setPreviousStatus(task.getTaskStatus());
        review.setTargetStatus(target.getCode());
        review.setExecutorStopped(command.getExecutorStopped());
        review.setNotDeployed(command.getNotDeployed());
        review.setArtifactsReviewed(command.getArtifactsReviewed());
        review.setMigrationsReviewed(command.getMigrationsReviewed());
        review.setNote(command.getNote());
        review.setCreateBy(actor.userId());
        review.setUpdateBy(actor.userId());
        review.setCreateDept(actor.deptId());
        review.setCreateTime(now);
        review.setUpdateTime(now);
        review.setDelFlag(0);
        return review;
    }

    private String digest(PluginTaskReviewDTO command) {
        try {
            return PackageDigests.sha256(json.writeValueAsString(command).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("核查请求无法保存", exception);
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(409, message);
        }
    }
}
