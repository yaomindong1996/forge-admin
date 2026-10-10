package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskReviewViews;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysPluginTaskReviewMapperTest {
    private PluginReviewTestDatabase db;
    private final PluginTaskActor actor = new PluginTaskActor(1L, 9L, 1L);

    @BeforeEach
    void database() throws Exception { db = new PluginReviewTestDatabase(); }

    @Test
    void closes_expired_build_once_and_seals_old_inflight_updates_without_inventing_result() throws Exception {
        var stale = db.builds.selectBuild(1L, "task");
        var dto = db.close();
        var reviewer = new PluginTaskActor(1L, 10L, 1L);
        db.service.review("task", dto, reviewer);
        db.service.review("task", dto, reviewer);
        var task = db.tasks.selectTask(1L, "task");
        assertThat(task.getTaskStatus()).isEqualTo("closed");
        assertThat(task.getRevision()).isEqualTo(3);
        assertThat(task.getActivePluginId()).isNull();
        assertThat(task.getUpdateBy()).isEqualTo(10L);
        assertThat(db.tasks.selectArchive(1L, "task").getArchiveData()).containsExactly(1, 2, 3);
        var build = db.builds.selectBuild(1L, "task");
        assertThat(build.getFinishedTime()).isNotNull();
        assertThat(build.getResultJson()).isNull();
        assertThat(build.getResultSha256()).isNull();
        assertThat(build.getUpdateBy()).isEqualTo(10L);
        stale.setLeaseExpiresTime(LocalDateTime.now().plusSeconds(90));
        assertThat(db.builds.heartbeat(stale, "container_build", stale.getStartedTime())).isZero();
        assertThat(db.builds.finish(stale, "container_build", stale.getStartedTime())).isZero();
        assertThat(db.reviews.selectRecent(1L, "task")).hasSize(1);
        String response = db.json.writeValueAsString(new PluginTaskReviewViews(db.reviews).recent(1L, "task"));
        assertThat(response).doesNotContain("commandSha256", "requestId", "leaseHash", "archiveData");
        dto.setNote("同请求不得改变说明，这里更换了原始说明内容");
        assertThatThrownBy(() -> db.service.review("task", dto, reviewer)).hasMessageContaining("变更内容");
    }

    @Test
    void audit_failure_rolls_back_both_seal_and_task_slot_release() throws Exception {
        db.execute("ALTER TABLE sys_plugin_task_review ADD CONSTRAINT test_failure CHECK (note <> 'force_failed')");
        var dto = db.close();
        dto.setNote("force_failed");
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).isInstanceOf(RuntimeException.class);
        assertThat(db.tasks.selectTask(1L, "task").getTaskStatus()).isEqualTo("building");
        assertThat(db.tasks.selectTask(1L, "task").getActivePluginId()).isEqualTo("demo");
        assertThat(db.builds.selectBuild(1L, "task").getFinishedTime()).isNull();
        assertThat(db.reviews.selectRecent(1L, "task")).isEmpty();
    }

    @Test
    void active_wrong_tenant_flags_digest_revision_and_absent_task_are_rejected() throws Exception {
        var dto = db.close();
        assertThatThrownBy(() -> db.service.review("task", dto, new PluginTaskActor(2L, 9L, 1L)))
                .hasMessageContaining("任务不存在");
        dto.setExecutorStopped(false);
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("人工确认");
        dto.setExecutorStopped(true);
        dto.setRevision(1);
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("版本");
        dto.setRevision(2);
        dto.setResultSha256("f".repeat(64));
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("结果已变化");
        dto.setResultSha256(null);
        db.execute("UPDATE sys_plugin_build SET lease_expires_time = DATEADD('SECOND', 90, CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("活跃构建");
        assertThat(db.reviews.selectRecent(1L, "task")).isEmpty();
    }

    @Test
    void approve_requires_bound_success_and_human_checks_then_can_close_preserving_worker_report() throws Exception {
        var build = db.builds.selectBuild(1L, "task");
        var result = new PluginBuildResultDTO();
        result.setSuccess(true);
        result.setJobId("job-test");
        result.setPackageSha256("a".repeat(64));
        result.setSourceCommit(build.getSourceCommit());
        result.setImage(build.getImage());
        result.setPhase("artifact_verification");
        result.setSourceSha256("e".repeat(64));
        result.setArtifactCount(1);
        result.setArtifactBytes(100L);
        result.setArtifactManifestSha256("f".repeat(64));
        String encoded = db.json.writeValueAsString(result);
        build.setResultJson(encoded);
        build.setResultSha256(PackageDigests.sha256(encoded.getBytes(StandardCharsets.UTF_8)));
        build.setFinishedTime(LocalDateTime.now());
        db.builds.updateById(build);
        db.execute("UPDATE sys_plugin_task SET task_status = 'built'");
        var dto = db.close();
        dto.setDecision("approve_build");
        dto.setResultSha256(build.getResultSha256());
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("产物及迁移");
        dto.setArtifactsReviewed(true);
        dto.setMigrationsReviewed(true);
        build.setResultJson(encoded.replace("\"success\":true", "\"success\":false"));
        db.builds.updateById(build);
        assertThatThrownBy(() -> db.service.review("task", dto, actor)).hasMessageContaining("报告损坏");
        build.setResultJson(encoded);
        db.builds.updateById(build);
        db.service.review("task", dto, actor);
        assertThat(db.tasks.selectTask(1L, "task").getTaskStatus()).isEqualTo("release_ready");
        assertThat(db.tasks.selectTask(1L, "task").getActivePluginId()).isEqualTo("demo");
        var close = db.close();
        close.setRevision(3);
        close.setResultSha256(build.getResultSha256());
        db.service.review("task", close, actor);
        assertThat(db.reviews.selectRecent(1L, "task")).hasSize(2);
        assertThat(db.builds.selectBuild(1L, "task").getResultJson()).isEqualTo(encoded);
    }

    @Test
    void simultaneous_admins_only_one_closes_and_new_upload_slot_becomes_available() throws Exception {
        var barrier = new CountDownLatch(1);
        var successes = new AtomicInteger();
        var rejected = new AtomicInteger();
        Runnable work = () -> {
            try {
                assertThat(barrier.await(5, TimeUnit.SECONDS)).isTrue();
                db.service.review("task", db.close(), actor);
                successes.incrementAndGet();
            } catch (BusinessException failure) {
                rejected.incrementAndGet();
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
            }
        };
        var first = new Thread(work, "test-plugin-review-1");
        var second = new Thread(work, "test-plugin-review-2");
        first.start();
        second.start();
        barrier.countDown();
        first.join(10000);
        second.join(10000);
        assertThat(first.isAlive() || second.isAlive()).isFalse();
        assertThat(successes).hasValue(1);
        assertThat(rejected).hasValue(1);
        assertThat(db.reviews.selectRecent(1L, "task")).hasSize(1);
        var next = db.tasks.selectTask(1L, "task");
        next.setId("next-task");
        next.setRequestId("next-request");
        next.setTaskStatus("await_confirmation");
        next.setActivePluginId("demo");
        next.setArchiveData(new byte[]{1, 2, 3});
        assertThat(db.tasks.insert(next)).isEqualTo(1);
    }

    @Test
    void locking_query_survives_actual_jsqlparser_without_limit_order_or_tenant_bypass() throws Exception {
        String sql = "SELECT id, tenant_id FROM sys_plugin_build WHERE tenant_id = 1 AND id = 'task'"
                + " AND del_flag = 0 FOR UPDATE";
        assertThat(CCJSqlParserUtil.parse(sql).toString()).endsWith("FOR UPDATE").doesNotContain("ORDER BY", "LIMIT");
    }
}
