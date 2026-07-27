package com.mdframe.forge.plugin.generator.service.crypto;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.PostgreSqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowcodeCryptoMigrationRepositoryTest {

    @Test
    void shouldRejectUnsafeIdentifier() {
        LowcodeCryptoMigrationRepository repository = repository(new CapturingNamedJdbcTemplate());

        assertThrows(BusinessException.class, () -> repository.validateIdentifier("customer;drop"));
        assertThrows(BusinessException.class, () -> repository.validateIdentifier("customer-name"));
        assertThrows(BusinessException.class, () -> repository.validateIdentifier(""));
    }

    @Test
    void updateShouldUseQuotedIdentifiersAndBoundCipherParameters() {
        CapturingNamedJdbcTemplate jdbc = new CapturingNamedJdbcTemplate();
        LowcodeCryptoMigrationRepository repository = repository(jdbc);
        LowcodeRuntimeDataSourceContext context = context();

        int affected = repository.updateIfMatch(
                context,
                "crm_customer",
                "id",
                10L,
                "phone_cipher",
                "legacyCipher",
                "activeCipher",
                Set.of("id", "phone_cipher", "tenant_id", "del_flag"),
                1L
        );

        String sql = jdbc.sql;
        SqlParameterSource params = jdbc.params;
        assertEquals(1, affected);
        assertTrue(sql.contains("UPDATE `crm_customer` SET `phone_cipher` = :newCipher"));
        assertTrue(sql.contains("`id` = :primaryKeyValue"));
        assertTrue(sql.contains("`phone_cipher` = :oldCipher"));
        assertTrue(sql.contains("`tenant_id` = :tenantId"));
        assertTrue(sql.contains("`del_flag` = :logicActiveValue"));
        assertFalse(sql.contains("legacyCipher"));
        assertFalse(sql.contains("activeCipher"));
        assertEquals(10L, params.getValue("primaryKeyValue"));
        assertEquals("legacyCipher", params.getValue("oldCipher"));
        assertEquals("activeCipher", params.getValue("newCipher"));
        assertEquals(1L, params.getValue("tenantId"));
        assertEquals("0", params.getValue("logicActiveValue"));
    }

    @Test
    void shouldRejectMissingTenantOrLogicDeleteGuardColumns() {
        LowcodeCryptoMigrationRepository repository = repository(new CapturingNamedJdbcTemplate());
        LowcodeRuntimeDataSourceContext context = context();

        assertThrows(BusinessException.class, () -> repository.validateRuntimeConditions(
                context, Set.of("id", "phone_cipher", "del_flag"), 1L));
        assertThrows(BusinessException.class, () -> repository.validateRuntimeConditions(
                context, Set.of("id", "phone_cipher", "tenant_id"), 1L));
    }

    @Test
    void executeInTransactionShouldRollbackRuntimeDatasourceOnFailure() {
        TrackingDataSource dataSource = new TrackingDataSource();
        RuntimeJdbcTemplateProvider provider = new RuntimeJdbcTemplateProvider(
                new JdbcTemplate(dataSource), new NamedParameterJdbcTemplate(dataSource), null);
        RuntimeDatabaseDialectFactory dialectFactory = new RuntimeDatabaseDialectFactory(List.of(new TestDialect()));
        LowcodeCryptoMigrationRepository repository = new LowcodeCryptoMigrationRepository(provider, dialectFactory);

        assertThrows(IllegalStateException.class, () -> repository.executeInTransaction(context(), () -> {
            throw new IllegalStateException("simulated batch failure");
        }));

        assertEquals(1, dataSource.rollbacks.get());
    }

    @Test
    void postgresMetadataShouldExposeCharacterCapacity() {
        String sql = new PostgreSqlRuntimeDatabaseDialect().listColumnMetadataSql();

        assertTrue(sql.contains("character_maximum_length"));
    }

    private LowcodeCryptoMigrationRepository repository(NamedParameterJdbcTemplate jdbc) {
        RuntimeJdbcTemplateProvider provider = new RuntimeJdbcTemplateProvider(null, jdbc, null);
        RuntimeDatabaseDialectFactory dialectFactory = new RuntimeDatabaseDialectFactory(List.of(new TestDialect()));
        return new LowcodeCryptoMigrationRepository(provider, dialectFactory);
    }

    private LowcodeRuntimeDataSourceContext context() {
        LowcodeRuntimeDataSourceContext context = LowcodeRuntimeDataSourceContext.master("crm_customer");
        LowcodeTenantStrategy tenantStrategy = new LowcodeTenantStrategy();
        tenantStrategy.setMode("FORGE_TENANT_ID");
        tenantStrategy.setColumnName("tenant_id");
        context.setTenantStrategy(tenantStrategy);
        LowcodeLogicDeleteStrategy logicDeleteStrategy = new LowcodeLogicDeleteStrategy();
        logicDeleteStrategy.setMode("DEL_FLAG");
        logicDeleteStrategy.setColumnName("del_flag");
        logicDeleteStrategy.setActiveValue("0");
        context.setLogicDeleteStrategy(logicDeleteStrategy);
        return context;
    }

    private static final class CapturingNamedJdbcTemplate extends NamedParameterJdbcTemplate {

        private String sql;
        private SqlParameterSource params;

        private CapturingNamedJdbcTemplate() {
            super(new DriverManagerDataSource());
        }

        @Override
        public int update(String sql, SqlParameterSource paramSource) {
            this.sql = sql;
            this.params = paramSource;
            return 1;
        }
    }

    private static final class TrackingDataSource extends AbstractDataSource {

        private final AtomicInteger rollbacks = new AtomicInteger();

        @Override
        public Connection getConnection() {
            return connection();
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection();
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getAutoCommit" -> true;
                        case "rollback" -> {
                            rollbacks.incrementAndGet();
                            yield null;
                        }
                        case "isClosed" -> false;
                        case "unwrap" -> throw new SQLException("not a wrapper");
                        case "isWrapperFor" -> false;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private Object defaultValue(Class<?> returnType) {
            if (!returnType.isPrimitive()) {
                return null;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == float.class) {
                return 0F;
            }
            if (returnType == double.class) {
                return 0D;
            }
            if (returnType == char.class) {
                return '\0';
            }
            return null;
        }
    }

    private static final class TestDialect implements RuntimeDatabaseDialect {

        @Override
        public String dbType() {
            return "MySQL";
        }

        @Override
        public boolean supports(String dbType) {
            return true;
        }

        @Override
        public String quote(String identifier) {
            return "`" + identifier + "`";
        }

        @Override
        public String tableExistsSql() {
            return "";
        }

        @Override
        public String listColumnsSql() {
            return "";
        }

        @Override
        public String listColumnMetadataSql() {
            return "";
        }

        @Override
        public String listIndexesSql() {
            return "";
        }

        @Override
        public String primaryKeyMetadataSql() {
            return "";
        }

        @Override
        public String paginate(String sql, long offset, long limit) {
            return sql + " LIMIT " + limit;
        }
    }
}
