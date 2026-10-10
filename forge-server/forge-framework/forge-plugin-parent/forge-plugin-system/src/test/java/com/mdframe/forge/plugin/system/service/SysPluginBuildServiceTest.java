package com.mdframe.forge.plugin.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginBuildClaimDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildFinishDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildHeartbeatDTO;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginBuild;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.mapper.SysPluginBuildMapper;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildService;
import com.mdframe.forge.plugin.system.service.plugin.PluginBuildViews;
import com.mdframe.forge.plugin.system.service.plugin.PluginPreviewPlanner;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SysPluginBuildServiceTest {
    private final SysPluginTaskMapper tasks = mock(SysPluginTaskMapper.class);
    private final SysPluginBuildMapper builds = mock(SysPluginBuildMapper.class);
    private final PluginPreviewPlanner planner = mock(PluginPreviewPlanner.class);
    private final PluginBuildViews views = new PluginBuildViews(builds, new ObjectMapper().findAndRegisterModules());
    private final PluginBuildService service = new PluginBuildService(builds, tasks, views, planner);
    private final PluginWorkerIdentity worker = new PluginWorkerIdentity(1L, "test-worker");
    private final String lease = UUID.randomUUID().toString().replace("-", "").repeat(2);
    private final byte[] zip = new byte[]{1, 2, 3};
    private SysPluginTask task;
    private SysPluginBuild build;
    private PluginBuildClaimDTO claim;

    @BeforeEach
    void fixture() {
        task = new SysPluginTask();
        task.setId("task");
        task.setTenantId(1L);
        task.setPluginId("demo");
        task.setPluginVersion("1.0.0");
        task.setOperationType("install");
        task.setTaskStatus("queued");
        task.setActivePluginId("demo");
        task.setArchiveSha256(PackageDigests.sha256(zip));
        task.setArchiveBytes(zip.length);
        task.setRevision(1);
        task.setConfirmedBy(9L);
        task.setRuntimeSnapshot("snapshot");
        when(tasks.selectTask(1L, "task")).thenReturn(task);
        when(planner.snapshot()).thenReturn("snapshot");
        when(tasks.transition(any(), anyString(), anyInt())).thenReturn(1);
        when(builds.insert(any(SysPluginBuild.class))).thenAnswer(invocation -> {
            build = invocation.getArgument(0);
            when(builds.selectBuild(1L, "task")).thenReturn(build);
            return 1;
        });
        claim = new PluginBuildClaimDTO();
        claim.setRevision(1);
        claim.setSha256(task.getArchiveSha256());
        claim.setLeaseToken(lease);
        claim.setSourceCommit("a".repeat(40));
        claim.setImage("test-builder@sha256:" + "b".repeat(64));
    }

    @Test
    void claims_once_with_fixed_input_and_repeated_lease_is_idempotent_not_new_execution() throws Exception {
        assertThat(service.claim("task", claim, worker).status()).isEqualTo("building");
        assertThat(service.claim("task", claim, worker).revision()).isEqualTo(2);
        verify(builds, times(1)).insert(any(SysPluginBuild.class));
        assertThat(task.getActivePluginId()).isEqualTo("demo");
        assertThat(build.getLeaseHash()).isNotEqualTo(lease);
        assertThat(build.getDeadlineTime()).isEqualTo(build.getStartedTime().plusMinutes(25));
        when(builds.selectSummary(1L, "task")).thenReturn(build);
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(views.summary(1L, "task")))
                .doesNotContain(lease, build.getLeaseHash(), "leaseToken", "archiveData");
        claim.setSourceCommit("c".repeat(40));
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("变化");
    }

    @Test
    void rejects_wrong_tenant_hash_revision_runtime_snapshot_cancel_and_racing_claim() {
        assertThatThrownBy(() -> service.claim("task", claim, new PluginWorkerIdentity(2L, "test-worker")))
                .hasMessageContaining("租户");
        claim.setSha256("0".repeat(64));
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("摘要");
        claim.setSha256(task.getArchiveSha256());
        claim.setRevision(2);
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("待构建版本");
        claim.setRevision(1);
        task.setRuntimeSnapshot("changed");
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("运行清单");
        task.setRuntimeSnapshot("snapshot");
        when(tasks.transition(any(), eq("queued"), anyInt())).thenReturn(0);
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("修改");
        verify(builds, never()).insert(any(SysPluginBuild.class));
    }

    @Test
    void package_is_only_available_to_current_live_lease_and_digest_is_revalidated() {
        service.claim("task", claim, worker);
        var archive = new SysPluginTask();
        archive.setArchiveData(zip);
        when(tasks.selectArchive(1L, "task")).thenReturn(archive);
        assertThat(service.archive("task", claim, worker)).isEqualTo(zip);
        claim.setLeaseToken("0".repeat(64));
        assertThatThrownBy(() -> service.archive("task", claim, worker)).hasMessageContaining("租约");
        claim.setLeaseToken(lease);
        archive.setArchiveData(new byte[]{9});
        assertThatThrownBy(() -> service.archive("task", claim, worker)).hasMessageContaining("摘要");
        build.setLeaseExpiresTime(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.archive("task", claim, worker)).hasMessageContaining("到期");
        assertThat(task.getActivePluginId()).isEqualTo("demo");
    }

    @Test
    void heartbeat_is_monotonic_bounded_by_deadline_and_cannot_revive_expired_or_finished_lease() {
        service.claim("task", claim, worker);
        var heartbeat = new PluginBuildHeartbeatDTO();
        heartbeat.setLeaseToken(lease);
        heartbeat.setPhase("container_build");
        when(builds.heartbeat(any(), anyString(), any())).thenReturn(1);
        build.setDeadlineTime(LocalDateTime.now().plusSeconds(10));
        service.heartbeat("task", heartbeat, worker);
        assertThat(build.getLeaseExpiresTime()).isEqualTo(build.getDeadlineTime());
        heartbeat.setPhase("source_preflight");
        assertThatThrownBy(() -> service.heartbeat("task", heartbeat, worker)).hasMessageContaining("后退");
        heartbeat.setPhase("container_build");
        build.setLeaseExpiresTime(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> service.heartbeat("task", heartbeat, worker)).hasMessageContaining("到期");
        assertThatThrownBy(() -> service.claim("task", claim, worker)).hasMessageContaining("到期");
        assertThat(task.getTaskStatus()).isEqualTo("building");
    }

    @Test
    void successful_finish_is_immutable_idempotent_bound_to_input_and_retains_deployment_slot() {
        service.claim("task", claim, worker);
        var command = finish(true);
        command.getResult().setSourceCommit("c".repeat(40));
        assertThatThrownBy(() -> service.finish("task", command, worker)).hasMessageContaining("输入");
        command.getResult().setSourceCommit(claim.getSourceCommit());
        command.getResult().setArtifactManifestSha256(null);
        assertThatThrownBy(() -> service.finish("task", command, worker)).hasMessageContaining("缺少");
        command.getResult().setArtifactManifestSha256("e".repeat(64));
        when(builds.finish(any(), anyString(), any())).thenReturn(1);
        assertThat(service.finish("task", command, worker).status()).isEqualTo("built");
        assertThat(service.finish("task", command, worker).status()).isEqualTo("built");
        verify(builds, times(1)).finish(any(), anyString(), any());
        assertThat(task.getActivePluginId()).isEqualTo("demo");
        command.getResult().setArtifactBytes(200L);
        assertThatThrownBy(() -> service.finish("task", command, worker)).hasMessageContaining("不能变更");
    }

    @Test
    void failed_finish_and_expired_cas_do_not_claim_deployment_or_release_slot() {
        service.claim("task", claim, worker);
        var command = finish(false);
        assertThatThrownBy(() -> service.finish("task", command, worker)).hasMessageContaining("失效");
        when(builds.finish(any(), anyString(), any())).thenReturn(1);
        assertThat(service.finish("task", command, worker).status()).isEqualTo("build_failed");
        assertThat(task.getActivePluginId()).isEqualTo("demo");
        assertThat(build.getResultJson()).doesNotContain(lease, "deployed", "path", "log");
    }

    private PluginBuildFinishDTO finish(boolean success) {
        var result = new PluginBuildResultDTO();
        result.setSuccess(success);
        result.setJobId("job-fixture");
        result.setPackageSha256(task.getArchiveSha256());
        result.setSourceCommit(claim.getSourceCommit());
        result.setImage(claim.getImage());
        result.setPhase(success ? "artifact_verification" : "source_snapshot");
        if (success) {
            result.setSourceSha256("d".repeat(64));
            result.setArtifactCount(2);
            result.setArtifactBytes(100L);
            result.setArtifactManifestSha256("e".repeat(64));
        } else {
            result.setFailureCode("SOURCE_DIRTY");
        }
        var command = new PluginBuildFinishDTO();
        command.setLeaseToken(lease);
        command.setResult(result);
        return command;
    }
}
