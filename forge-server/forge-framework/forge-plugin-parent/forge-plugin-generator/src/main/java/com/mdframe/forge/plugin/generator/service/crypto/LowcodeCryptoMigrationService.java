package com.mdframe.forge.plugin.generator.service.crypto;

import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlRepository;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContextHolder;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationItem;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 低代码动态业务字段持久化密文盘点与迁移。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeCryptoMigrationService {

    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int MAX_BATCH_SIZE = 1000;
    private static final String SOURCE = "LOWCODE_FIELD";
    private static final Pattern CONFIG_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private final AiCrudConfigMapper configMapper;
    private final LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    private final LowcodeDdlRepository ddlRepository;
    private final LowcodeEncryptConfigParser encryptConfigParser;
    private final LowcodeCryptoMigrationRepository migrationRepository;
    private final PersistentCryptoService persistentCryptoService;
    private final CryptoProperties cryptoProperties;

    public CryptoMigrationReport inventory(Long tenantId, Collection<String> configKeys) {
        return TenantContextHolder.executeWithTenant(tenantId, () -> {
            CryptoMigrationReport report = baseReport(tenantId);
            scanConfigs(tenantId, normalizeConfigKeys(configKeys), normalizeBatchSize(null), true, false, report);
            return report;
        });
    }

    public CryptoMigrationReport migrate(Long tenantId,
                                         Collection<String> configKeys,
                                         String expectedActiveKeyId,
                                         Integer batchSize,
                                         boolean dryRun) {
        List<String> normalizedConfigKeys = normalizeConfigKeys(configKeys);
        validateMigrationRequest(normalizedConfigKeys, expectedActiveKeyId);
        return TenantContextHolder.executeWithTenant(tenantId, () -> {
            CryptoMigrationReport report = baseReport(tenantId);
            report.setExpectedActiveKeyId(expectedActiveKeyId);
            report.setBatchSize(normalizeBatchSize(batchSize));
            report.setDryRun(dryRun);
            scanConfigs(tenantId, normalizedConfigKeys, report.getBatchSize(), dryRun, true, report);
            return report;
        });
    }

    public void validateMigrationRequest(Collection<String> configKeys, String expectedActiveKeyId) {
        List<String> normalizedConfigKeys = normalizeConfigKeys(configKeys);
        if (normalizedConfigKeys.isEmpty()) {
            throw new BusinessException("低代码密文迁移必须显式指定configKey");
        }
        assertActiveKey(expectedActiveKeyId);
    }

    private void scanConfigs(Long tenantId,
                             List<String> configKeys,
                             int batchSize,
                             boolean dryRun,
                             boolean migrate,
                             CryptoMigrationReport report) {
        List<AiCrudConfig> configs = configMapper.selectEncryptConfigCandidates(
                tenantId, configKeys.isEmpty() ? null : configKeys);
        for (AiCrudConfig config : configs) {
            scanConfig(config, batchSize, dryRun, migrate, report);
        }
    }

    private void scanConfig(AiCrudConfig config,
                            int batchSize,
                            boolean dryRun,
                            boolean migrate,
                            CryptoMigrationReport report) {
        List<LowcodeEncryptConfigParser.FieldRule> rules;
        try {
            rules = encryptConfigParser.parse(config.getEncryptConfig());
        } catch (Exception e) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), null, null, e.getMessage()));
            return;
        }
        if (rules.isEmpty()) {
            return;
        }

        LowcodeRuntimeDataSourceContext context;
        try {
            context = runtimeDataSourceResolver.resolve(config);
        } catch (Exception e) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), config.getTableName(), null, e.getMessage()));
            return;
        }
        String tableName = StringUtils.firstNonBlank(context.getTableName(), config.getRuntimeTableName(), config.getTableName());
        try (LowcodeRuntimeDataSourceContextHolder.Scope ignored = LowcodeRuntimeDataSourceContextHolder.use(context)) {
            Precheck precheck = precheck(config, context, tableName, rules, migrate, report);
            if (precheck.blocked()) {
                return;
            }
            for (LowcodeEncryptConfigParser.FieldRule rule : rules) {
                scanField(config, context, precheck, rule, batchSize, dryRun, migrate, report);
            }
        }
    }

    private Precheck precheck(AiCrudConfig config,
                              LowcodeRuntimeDataSourceContext context,
                              String tableName,
                              List<LowcodeEncryptConfigParser.FieldRule> rules,
                              boolean migrate,
                              CryptoMigrationReport report) {
        try {
            migrationRepository.validateIdentifier(tableName);
        } catch (Exception e) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, e.getMessage()));
            return Precheck.blockedResult();
        }
        if (migrate && (context.isReadonly() || !context.isAllowWrite())) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, "运行数据源只读或未开放写入"));
            return Precheck.blockedResult();
        }
        if (!ddlRepository.tableExists(context, tableName)) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, "运行表不存在"));
            return Precheck.blockedResult();
        }
        Set<String> columns = ddlRepository.listColumns(context, tableName);
        try {
            migrationRepository.validateRuntimeConditions(context, columns, config.getTenantId());
        } catch (Exception e) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, e.getMessage()));
            return Precheck.blockedResult();
        }
        List<LowcodeDdlRepository.PrimaryKeyMetadata> primaryKeys = ddlRepository.listPrimaryKeys(context, tableName);
        if (primaryKeys.size() != 1) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, "运行表不是单主键"));
            return Precheck.blockedResult();
        }
        String primaryKeyColumn = primaryKeys.get(0).columnName();
        String configuredPrimaryKeyColumn = context.getPrimaryKey() == null ? "id" : context.getPrimaryKey().getColumnName();
        if (StringUtils.isNotBlank(configuredPrimaryKeyColumn)
                && !StringUtils.equalsIgnoreCase(primaryKeyColumn, configuredPrimaryKeyColumn)) {
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, config.getConfigKey(), config.getConfigKey(), tableName, null, "运行表主键与低代码配置不一致"));
            return Precheck.blockedResult();
        }
        Map<String, LowcodeDdlRepository.ColumnMetadata> metadata = ddlRepository.listColumnMetadata(context, tableName);
        for (LowcodeEncryptConfigParser.FieldRule rule : rules) {
            if (!containsColumn(columns, rule.columnName())) {
                report.addItem(CryptoMigrationItem.blocked(
                        SOURCE, config.getConfigKey() + "." + rule.fieldName(), config.getConfigKey(),
                        tableName, rule.fieldName(), "加密字段列不存在"));
                return Precheck.blockedResult();
            }
            LowcodeDdlRepository.ColumnMetadata column = metadata.get(rule.columnName().toLowerCase(Locale.ROOT));
            int capacity = ddlRepository.characterCapacity(column);
            if (capacity == 0) {
                report.addItem(CryptoMigrationItem.blocked(
                        SOURCE, config.getConfigKey() + "." + rule.fieldName(), config.getConfigKey(),
                        tableName, rule.fieldName(), "加密字段不是字符列"));
                return Precheck.blockedResult();
            }
        }
        return new Precheck(tableName, primaryKeyColumn, columns, metadata, false);
    }

    private void scanField(AiCrudConfig config,
                           LowcodeRuntimeDataSourceContext context,
                           Precheck precheck,
                           LowcodeEncryptConfigParser.FieldRule rule,
                           int batchSize,
                           boolean dryRun,
                           boolean migrate,
                           CryptoMigrationReport report) {
        Object afterPrimaryKey = null;
        while (true) {
            List<LowcodeCryptoMigrationRepository.RowCipher> rows = migrationRepository.selectBatch(
                    context, precheck.tableName(), precheck.primaryKeyColumn(), rule.columnName(),
                    precheck.columns(), config.getTenantId(), afterPrimaryKey, batchSize);
            if (rows.isEmpty()) {
                return;
            }
            handleBatch(config, context, precheck, rule, rows, dryRun, migrate, report);
            for (LowcodeCryptoMigrationRepository.RowCipher row : rows) {
                afterPrimaryKey = row.primaryKey();
            }
            if (rows.size() < batchSize) {
                return;
            }
        }
    }

    private void handleBatch(AiCrudConfig config,
                             LowcodeRuntimeDataSourceContext context,
                             Precheck precheck,
                             LowcodeEncryptConfigParser.FieldRule rule,
                             List<LowcodeCryptoMigrationRepository.RowCipher> rows,
                             boolean dryRun,
                             boolean migrate,
                             CryptoMigrationReport report) {
        List<PendingUpdate> pendingUpdates = new ArrayList<>();
        boolean blocked = false;
        for (LowcodeCryptoMigrationRepository.RowCipher row : rows) {
            Preparation preparation = prepareRow(config, precheck, rule, row, dryRun, migrate, report);
            blocked = blocked || preparation.blocked();
            if (preparation.pendingUpdate() != null) {
                pendingUpdates.add(preparation.pendingUpdate());
            }
        }
        if (!migrate || dryRun || blocked || pendingUpdates.isEmpty()) {
            return;
        }
        executeBatch(config, context, precheck, rule, pendingUpdates, report);
    }

    private Preparation prepareRow(AiCrudConfig config,
                                   Precheck precheck,
                                   LowcodeEncryptConfigParser.FieldRule rule,
                                   LowcodeCryptoMigrationRepository.RowCipher row,
                                   boolean dryRun,
                                   boolean migrate,
                                   CryptoMigrationReport report) {
        String identifier = config.getConfigKey() + "." + rule.fieldName() + "#" + row.primaryKey();
        try {
            PersistentCiphertext inspected = persistentCryptoService.inspect(row.cipherText(), rule.algorithm());
            report.increment(inspected.format().name());
            if (!migrate || inspected.format() == PersistentCiphertext.Format.ACTIVE
                    || inspected.format() == PersistentCiphertext.Format.EMPTY) {
                return Preparation.skipped();
            }
            if (inspected.format() == PersistentCiphertext.Format.UNKNOWN
                    || inspected.format() == PersistentCiphertext.Format.UNKNOWN_KEY) {
                report.addItem(CryptoMigrationItem.blocked(SOURCE, identifier, config.getConfigKey(),
                        precheck.tableName(), rule.fieldName(), inspected.failureReason()));
                return Preparation.blockedResult();
            }
            String newCipher = persistentCryptoService.reencrypt(row.cipherText(), inspected.algorithm());
            if (!fits(precheck.metadata().get(rule.columnName().toLowerCase(Locale.ROOT)), newCipher)) {
                report.addItem(CryptoMigrationItem.blocked(SOURCE, identifier, config.getConfigKey(),
                        precheck.tableName(), rule.fieldName(), "目标列容量不足"));
                return Preparation.blockedResult();
            }
            if (dryRun) {
                report.increment("MIGRATABLE");
                return Preparation.skipped();
            }
            return Preparation.ready(new PendingUpdate(row.primaryKey(), row.cipherText(), newCipher, identifier));
        } catch (Exception e) {
            log.warn("[LowcodeCryptoMigrationService] 低代码密文字段处理失败, configKey={}, field={}, rowId={}, "
                            + "exceptionType={}",
                    config.getConfigKey(), rule.fieldName(), row.primaryKey(), e.getClass().getSimpleName());
            report.addItem(CryptoMigrationItem.blocked(SOURCE, identifier, config.getConfigKey(),
                    precheck.tableName(), rule.fieldName(), "密文处理失败"));
            report.increment("FAILED");
            return Preparation.blockedResult();
        }
    }

    private void executeBatch(AiCrudConfig config,
                              LowcodeRuntimeDataSourceContext context,
                              Precheck precheck,
                              LowcodeEncryptConfigParser.FieldRule rule,
                              List<PendingUpdate> pendingUpdates,
                              CryptoMigrationReport report) {
        try {
            migrationRepository.executeInTransaction(context, () -> {
                for (PendingUpdate pendingUpdate : pendingUpdates) {
                    int affected = migrationRepository.updateIfMatch(
                            context, precheck.tableName(), precheck.primaryKeyColumn(), pendingUpdate.primaryKey(),
                            rule.columnName(), pendingUpdate.oldCipher(), pendingUpdate.newCipher(),
                            precheck.columns(), config.getTenantId());
                    if (affected != 1) {
                        throw new BatchConflictException(pendingUpdate.identifier());
                    }
                }
                return pendingUpdates.size();
            });
            report.increment("MIGRATED", pendingUpdates.size());
        } catch (BatchConflictException e) {
            report.increment("CONFLICT");
            report.increment("FAILED");
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, e.identifier(), config.getConfigKey(), precheck.tableName(), rule.fieldName(),
                    "并发修改导致当前批次回滚"));
        } catch (Exception e) {
            log.warn("[LowcodeCryptoMigrationService] 低代码密文字段批次写入失败并回滚, configKey={}, field={}, "
                            + "exceptionType={}",
                    config.getConfigKey(), rule.fieldName(), e.getClass().getSimpleName());
            report.increment("FAILED");
            report.addItem(CryptoMigrationItem.blocked(
                    SOURCE, batchIdentifier(pendingUpdates), config.getConfigKey(), precheck.tableName(), rule.fieldName(),
                    "当前批次写入失败并已回滚"));
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

    private boolean fits(LowcodeDdlRepository.ColumnMetadata column, String value) {
        int capacity = ddlRepository.characterCapacity(column);
        return capacity < 0 || value == null || value.length() <= capacity;
    }

    private boolean containsColumn(Set<String> columns, String columnName) {
        return columns != null && columns.contains(StringUtils.defaultString(columnName).toLowerCase(Locale.ROOT));
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

    private List<String> normalizeConfigKeys(Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String configKey : configKeys) {
            String value = StringUtils.trimToEmpty(configKey);
            if (StringUtils.isBlank(value)) {
                continue;
            }
            if (!CONFIG_KEY_PATTERN.matcher(value).matches()) {
                throw new BusinessException("configKey格式不正确: " + value);
            }
            normalized.add(value);
        }
        return new ArrayList<>(normalized);
    }

    private record Precheck(String tableName,
                            String primaryKeyColumn,
                            Set<String> columns,
                            Map<String, LowcodeDdlRepository.ColumnMetadata> metadata,
                            boolean blocked) {

        static Precheck blockedResult() {
            return new Precheck(null, null, Set.of(), Map.of(), true);
        }
    }

    private record PendingUpdate(Object primaryKey, String oldCipher, String newCipher, String identifier) {
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
