package com.mdframe.forge.plugin.data.service;

import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.crypto.migration.CryptoMigrationReport;
import com.mdframe.forge.starter.crypto.persistence.PersistentCiphertext;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataConnectionCryptoMigrationServiceTest {

    @Test
    void dryRunShouldClassifyWithoutUpdating() {
        AtomicInteger updates = new AtomicInteger();
        DataConnectionCryptoMigrationService service = new DataConnectionCryptoMigrationService(
                mapper(connections("legacy-cipher", "FPC1:SM4:v2:active"), updates),
                cryptoService(),
                properties(),
                new TestTransactionManager()
        );

        CryptoMigrationReport report = service.migrate(1L, "v2", 100, true);

        assertEquals(1L, report.getTotals().get("LEGACY"));
        assertEquals(1L, report.getTotals().get("ACTIVE"));
        assertEquals(1L, report.getTotals().get("MIGRATABLE"));
        assertEquals(0, updates.get());
    }

    @Test
    void executeShouldUpdateLegacyWithOriginalCipherCondition() {
        AtomicInteger updates = new AtomicInteger();
        DataConnectionCryptoMigrationService service = new DataConnectionCryptoMigrationService(
                mapper(connections("legacy-cipher"), updates),
                cryptoService(),
                properties(),
                new TestTransactionManager()
        );

        CryptoMigrationReport report = service.migrate(1L, "v2", 100, false);

        assertEquals(1L, report.getTotals().get("MIGRATED"));
        assertEquals(1, updates.get());
    }

    @Test
    void executeShouldNotReportPartialSuccessWhenBatchUpdateFails() {
        AtomicInteger updates = new AtomicInteger();
        DataConnectionMapper mapper = mapper(connections("legacy-1", "legacy-2"), updates, true);
        TestTransactionManager transactionManager = new TestTransactionManager();
        DataConnectionCryptoMigrationService service = new DataConnectionCryptoMigrationService(
                mapper,
                cryptoService(),
                properties(),
                transactionManager
        );

        CryptoMigrationReport report = service.migrate(1L, "v2", 100, false);

        assertFalse(report.getTotals().containsKey("MIGRATED"));
        assertEquals(1L, report.getTotals().get("FAILED"));
        assertEquals(1, transactionManager.rollbacks.get());
    }

    @Test
    void migrateShouldRequireVersionedWrites() {
        CryptoProperties properties = properties();
        properties.getPersistence().setWriteVersioned(false);
        DataConnectionCryptoMigrationService service = new DataConnectionCryptoMigrationService(
                mapper(List.of(), new AtomicInteger()),
                cryptoService(),
                properties,
                new TestTransactionManager()
        );

        assertThrows(BusinessException.class, () -> service.migrate(1L, "v2", 100, true));
    }

    private DataConnectionMapper mapper(List<DataConnection> data, AtomicInteger updates) {
        return mapper(data, updates, false);
    }

    private DataConnectionMapper mapper(List<DataConnection> data, AtomicInteger updates, boolean failSecondUpdate) {
        return (DataConnectionMapper) Proxy.newProxyInstance(
                DataConnectionMapper.class.getClassLoader(),
                new Class<?>[]{DataConnectionMapper.class},
                (proxy, method, args) -> {
                    if ("selectCryptoMigrationBatch".equals(method.getName())) {
                        Long afterId = (Long) args[1];
                        int batchSize = (Integer) args[2];
                        return data.stream()
                                .filter(item -> afterId == null || item.getId() > afterId)
                                .limit(batchSize)
                                .toList();
                    }
                    if ("updatePasswordCipherIfMatch".equals(method.getName())) {
                        int updateNumber = updates.incrementAndGet();
                        if (failSecondUpdate && updateNumber == 2) {
                            throw new IllegalStateException("simulated update failure");
                        }
                        return 1;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private List<DataConnection> connections(String... ciphers) {
        List<DataConnection> connections = new ArrayList<>();
        long id = 1L;
        for (String cipher : ciphers) {
            DataConnection connection = new DataConnection();
            connection.setId(id++);
            connection.setTenantId(1L);
            connection.setPasswordCipher(cipher);
            connections.add(connection);
        }
        return connections;
    }

    private PersistentCryptoService cryptoService() {
        return new PersistentCryptoService() {
            @Override
            public String encrypt(String plaintext, String algorithm) {
                return plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String legacyAlgorithm) {
                return "plain";
            }

            @Override
            public PersistentCiphertext inspect(String ciphertext, String legacyAlgorithm) {
                if (ciphertext.startsWith("FPC1:SM4:v2:")) {
                    return new PersistentCiphertext(
                            PersistentCiphertext.Format.ACTIVE, "SM4", "v2", "payload", null);
                }
                return new PersistentCiphertext(
                        PersistentCiphertext.Format.LEGACY, "SM4", null, ciphertext, null);
            }

            @Override
            public String reencrypt(String ciphertext, String legacyAlgorithm) {
                return "FPC1:SM4:v2:" + ciphertext;
            }
        };
    }

    private CryptoProperties properties() {
        CryptoProperties properties = new CryptoProperties();
        properties.getPersistence().setActiveKeyId("v2");
        properties.getPersistence().setWriteVersioned(true);
        return properties;
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {

        private final AtomicInteger rollbacks = new AtomicInteger();

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks.incrementAndGet();
        }
    }
}
