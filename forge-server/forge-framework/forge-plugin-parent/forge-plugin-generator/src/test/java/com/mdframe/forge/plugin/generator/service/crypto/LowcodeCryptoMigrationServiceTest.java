package com.mdframe.forge.plugin.generator.service.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlRepository;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class LowcodeCryptoMigrationServiceTest {

    @Test
    void migrateShouldRequireExplicitConfigKey() {
        LowcodeCryptoMigrationService service = service(
                mapper(List.of()),
                resolver(LowcodeRuntimeDataSourceContext.master("crm_customer")),
                new FailingDdlRepository(),
                new LowcodeCryptoMigrationRepository(null, null),
                cryptoService()
        );

        assertThrows(BusinessException.class, () -> service.migrate(1L, List.of(), "v2", 100, true));
    }

    @Test
    void executeShouldBlockReadonlyRuntimeDatasourceBeforeDdlAccess() {
        AiCrudConfig config = new AiCrudConfig();
        config.setTenantId(1L);
        config.setConfigKey("crm_customer");
        config.setTableName("crm_customer");
        config.setEncryptConfig("""
                {"phoneNumber":{"algorithm":"SM4"}}
                """);

        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master("crm_customer");
        context.setReadonly(true);
        context.setAllowWrite(false);
        LowcodeCryptoMigrationService service = service(
                mapper(List.of(config)),
                resolver(context),
                new FailingDdlRepository(),
                new LowcodeCryptoMigrationRepository(null, null),
                cryptoService()
        );

        CryptoMigrationReport report = service.migrate(1L, List.of("crm_customer"), "v2", 100, false);

        assertEquals(1L, report.getTotals().get("BLOCKED"));
        assertEquals("运行数据源只读或未开放写入", report.getItems().get(0).getReason());
    }

    @Test
    void migrateShouldRequireVersionedWrites() {
        LowcodeCryptoMigrationService service = service(
                mapper(List.of()),
                resolver(LowcodeRuntimeDataSourceContext.master("crm_customer")),
                new FailingDdlRepository(),
                new LowcodeCryptoMigrationRepository(null, null),
                cryptoService(),
                false
        );

        assertThrows(BusinessException.class,
                () -> service.migrate(1L, List.of("crm_customer"), "v2", 100, true));
    }

    @Test
    void executeShouldNotReportPartialSuccessWhenBatchUpdateFails() {
        AiCrudConfig config = new AiCrudConfig();
        config.setTenantId(1L);
        config.setConfigKey("crm_customer");
        config.setTableName("crm_customer");
        config.setEncryptConfig("""
                {"phoneNumber":{"algorithm":"SM4"}}
                """);
        BatchFailingMigrationRepository migrationRepository = new BatchFailingMigrationRepository();
        LowcodeCryptoMigrationService service = service(
                mapper(List.of(config)),
                resolver(LowcodeRuntimeDataSourceContext.master("crm_customer")),
                new MigrationDdlRepository(),
                migrationRepository,
                legacyCryptoService()
        );

        CryptoMigrationReport report = service.migrate(1L, List.of("crm_customer"), "v2", 100, false);

        assertFalse(report.getTotals().containsKey("MIGRATED"));
        assertEquals(1L, report.getTotals().get("FAILED"));
        assertEquals(1, migrationRepository.rollbacks.get());
    }

    private LowcodeCryptoMigrationService service(AiCrudConfigMapper configMapper,
                                                  LowcodeRuntimeDataSourceResolver resolver,
                                                  LowcodeDdlRepository ddlRepository,
                                                  LowcodeCryptoMigrationRepository migrationRepository,
                                                  PersistentCryptoService cryptoService) {
        return service(configMapper, resolver, ddlRepository, migrationRepository, cryptoService, true);
    }

    private LowcodeCryptoMigrationService service(AiCrudConfigMapper configMapper,
                                                   LowcodeRuntimeDataSourceResolver resolver,
                                                   LowcodeDdlRepository ddlRepository,
                                                   LowcodeCryptoMigrationRepository migrationRepository,
                                                   PersistentCryptoService cryptoService,
                                                   boolean writeVersioned) {
        CryptoProperties properties = new CryptoProperties();
        properties.getPersistence().setActiveKeyId("v2");
        properties.getPersistence().setWriteVersioned(writeVersioned);
        return new LowcodeCryptoMigrationService(
                configMapper,
                resolver,
                ddlRepository,
                new LowcodeEncryptConfigParser(new ObjectMapper()),
                migrationRepository,
                cryptoService,
                properties
        );
    }

    private AiCrudConfigMapper mapper(List<AiCrudConfig> configs) {
        return (AiCrudConfigMapper) Proxy.newProxyInstance(
                AiCrudConfigMapper.class.getClassLoader(),
                new Class<?>[]{AiCrudConfigMapper.class},
                (proxy, method, args) -> {
                    if ("selectEncryptConfigCandidates".equals(method.getName())) {
                        return configs;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private LowcodeRuntimeDataSourceResolver resolver(LowcodeRuntimeDataSourceContext context) {
        return new LowcodeRuntimeDataSourceResolver(new ObjectMapper(), null) {
            @Override
            public LowcodeRuntimeDataSourceContext resolve(AiCrudConfig config) {
                return context;
            }
        };
    }

    private PersistentCryptoService cryptoService() {
        return new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                return ciphertext;
            }

            @Override
            public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
                return new PersistentCiphertext(PersistentCiphertext.Format.ACTIVE, legacyAlgorithm, "v2", ciphertext, null);
            }

            @Override
            public String reencrypt(String ciphertext, String legacyAlgorithm) {
                return ciphertext;
            }
        };
    }

    private PersistentCryptoService legacyCryptoService() {
        return new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                return ciphertext;
            }

            @Override
            public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
                return new PersistentCiphertext(
                        PersistentCiphertext.Format.LEGACY, legacyAlgorithm, null, ciphertext, null);
            }

            @Override
            public String reencrypt(String ciphertext, String legacyAlgorithm) {
                return "FPC1:SM4:v2:" + ciphertext;
            }
        };
    }

    private static final class FailingDdlRepository extends LowcodeDdlRepository {
        private FailingDdlRepository() {
            super(null, null);
        }

        @Override
        public boolean tableExists(LowcodeRuntimeDataSourceContext context, String tableName) {
            fail("readonly datasource must block before DDL metadata access");
            return false;
        }
    }

    private static final class MigrationDdlRepository extends LowcodeDdlRepository {

        private MigrationDdlRepository() {
            super(null, null);
        }

        @Override
        public boolean tableExists(LowcodeRuntimeDataSourceContext context, String tableName) {
            return true;
        }

        @Override
        public Set<String> listColumns(LowcodeRuntimeDataSourceContext context, String tableName) {
            return Set.of("id", "tenant_id", "del_flag", "phone_number");
        }

        @Override
        public List<PrimaryKeyMetadata> listPrimaryKeys(LowcodeRuntimeDataSourceContext context, String tableName) {
            return List.of(new PrimaryKeyMetadata("id", "bigint", true));
        }

        @Override
        public Map<String, ColumnMetadata> listColumnMetadata(
                LowcodeRuntimeDataSourceContext context, String tableName) {
            return Map.of("phone_number", new ColumnMetadata(
                    "phone_number", "varchar(500)", "YES", null, "", "", ""));
        }
    }

    private static final class BatchFailingMigrationRepository extends LowcodeCryptoMigrationRepository {

        private final AtomicInteger updates = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();

        private BatchFailingMigrationRepository() {
            super(null, null);
        }

        @Override
        public void validateIdentifier(String identifier) {
        }

        @Override
        public void validateRuntimeConditions(LowcodeRuntimeDataSourceContext context, Set<String> columns, Long tenantId) {
        }

        @Override
        public <T> T executeInTransaction(LowcodeRuntimeDataSourceContext context, Supplier<T> callback) {
            try {
                return callback.get();
            } catch (RuntimeException e) {
                rollbacks.incrementAndGet();
                throw e;
            }
        }

        @Override
        public List<RowCipher> selectBatch(LowcodeRuntimeDataSourceContext context,
                                           String tableName,
                                           String primaryKeyColumn,
                                           String fieldColumn,
                                           Set<String> columns,
                                           Long tenantId,
                                           Object afterPrimaryKey,
                                           int batchSize) {
            if (afterPrimaryKey != null) {
                return List.of();
            }
            return List.of(new RowCipher(1L, "legacy-1"), new RowCipher(2L, "legacy-2"));
        }

        @Override
        public int updateIfMatch(LowcodeRuntimeDataSourceContext context,
                                 String tableName,
                                 String primaryKeyColumn,
                                 Object primaryKeyValue,
                                 String fieldColumn,
                                 String oldCipher,
                                 String newCipher,
                                 Set<String> columns,
                                 Long tenantId) {
            if (updates.incrementAndGet() == 2) {
                throw new IllegalStateException("simulated update failure");
            }
            return 1;
        }
    }
}
