package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SysPluginArtifactTest {
    private PluginArtifactTestDatabase f;
    @BeforeEach void database() throws Exception { f = new PluginArtifactTestDatabase(); }
    @AfterEach void close() { f.close(); }

    @Test
    void append_only_registration_preserves_task_and_idempotent_history_becomes_stale_after_close() throws Exception {
        var command = f.command();
        String before = f.db.json.writeValueAsString(f.db.tasks.selectTask(1L, f.id));
        f.registrations.register(f.id, command, f.actor);
        f.registrations.register(f.id, command, f.actor);
        assertThat(f.db.artifacts.selectTask(1L, f.id)).hasSize(1);
        assertThat(f.db.json.writeValueAsString(f.db.tasks.selectTask(1L, f.id))).isEqualTo(before);
        var view = f.views.recent(1L, f.id).get(0);
        assertThat(view.currentApprovalMatches()).isTrue();
        assertThat(view.serverArtifactBytesVerified()).isFalse();
        assertThat(view.deployed()).isFalse();
        assertThat(view.registeredBy()).isEqualTo(9L);
        assertThat(f.db.json.writeValueAsString(view)).doesNotContain("commandSha256", "leaseHash", "archiveData");
        f.closeTask();
        f.registrations.register(f.id, command, f.actor);
        assertThat(f.views.recent(1L, f.id).get(0).currentApprovalMatches()).isFalse();
        assertThat(f.db.tasks.selectArchive(1L, f.id).getArchiveData()).containsExactly(1, 2, 3);
        assertThat(f.db.artifacts.selectTask(1L, f.id)).hasSize(1);
    }

    @Test
    void changed_request_existing_candidate_closed_task_and_cross_tenant_fail_without_overwrite() {
        var command = f.command();
        assertThatThrownBy(() -> f.registrations.register(f.id, command, new PluginTaskActor(2L, 9L, 1L)))
                .isInstanceOf(BusinessException.class);
        f.registrations.register(f.id, command, f.actor);
        command.setNote("同请求改变核查内容应该拒绝覆盖登记记录");
        assertThatThrownBy(() -> f.registrations.register(f.id, command, f.actor)).hasMessageContaining("变更内容");
        assertThatThrownBy(() -> f.registrations.register(f.id, f.command(), f.actor)).hasMessageContaining("已有候选");
        f.closeTask();
        assertThatThrownBy(() -> f.registrations.register(f.id, f.command(), f.actor)).hasMessageContaining("审批或版本");
        assertThat(f.db.artifacts.selectTask(2L, f.id)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"taskId", "reviewId"})
    void saved_metadata_identity_must_match_audit_binding(String field) throws Exception {
        f.registrations.register(f.id, f.command(), f.actor);
        var row = f.db.artifacts.selectTask(1L, f.id).get(0);
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode) f.db.json.valueToTree(f.metadata);
        metadata.put(field, java.util.UUID.randomUUID().toString());
        row.setMetadataJson(metadata.toString());
        f.db.artifacts.updateById(row);
        assertThat(f.views.recent(1L, f.id).get(0).currentApprovalMatches()).isFalse();
        assertThat(f.db.tasks.selectTask(1L, f.id).getRevision()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"revision", "serverResultSha256", "pluginVersion", "coreVersion",
            "operation", "reviewId", "manifestSha256"})
    void stale_or_mismatched_binding_fails(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) f.db.json.valueToTree(f.metadata);
        if ("revision".equals(field)) node.put(field, 2);
        else if ("reviewId".equals(field)) node.put(field, java.util.UUID.randomUUID().toString());
        else if (field.endsWith("Sha256")) node.put(field, "0".repeat(64));
        else node.put(field, "operation".equals(field) ? "replace" : "2.0.0");
        var command = f.command();
        command.setMetadataJson(node.toString());
        assertThatThrownBy(() -> f.registrations.register(f.id, command, f.actor))
                .isInstanceOf(BusinessException.class);
        assertThat(f.db.artifacts.selectTask(1L, f.id)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sys_plugin_task", "sys_plugin_build", "sys_plugin_task_review"})
    void joined_soft_deleted_rows_cannot_register(String table) throws Exception {
        f.db.execute("UPDATE " + table + " SET del_flag = 1");
        assertThatThrownBy(() -> f.registrations.register(f.id, f.command(), f.actor))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void strict_metadata_rejects_unknown_duplicate_coerced_or_unbounded_json_and_missing_declarations() {
        var command = f.command();
        String valid = command.getMetadataJson();
        f.db.json.disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        for (String text : new String[]{valid.replaceFirst("\\{", "{\"tenantId\":9,"),
                valid.replaceFirst("\\{", "{\"revision\":3,"),
                valid.replace("\"revision\":3", "\"revision\":\"3\""),
                valid.replace("\"success\":true", "\"success\":\"true\""),
                valid + "{}", " ".repeat(65537), "null"}) {
            command.setMetadataJson(text);
            assertThatThrownBy(() -> f.registrations.register(f.id, command, f.actor))
                    .isInstanceOf(BusinessException.class);
        }
        command.setMetadataJson(valid);
        command.setLocalVerified(false);
        assertThatThrownBy(() -> f.registrations.register(f.id, command, f.actor)).hasMessageContaining("人工确认");
        assertThat(f.db.artifacts.selectTask(1L, f.id)).isEmpty();
    }

    @Test
    void audit_insert_failure_rolls_back_without_task_or_report_mutation() throws Exception {
        f.db.execute("ALTER TABLE sys_plugin_artifact_registration ADD CONSTRAINT force_failure"
                + " CHECK (note <> 'force_failed')");
        var command = f.command();
        command.setNote("force_failed");
        assertThatThrownBy(() -> f.registrations.register(f.id, command, f.actor))
                .isInstanceOf(RuntimeException.class);
        assertThat(f.db.artifacts.selectTask(1L, f.id)).isEmpty();
        assertThat(f.db.tasks.selectTask(1L, f.id).getRevision()).isEqualTo(3);
        assertThat(f.db.tasks.selectTask(1L, f.id).getActivePluginId()).isEqualTo("demo");
    }

    @Test
    void simultaneous_registrations_only_append_one_record() throws Exception {
        var ready = new CountDownLatch(1);
        var successes = new AtomicInteger();
        var failures = new AtomicInteger();
        Runnable work = () -> {
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                f.registrations.register(f.id, f.command(), f.actor);
                successes.incrementAndGet();
            } catch (BusinessException conflict) {
                failures.incrementAndGet();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        };
        var first = new Thread(work, "artifact-test-1");
        var second = new Thread(work, "artifact-test-2");
        first.start(); second.start(); ready.countDown();
        first.join(10000); second.join(10000);
        assertThat(first.isAlive() || second.isAlive()).isFalse();
        assertThat(successes).hasValue(1);
        assertThat(failures).hasValue(1);
        assertThat(f.db.artifacts.selectTask(1L, f.id)).hasSize(1);
    }

    @Test
    void registration_racing_close_never_revives_approval_or_loses_audit() throws Exception {
        var ready = new CountDownLatch(1);
        var unexpected = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var registered = new AtomicInteger();
        var first = new Thread(() -> {
            try {
                ready.await();
                f.registrations.register(f.id, f.command(), f.actor);
                registered.incrementAndGet();
            } catch (BusinessException alreadyClosed) {
                // 关闭先取得build锁时登记必须拒绝；不是构建重试。
            } catch (Throwable failure) {
                unexpected.set(failure);
            }
        }, "artifact-register-race");
        var second = new Thread(() -> {
            try {
                ready.await();
                f.closeTask();
            } catch (Throwable failure) {
                unexpected.set(failure);
            }
        }, "artifact-close-race");
        first.start();
        second.start();
        ready.countDown();
        first.join(10000);
        second.join(10000);
        assertThat(first.isAlive() || second.isAlive()).isFalse();
        assertThat(unexpected.get()).isNull();
        assertThat(f.db.tasks.selectTask(1L, f.id).getTaskStatus()).isEqualTo("closed");
        assertThat(f.db.tasks.selectTask(1L, f.id).getActivePluginId()).isNull();
        assertThat(f.views.recent(1L, f.id)).hasSize(registered.get())
                .allMatch(row -> !row.currentApprovalMatches());
    }
}
