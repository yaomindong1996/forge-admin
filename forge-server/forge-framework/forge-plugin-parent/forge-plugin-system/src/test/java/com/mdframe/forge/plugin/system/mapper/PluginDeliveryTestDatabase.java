package com.mdframe.forge.plugin.system.mapper;

import com.mdframe.forge.plugin.system.config.PluginDeliveryProperties;
import com.mdframe.forge.plugin.system.config.PluginWorkerIdentity;
import com.mdframe.forge.plugin.system.dto.*;
import com.mdframe.forge.plugin.system.service.plugin.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class PluginDeliveryTestDatabase implements AutoCloseable {
    final PluginArtifactTestDatabase artifacts = new PluginArtifactTestDatabase();
    final PluginDeliveryProperties config = new PluginDeliveryProperties();
    final PluginWorkerIdentity worker = new PluginWorkerIdentity(1L, "delivery-worker");
    final PluginDeliveryService service;
    PluginDeliveryTestDatabase() throws Exception {
        artifacts.registrations.register(artifacts.id, artifacts.command(), artifacts.actor);
        config.setEnabled(true);
        config.getWorker().setId(worker.workerId());
        config.getWorker().setTenantId(1L);
        config.getWorker().setTokenSha256("1".repeat(64));
        config.getWorker().setExpiresAt(Instant.now().plusSeconds(3600));
        var target = new PluginDeliveryProperties.Target();
        target.setId("test"); target.setName("隔离测试"); target.setRepositoryId("local-test");
        config.setTargets(List.of(target));
        var approval = new PluginDeliveryApproval(artifacts.db.deliveries, artifacts.db.reviews,
                artifacts.codec, artifacts.approvals);
        var proxy = new ProxyFactory(new PluginDeliveryService(artifacts.db.deliveries, config,
                approval, artifacts.codec));
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(artifacts.db.source),
                new AnnotationTransactionAttributeSource()));
        service = (PluginDeliveryService) proxy.getProxy();
    }
    PluginDeliveryCreateDTO command(String action) {
        var value = new PluginDeliveryCreateDTO();
        value.setRequestId(UUID.randomUUID().toString()); value.setTaskId(artifacts.id);
        value.setTargetId("test"); value.setReleaseId(artifacts.metadata.getReleaseId());
        value.setAction(action); value.setNote("已确认本次发布制品及目标范围和数据库备份");
        value.setBackupReference("backup-20261009");
        value.setMigrationsReviewed(true); value.setBackwardCompatible(true);
        return value;
    }
    PluginDeliveryLeaseDTO lease() {
        var value = new PluginDeliveryLeaseDTO();
        value.setLease("2".repeat(64)); value.setNonce(UUID.randomUUID().toString()); return value;
    }
    PluginDeliveryFinishDTO result(String status, boolean runtime) {
        var value = new PluginDeliveryFinishDTO();
        value.setLease(lease().getLease()); value.setNonce(UUID.randomUUID().toString());
        value.setStatus(status); value.setCosVerified(true); value.setRuntimeVerified(runtime);
        value.setArtifactManifestSha256(artifacts.metadata.getResult().getArtifactManifestSha256());
        if (!"succeeded".equals(status)) { value.setFailureCode("FIXTURE_FAILURE"); }
        return value;
    }
    String complete(String action) {
        var row = service.create(command(action), artifacts.actor);
        service.claim(row.id(), lease(), worker);
        service.finish(row.id(), result("succeeded", !"publish".equals(action)), worker);
        return row.id();
    }
    @Override public void close() { artifacts.close(); }
}
