package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryReconcileDTO;
import com.mdframe.forge.plugin.system.dto.PluginDeliveryRecoveryDTO;
import com.mdframe.forge.plugin.system.service.plugin.PluginTaskActor;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SysPluginDeliveryTest {
    @Test void concurrent_same_request_returns_one_task_after_target_lock() throws Exception {
        try (var db = new PluginDeliveryTestDatabase()) {
            var command = db.command("publish");
            var ready = new java.util.concurrent.CountDownLatch(2);
            var start = new java.util.concurrent.CountDownLatch(1);
            var ids = new java.util.concurrent.ArrayBlockingQueue<String>(2);
            var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Runnable work = () -> {
                ready.countDown();
                try {
                    if (!start.await(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new IllegalStateException("fixture start timeout");
                    }
                    ids.add(db.service.create(command, db.artifacts.actor).id());
                } catch (Throwable failure) {
                    error.compareAndSet(null, failure);
                }
            };
            var first = new Thread(work, "delivery-retry-1");
            var second = new Thread(work, "delivery-retry-2");
            first.start(); second.start();
            assertThat(ready.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            first.join(8000); second.join(8000);
            assertThat(first.isAlive() || second.isAlive()).isFalse();
            assertThat(error.get()).isNull();
            assertThat(ids).hasSize(2);
            assertThat(ids.poll()).isEqualTo(ids.poll());
            assertThat(db.service.recent(db.artifacts.actor)).hasSize(1);
        }
    }

    @Test void actual_xml_transaction_publish_deploy_nonce_idempotency_and_target_serialization() throws Exception {
        try (var db = new PluginDeliveryTestDatabase()) {
            var command = db.command("publish");
            var task = db.service.create(command, db.artifacts.actor);
            assertThat(db.service.create(command, db.artifacts.actor).id()).isEqualTo(task.id());
            command.setNote("同一请求不能换参数后再次进行不同操作");
            assertThatThrownBy(() -> db.service.create(command, db.artifacts.actor)).hasMessageContaining("内容不同");
            assertThatThrownBy(() -> db.service.create(db.command("publish"), db.artifacts.actor))
                    .hasMessageContaining("活动任务");
            var lease = db.lease();
            var claim = db.service.claim(task.id(), lease, db.worker);
            assertThat(claim.nonce()).isEqualTo(lease.getNonce());
            assertThat(claim.workerId()).isEqualTo("delivery-worker");
            assertThatThrownBy(() -> db.service.claim(task.id(), lease, db.worker)).hasMessageContaining("领取");
            db.service.authorize(task.id(), lease, db.worker);
            var receipt = db.result("succeeded", false);
            assertThat(db.service.finish(task.id(), receipt, db.worker).status()).isEqualTo("succeeded");
            assertThat(db.service.finish(task.id(), receipt, db.worker).status()).isEqualTo("succeeded");
            receipt.setArtifactManifestSha256("9".repeat(64));
            assertThatThrownBy(() -> db.service.finish(task.id(), receipt, db.worker)).hasMessageContaining("摘要");
            assertThat(db.service.targets(db.artifacts.actor).get(0).currentReleaseId()).isNull();
            db.complete("deploy");
            assertThat(db.service.targets(db.artifacts.actor).get(0).currentReleaseId())
                    .isEqualTo(db.artifacts.metadata.getReleaseId());
            assertThat(db.service.recent(db.artifacts.actor)).hasSize(2);
            assertThat(db.service.candidates(db.artifacts.actor)).hasSize(1);
        }
    }

    @Test void current_approval_tenant_identity_and_backup_fail_closed() throws Exception {
        try (var db = new PluginDeliveryTestDatabase()) {
            assertThatThrownBy(() -> db.service.create(db.command("deploy"), db.artifacts.actor))
                    .hasMessageContaining("尚未");
            assertThatThrownBy(() -> db.service.create(db.command("publish"), new PluginTaskActor(2L, 9L, 1L)))
                    .hasMessageContaining("未启用");
            db.complete("publish");
            var command = db.command("deploy"); command.setBackupReference(null);
            assertThatThrownBy(() -> db.service.create(command, db.artifacts.actor)).hasMessageContaining("备份");
            var row = db.service.create(db.command("deploy"), db.artifacts.actor);
            assertThatThrownBy(() -> db.service.claim(row.id(), db.lease(), new PluginWorkerIdentity(2L, "delivery-worker")))
                    .hasMessageContaining("无权");
            db.service.claim(row.id(), db.lease(), db.worker);
            db.artifacts.closeTask();
            assertThatThrownBy(() -> db.service.authorize(row.id(), db.lease(), db.worker))
                    .hasMessageContaining("审批");
            assertThatThrownBy(() -> db.service.finish(row.id(), db.result("succeeded", true), db.worker))
                    .hasMessageContaining("审批");
            assertThat(db.service.finish(row.id(), db.result("uncertain", false), db.worker).status())
                    .isEqualTo("uncertain");
            assertThat(db.service.targets(db.artifacts.actor).get(0).activeTaskId()).isEqualTo(row.id());
        }
    }

    @Test void expired_lease_keeps_lock_reconcile_preserves_audit_and_restore_confirms_old_version() throws Exception {
        try (var db = new PluginDeliveryTestDatabase()) {
            db.complete("publish"); db.complete("deploy");
            var row = db.service.create(db.command("deploy"), db.artifacts.actor);
            db.service.claim(row.id(), db.lease(), db.worker);
            db.artifacts.db.execute("UPDATE sys_plugin_delivery SET lease_expires_time = '2000-01-01' WHERE id='"
                    + row.id() + "'");
            assertThat(db.service.recent(db.artifacts.actor)).extracting("status").contains("uncertain");
            assertThatThrownBy(() -> db.service.finish(row.id(), db.result("succeeded", true), db.worker))
                    .hasMessageContaining("结束");
            var close = new PluginDeliveryReconcileDTO();
            close.setExecutorStopped(true); close.setNote("执行器已停止并逐项核查目标，仍未确认运行版本");
            var closed = db.service.reconcile(row.id(), close, db.artifacts.actor);
            assertThat(closed.note()).isEqualTo(row.note());
            assertThat(closed.reconcileNote()).isEqualTo(close.getNote());
            var state = db.service.targets(db.artifacts.actor).get(0);
            assertThat(state.activeTaskId()).isNull();
            assertThat(state.unverifiedReleaseId()).isEqualTo(row.releaseId());
            assertThatThrownBy(() -> db.service.create(db.command("deploy"), db.artifacts.actor))
                    .hasMessageContaining("只能恢复");
            var recovery = new PluginDeliveryRecoveryDTO(); recovery.setNonce(db.lease().getNonce());
            assertThat(db.service.recovery(row.id(), recovery, db.worker).nonce()).isEqualTo(recovery.getNonce());
            db.complete("restore");
            assertThat(db.service.targets(db.artifacts.actor).get(0).unverifiedReleaseId()).isNull();
        }
    }
}
