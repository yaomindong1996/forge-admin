package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginBuildResultDTO;
import com.mdframe.forge.plugin.system.dto.PluginReleaseCheckDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginReleaseCheckService;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.starter.plugin.ForgeVersion;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 真XML/H2合成表及原审批服务，不冒充真实MySQL/HTTPS环境。 */
class SysPluginReleaseCheckTest {
    private PluginReviewTestDatabase db;
    private PluginReleaseCheckService service;
    private PluginReleaseCheckDTO command;
    private final PluginWorkerIdentity worker = new PluginWorkerIdentity(1L, "worker");

    @BeforeEach
    void database() throws Exception {
        db = new PluginReviewTestDatabase();
        service = new PluginReleaseCheckService(db.reviews, db.json);
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
        build.setPhase("artifact_verification");
        db.builds.updateById(build);
        db.execute("UPDATE sys_plugin_task SET task_status = 'built', preview_json = '{\"coreVersion\":\""
                + ForgeVersion.CURRENT + "\"}'");
        var review = db.close();
        review.setDecision("approve_build");
        review.setResultSha256(build.getResultSha256());
        review.setArtifactsReviewed(true);
        review.setMigrationsReviewed(true);
        db.service.review("task", review, new PluginTaskActor(1L, 9L, 1L));
        command = new PluginReleaseCheckDTO();
        command.setCheckId(UUID.randomUUID().toString());
        command.setReviewId(db.reviews.selectRecent(1L, "task").get(0).getId());
        command.setRevision(3);
        command.setServerResultSha256(build.getResultSha256());
        command.setManifestSha256("b".repeat(64));
        command.setPluginId("demo");
        command.setPluginVersion("1.0.0");
        command.setCoreVersion(ForgeVersion.CURRENT);
        command.setOperation("install");
        command.setResult(result);
    }

    @Test
    void current_approval_binds_report_and_does_not_write_or_grant_deployment() throws Exception {
        String before = db.json.writeValueAsString(db.tasks.selectTask(1L, "task"));
        var value = service.check("task", command, worker);
        assertThat(value.liveTaskApprovalVerified()).isTrue();
        assertThat(value.deployed()).isFalse();
        assertThat(value.checkId()).isEqualTo(command.getCheckId());
        assertThat(value.manifestSha256()).isEqualTo(command.getManifestSha256());
        assertThat(value.serverResultSha256()).isEqualTo(command.getServerResultSha256());
        assertThat(Instant.parse(value.checkedAt())).isBeforeOrEqualTo(Instant.now());
        assertThat(db.json.writeValueAsString(db.tasks.selectTask(1L, "task"))).isEqualTo(before);
        assertThat(db.reviews.selectRecent(1L, "task")).hasSize(1);
        assertThat(db.tasks.selectArchive(1L, "task").getArchiveData()).containsExactly(1, 2, 3);
        assertThat(db.json.writeValueAsString(value)).doesNotContain("lease", "note", "preview", "resultJson", "By");
        // 审查已失效时绝不复用先前成功；关闭仍保留历史和包。
        var close = db.close();
        close.setRevision(3);
        close.setResultSha256(command.getServerResultSha256());
        db.service.review("task", close, new PluginTaskActor(1L, 9L, 1L));
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("审批或版本");
    }

    @Test
    void identity_task_and_expected_review_are_not_supplied_as_authority_by_client() {
        assertThatThrownBy(() -> service.check("task", command, new PluginWorkerIdentity(2L, "worker")))
                .hasMessageContaining("不可核验");
        assertThatThrownBy(() -> service.check("task", command, new PluginWorkerIdentity(1L, "other")))
                .hasMessageContaining("不可核验");
        assertThatThrownBy(() -> service.check("other", command, worker)).hasMessageContaining("不可核验");
        command.setReviewId(UUID.randomUUID().toString());
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("不可核验");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sys_plugin_task", "sys_plugin_build", "sys_plugin_task_review"})
    void every_join_filters_logical_deletion(String table) throws Exception {
        db.execute("UPDATE " + table + " SET del_flag = 1");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("不可核验");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sys_plugin_build", "sys_plugin_task_review"})
    void joined_rows_cannot_cross_tenant_even_when_task_id_collides(String table) throws Exception {
        db.execute("UPDATE " + table + " SET tenant_id = 2");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("不可核验");
    }

    @ParameterizedTest
    @ValueSource(strings = {"expected_revision = 1", "not_deployed = FALSE", "executor_stopped = FALSE",
            "artifacts_reviewed = FALSE", "migrations_reviewed = FALSE", "package_sha256 = 'invalid'",
            "result_sha256 = 'invalid'", "decision = 'close_task'", "target_status = 'closed'"})
    void review_must_bind_current_revision_package_result_and_all_confirmations(String assignment) throws Exception {
        db.execute("UPDATE sys_plugin_task_review SET " + assignment);
        assertThatThrownBy(() -> service.check("task", command, worker)).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jobId", "sourceSha256", "packageSha256", "sourceCommit", "image",
            "phase", "artifactCount", "artifactBytes", "artifactManifestSha256", "success", "failureCode"})
    void all_report_attributes_are_compared_not_just_artifact_digest(String field) throws Exception {
        var node = db.json.valueToTree(command.getResult());
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(field);
        if ("failureCode".equals(field)) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(field, "SYNTHETIC_FAILURE");
        }
        command.setResult(db.json.treeToValue(node, PluginBuildResultDTO.class));
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("制品与已审查");
    }

    @Test
    void rejects_raw_report_hash_stale_revision_plugin_operation_core_and_corrupt_storage() throws Exception {
        command.setServerResultSha256("c".repeat(64));
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("摘要不一致");
        command.setServerResultSha256(db.builds.selectBuild(1L, "task").getResultSha256());
        command.setRevision(2);
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("版本");
        command.setRevision(3);
        command.setPluginVersion("2.0.0");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("摘要不一致");
        command.setPluginVersion("1.0.0");
        command.setOperation("replace");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("摘要不一致");
        command.setOperation("install");
        command.setCoreVersion("0.0.0");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("核心版本");
        command.setCoreVersion(ForgeVersion.CURRENT);
        db.execute("UPDATE sys_plugin_build SET result_json = '{}'");
        assertThatThrownBy(() -> service.check("task", command, worker)).hasMessageContaining("报告摘要异常");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{}"})
    void malformed_stored_report_fails_as_business_error_even_if_hashes_are_consistent(String value) throws Exception {
        String digest = PackageDigests.sha256(value.getBytes(StandardCharsets.UTF_8));
        var build = db.builds.selectBuild(1L, "task");
        build.setResultJson(value);
        build.setResultSha256(digest);
        db.builds.updateById(build);
        db.execute("UPDATE sys_plugin_task_review SET result_sha256 = '" + digest + "'");
        command.setServerResultSha256(digest);
        assertThatThrownBy(() -> service.check("task", command, worker))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "{}", "{\"coreVersion\":123}", "{\"coreVersion\":\"0.0.0\"}"})
    void malformed_or_stale_preview_does_not_authorize_core_version(String preview) throws Exception {
        var task = db.tasks.selectTask(1L, "task");
        task.setPreviewJson(preview);
        db.tasks.updateById(task);
        assertThatThrownBy(() -> service.check("task", command, worker))
                .isInstanceOf(com.mdframe.forge.starter.core.exception.BusinessException.class);
    }

    @Test
    void parser_preserves_single_snapshot_tenant_and_soft_delete_constraints() throws Exception {
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        try (var input = new org.springframework.core.io.ClassPathResource("mapper/SysPluginTaskReviewMapper.xml")
                .getInputStream()) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(input, configuration, "review.xml",
                    configuration.getSqlFragments()).parse();
        }
        var params = java.util.Map.of("tenantId", 1L, "taskId", "task", "reviewId", command.getReviewId());
        String sql = configuration.getMappedStatement(SysPluginTaskReviewMapper.class.getName()
                + ".selectApprovalSnapshot").getBoundSql(params).getSql();
        String rewritten = net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql).toString();
        assertThat(rewritten).contains("b.tenant_id = t.tenant_id", "r.tenant_id = t.tenant_id",
                "t.tenant_id = ?", "b.del_flag = 0", "r.del_flag = 0", "t.del_flag = 0")
                .doesNotContain("archive_data", "lease_hash", " note", "FOR UPDATE", "LIMIT", "ORDER BY");
    }
}
