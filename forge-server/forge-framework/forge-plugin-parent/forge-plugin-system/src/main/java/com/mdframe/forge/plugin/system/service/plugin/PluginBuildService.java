package com.mdframe.forge.plugin.system.service.plugin;

import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginBuildClaimDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildFinishDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildHeartbeatDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildLeaseDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.enums.PluginBuildPhase;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.plugin.system.mapper.SysPluginBuildMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.vo.PluginBuildClaimVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Objects;

/** Web 只管理有界数据/CAS；绝不执行上传代码、访问源码工作区或启动构建进程。 */
@Service
@RequiredArgsConstructor
public class PluginBuildService {
    private static final int LEASE_SECONDS = 90;
    private final SysPluginBuildMapper builds;
    private final SysPluginTaskMapper tasks;
    private final PluginBuildViews views;
    private final PluginPreviewPlanner planner;

    @Transactional(rollbackFor = Exception.class)
    public PluginBuildClaimVO claim(String id, PluginBuildClaimDTO command, PluginWorkerIdentity worker) {
        var task = task(worker, id);
        require(Objects.equals(task.getArchiveSha256(), command.getSha256()), "任务包摘要不一致");
        var existing = builds.selectBuild(worker.tenantId(), id);
        if (existing != null) {
            lease(existing, command, worker);
            active(existing);
            require(PluginTaskStatus.BUILDING.matches(task.getTaskStatus())
                    && task.getRevision() == command.getRevision() + 1
                    && Objects.equals(existing.getSourceCommit(), command.getSourceCommit())
                    && Objects.equals(existing.getImage(), command.getImage()), "任务已领取或执行参数已变化");
            return claimView(task, existing);
        }
        require(PluginTaskStatus.QUEUED.matches(task.getTaskStatus())
                && Objects.equals(task.getRevision(), command.getRevision()), "任务不是可领取的待构建版本");
        require(Objects.equals(task.getRuntimeSnapshot(), planner.snapshot()), "运行清单已变化，请重新预检");
        var now = LocalDateTime.now();
        var build = createBuild(task, command, worker, now);
        transition(task, PluginTaskStatus.QUEUED, PluginTaskStatus.BUILDING, now);
        builds.insert(build);
        return claimView(task, build);
    }

    public byte[] archive(String id, PluginBuildLeaseDTO command, PluginWorkerIdentity worker) {
        var task = task(worker, id);
        var build = build(worker, id);
        lease(build, command, worker);
        active(build);
        require(PluginTaskStatus.BUILDING.matches(task.getTaskStatus()), "当前任务不能读取构建包");
        var stored = tasks.selectArchive(worker.tenantId(), id);
        byte[] archive = stored == null ? null : stored.getArchiveData();
        require(archive != null && archive.length == task.getArchiveBytes() && archive.length <= 8388608
                && PackageDigests.sha256(archive).equals(task.getArchiveSha256()), "任务包摘要异常");
        return archive;
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginBuildClaimVO heartbeat(String id, PluginBuildHeartbeatDTO command, PluginWorkerIdentity worker) {
        var task = task(worker, id);
        var build = build(worker, id);
        lease(build, command, worker);
        active(build);
        require(PluginTaskStatus.BUILDING.matches(task.getTaskStatus()), "任务已不在构建中");
        String previous = build.getPhase();
        var phase = PluginBuildPhase.from(command.getPhase());
        require(phase.ordinal() >= PluginBuildPhase.from(previous).ordinal(), "构建阶段不能后退");
        var now = LocalDateTime.now();
        build.setPhase(phase.getCode());
        var expires = now.plusSeconds(LEASE_SECONDS);
        build.setLeaseExpiresTime(expires.isBefore(build.getDeadlineTime()) ? expires : build.getDeadlineTime());
        require(builds.heartbeat(build, previous, now) == 1, "租约已失效或被修改");
        return claimView(task, build);
    }

    @Transactional(rollbackFor = Exception.class)
    public PluginBuildClaimVO finish(String id, PluginBuildFinishDTO command, PluginWorkerIdentity worker) {
        var task = task(worker, id);
        var build = build(worker, id);
        lease(build, command, worker);
        var result = command.getResult();
        validateResult(task, build, result);
        String encoded = views.encode(result);
        String digest = hash(encoded);
        if (build.getFinishedTime() != null) {
            require(Objects.equals(build.getResultSha256(), digest), "已完成任务不能变更构建结果");
            return claimView(task, build);
        }
        active(build);
        require(PluginTaskStatus.BUILDING.matches(task.getTaskStatus()), "任务已不在构建中");
        var now = LocalDateTime.now();
        String previousPhase = build.getPhase();
        build.setPhase(result.getPhase());
        build.setResultJson(encoded);
        build.setResultSha256(digest);
        require(builds.finish(build, previousPhase, now) == 1, "租约已失效，构建结果仅保留在执行器");
        build.setFinishedTime(now);
        var target = Boolean.TRUE.equals(result.getSuccess()) ? PluginTaskStatus.BUILT : PluginTaskStatus.BUILD_FAILED;
        // 终态保留插件占用和包/产物审计；失联也不自动抢占，不推定旧容器已经停止。
        transition(task, PluginTaskStatus.BUILDING, target, now);
        return claimView(task, build);
    }

    private SysPluginBuild createBuild(SysPluginTask task, PluginBuildClaimDTO command,
                                      PluginWorkerIdentity worker, LocalDateTime now) {
        var build = new SysPluginBuild();
        build.setId(task.getId());
        build.setTenantId(worker.tenantId());
        build.setWorkerId(worker.workerId());
        build.setLeaseHash(hash(command.getLeaseToken()));
        build.setPhase(PluginBuildPhase.SOURCE_SNAPSHOT.getCode());
        build.setLeaseExpiresTime(now.plusSeconds(LEASE_SECONDS));
        build.setStartedTime(now);
        build.setDeadlineTime(now.plusMinutes(25));
        build.setSourceCommit(command.getSourceCommit());
        build.setImage(command.getImage());
        build.setCreateBy(task.getConfirmedBy());
        build.setUpdateBy(task.getConfirmedBy());
        build.setCreateDept(task.getCreateDept());
        build.setCreateTime(now);
        build.setUpdateTime(now);
        build.setDelFlag(0);
        return build;
    }

    private void validateResult(SysPluginTask task, SysPluginBuild build, PluginBuildResultDTO result) {
        require(Objects.equals(result.getPackageSha256(), task.getArchiveSha256())
                && Objects.equals(result.getSourceCommit(), build.getSourceCommit())
                && Objects.equals(result.getImage(), build.getImage()), "结果与固定执行输入不一致");
        var phase = PluginBuildPhase.from(result.getPhase());
        require(phase.ordinal() >= PluginBuildPhase.from(build.getPhase()).ordinal(), "结果阶段不能后退");
        if (Boolean.TRUE.equals(result.getSuccess())) {
            require(phase == PluginBuildPhase.ARTIFACT_VERIFICATION && result.getFailureCode() == null
                    && result.getSourceSha256() != null && result.getArtifactCount() != null
                    && result.getArtifactBytes() != null && result.getArtifactManifestSha256() != null,
                    "构建成功结果缺少产物核验摘要");
        } else {
            require(result.getFailureCode() != null && result.getArtifactCount() == null
                    && result.getArtifactBytes() == null && result.getArtifactManifestSha256() == null,
                    "构建失败结果无效");
        }
    }

    private void transition(SysPluginTask task, PluginTaskStatus previous, PluginTaskStatus target,
                            LocalDateTime now) {
        int revision = task.getRevision();
        task.setTaskStatus(target.getCode());
        task.setUpdateBy(task.getConfirmedBy());
        task.setUpdateTime(now);
        require(tasks.transition(task, previous.getCode(), revision) == 1, "任务已被修改，请重新核查");
        task.setRevision(revision + 1);
    }

    private SysPluginTask task(PluginWorkerIdentity worker, String id) {
        var task = tasks.selectTask(worker.tenantId(), id);
        require(task != null && task.getConfirmedBy() != null, "任务不存在、未确认或不属于执行器租户");
        return task;
    }

    private SysPluginBuild build(PluginWorkerIdentity worker, String id) {
        var build = builds.selectBuild(worker.tenantId(), id);
        require(build != null, "构建租约不存在");
        return build;
    }

    private void lease(SysPluginBuild build, PluginBuildLeaseDTO command, PluginWorkerIdentity worker) {
        require(worker.workerId().equals(build.getWorkerId()) && command.getLeaseToken() != null
                && build.getLeaseHash() != null
                && MessageDigest.isEqual(hash(command.getLeaseToken()).getBytes(StandardCharsets.US_ASCII),
                build.getLeaseHash().getBytes(StandardCharsets.US_ASCII)), "构建租约不属于当前请求");
    }

    private void active(SysPluginBuild build) {
        var now = LocalDateTime.now();
        require(build.getFinishedTime() == null && build.getLeaseExpiresTime().isAfter(now)
                && build.getDeadlineTime().isAfter(now), "租约已到期或结束，请人工核查执行器，不自动重试");
    }

    private PluginBuildClaimVO claimView(SysPluginTask task, SysPluginBuild build) {
        return new PluginBuildClaimVO(task.getId(), task.getPluginId(), task.getPluginVersion(),
                task.getOperationType(), task.getArchiveSha256(), task.getArchiveBytes(), task.getTaskStatus(),
                task.getRevision(), build.getLeaseExpiresTime(), LEASE_SECONDS);
    }

    private String hash(String value) {
        return PackageDigests.sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(409, message);
        }
    }
}
