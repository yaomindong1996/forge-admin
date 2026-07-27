package com.mdframe.forge.plugin.data.service;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationItem;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据连接密码持久化密文盘点与迁移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataConnectionCryptoMigrationService {

    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final String SOURCE = "DATA_CONNECTION";

    private final DataConnectionMapper connectionMapper;
    private final PersistentCryptoService persistentCryptoService;
    private final CryptoProperties cryptoProperties;
    private final PlatformTransactionManager transactionManager;

    public CryptoMigrationReport inventory(Long tenantId) {
        CryptoMigrationReport report = baseReport(tenantId);
        scan(tenantId, normalizeBatchSize(null), true, false, report);
        return report;
    }

    public CryptoMigrationReport migrate(Long tenantId,
                                         String expectedActiveKeyId,
                                         Integer batchSize,
                                         boolean dryRun) {
        validateMigrationRequest(expectedActiveKeyId);
        CryptoMigrationReport report = baseReport(tenantId);
        report.setExpectedActiveKeyId(expectedActiveKeyId);
        report.setDryRun(dryRun);
        report.setBatchSize(normalizeBatchSize(batchSize));
        scan(tenantId, report.getBatchSize(), dryRun, true, report);
        return report;
    }

    public void validateMigrationRequest(String expectedActiveKeyId) {
        assertActiveKey(expectedActiveKeyId);
    }

    private void scan(Long tenantId,
                      int batchSize,
                      boolean dryRun,
                      boolean migrate,
                      CryptoMigrationReport report) {
        Long afterId = null;
        while (true) {
            List<DataConnection> batch = connectionMapper.selectCryptoMigrationBatch(tenantId, afterId, batchSize);
            if (batch == null || batch.isEmpty()) {
                return;
            }
            handleBatch(batch, dryRun, migrate, report);
            for (DataConnection connection : batch) {
                afterId = connection.getId();
            }
            if (batch.size() < batchSize) {
                return;
            }
        }
    }

    private void handleBatch(List<DataConnection> batch,
                             boolean dryRun,
                             boolean migrate,
                             CryptoMigrationReport report) {
        List<PendingUpdate> pendingUpdates = new ArrayList<>();
        boolean blocked = false;
        for (DataConnection connection : batch) {
            Preparation preparation = prepareConnection(connection, dryRun, migrate, report);
            blocked = blocked || preparation.blocked();
            if (preparation.pendingUpdate() != null) {
                pendingUpdates.add(preparation.pendingUpdate());
            }
        }
        if (!migrate || dryRun || blocked || pendingUpdates.isEmpty()) {
            return;
        }
        executeBatch(pendingUpdates, report);
    }

    private Preparation prepareConnection(DataConnection connection,
                                           boolean dryRun,
                                           boolean migrate,
                                           CryptoMigrationReport report) {
        String identifier = connection.getId() == null ? null : String.valueOf(connection.getId());
        try {
            PersistentCiphertext inspected = persistentCryptoService.inspect(connection.getPasswordCipher(), null);
            String format = inspected.format().name();
            report.increment(format);
            if (!migrate || inspected.format() == PersistentCiphertext.Format.ACTIVE
                    || inspected.format() == PersistentCiphertext.Format.EMPTY) {
                return Preparation.skipped();
            }
            if (inspected.format() == PersistentCiphertext.Format.UNKNOWN
                    || inspected.format() == PersistentCiphertext.Format.UNKNOWN_KEY) {
                report.addItem(CryptoMigrationItem.blocked(SOURCE, identifier, null, null, "password_cipher",
                        inspected.failureReason()));
                return Preparation.blockedResult();
            }
            String newCipher = persistentCryptoService.reencrypt(connection.getPasswordCipher(), inspected.algorithm());
            if (dryRun) {
                report.increment("MIGRATABLE");
                return Preparation.skipped();
            }
            return Preparation.ready(new PendingUpdate(
                    connection.getTenantId(), connection.getId(), connection.getPasswordCipher(), newCipher, identifier));
        } catch (Exception e) {
            log.warn("[DataConnectionCryptoMigrationService] 数据连接密文处理失败, connectionId={}, exceptionType={}",
                    connection.getId(), e.getClass().getSimpleName());
            report.addItem(CryptoMigrationItem.blocked(SOURCE, identifier, null, null, "password_cipher", "密文处理失败"));
            report.increment("FAILED");
            return Preparation.blockedResult();
        }
    }

    private void executeBatch(List<PendingUpdate> pendingUpdates, CryptoMigrationReport report) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                for (PendingUpdate pendingUpdate : pendingUpdates) {
                    int affected = connectionMapper.updatePasswordCipherIfMatch(
                            pendingUpdate.tenantId(), pendingUpdate.id(),
                            pendingUpdate.oldCipher(), pendingUpdate.newCipher());
                    if (affected != 1) {
                        throw new BatchConflictException(pendingUpdate.identifier());
                    }
                }
            });
            report.increment("MIGRATED", pendingUpdates.size());
        } catch (BatchConflictException e) {
            report.increment("CONFLICT");
            report.increment("FAILED");
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, e.identifier(), null, null, "password_cipher", "并发修改导致当前批次回滚"));
        } catch (Exception e) {
            log.warn("[DataConnectionCryptoMigrationService] 数据连接密文批次写入失败并回滚, exceptionType={}",
                    e.getClass().getSimpleName());
            report.increment("FAILED");
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, batchIdentifier(pendingUpdates), null, null, "password_cipher", "当前批次写入失败并已回滚"));
        }
    }

    private String batchIdentifier(List<PendingUpdate> pendingUpdates) {
        if (pendingUpdates.isEmpty()) {
            return null;
        }
        String first = pendingUpdates.get(0).identifier();
        String last = pendingUpdates.get(pendingUpdates.size() - 1).identifier();
        return StringUtils.equals(first, last) ? first : first + ".." + last;
    }

    private CryptoMigrationReport baseReport(Long tenantId) {
        CryptoMigrationReport report = CryptoMigrationReport.of(tenantId, SOURCE);
        report.setActiveKeyId(activeKeyId());
        report.setDryRun(true);
        return report;
    }

    private void assertActiveKey(String expectedActiveKeyId) {
        CryptoProperties.PersistenceProperties persistence = cryptoProperties.getPersistence();
        if (persistence == null
                || !Boolean.TRUE.equals(persistence.getEnabled())
                || !Boolean.TRUE.equals(persistence.getWriteVersioned())) {
            throw new BusinessException("执行密文迁移前必须启用版本化写入");
        }
        String activeKeyId = persistence.getActiveKeyId();
        if (StringUtils.isBlank(expectedActiveKeyId)) {
            throw new BusinessException("expectedActiveKeyId不能为空");
        }
        if (!StringUtils.equals(expectedActiveKeyId, activeKeyId)) {
            throw new BusinessException("expectedActiveKeyId与当前活动keyId不一致");
        }
    }

    private String activeKeyId() {
        CryptoProperties.PersistenceProperties persistence = cryptoProperties.getPersistence();
        return persistence == null ? null : persistence.getActiveKeyId();
    }

    private int normalizeBatchSize(Integer batchSize) {
        int value = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        return Math.min(value, MAX_BATCH_SIZE);
    }

    private record PendingUpdate(Long tenantId, Long id, String oldCipher, String newCipher, String identifier) {
    }

    private record Preparation(PendingUpdate pendingUpdate, boolean blocked) {

        static Preparation ready(PendingUpdate pendingUpdate) {
            return new Preparation(pendingUpdate, false);
        }

        static Preparation skipped() {
            return new Preparation(null, false);
        }

        static Preparation blockedResult() {
            return new Preparation(null, true);
        }
    }

    private static final class BatchConflictException extends RuntimeException {

        private final String identifier;

        private BatchConflictException(String identifier) {
            super("migration batch conflict");
            this.identifier = identifier;
        }

        private String identifier() {
            return identifier;
        }
    }
}
