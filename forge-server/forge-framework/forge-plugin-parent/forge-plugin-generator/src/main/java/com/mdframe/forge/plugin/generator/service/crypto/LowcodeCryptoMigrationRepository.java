package com.mdframe.forge.plugin.generator.service.crypto;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeLogicDeleteStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeTenantStrategy;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 低代码动态表密文迁移仓储。
 */
@Repository
@RequiredArgsConstructor
public class LowcodeCryptoMigrationRepository {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    private final RuntimeJdbcTemplateProvider jdbcTemplateProvider;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    public List<RowCipher> selectBatch(LowcodeRuntimeDataSourceContext context,
                                       String tableName,
                                       String primaryKeyColumn,
                                       String fieldColumn,
                                       Set<String> columns,
                                       Long tenantId,
                                       Object afterPrimaryKey,
                                       int batchSize) {
        validateIdentifier(tableName);
        validateIdentifier(primaryKeyColumn);
        validateIdentifier(fieldColumn);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        NamedParameterJdbcTemplate jdbc = jdbcTemplateProvider.namedJdbcTemplate(context);
        MapSqlParameterSource params = new MapSqlParameterSource();
        String pk = dialect.quote(primaryKeyColumn);
        String field = dialect.quote(fieldColumn);
        StringBuilder sql = new StringBuilder()
                .append("SELECT ")
                .append(pk)
                .append(" AS pk_value, ")
                .append(field)
                .append(" AS cipher_value FROM ")
                .append(dialect.quote(tableName))
                .append(" WHERE ")
                .append(field)
                .append(" IS NOT NULL AND ")
                .append(field)
                .append(" <> ''");
        appendRuntimeConditions(sql, params, dialect, context, columns, tenantId);
        if (afterPrimaryKey != null) {
            sql.append(" AND ").append(pk).append(" > :afterPrimaryKey");
            params.addValue("afterPrimaryKey", afterPrimaryKey);
        }
        sql.append(" ORDER BY ").append(pk).append(" ASC");
        String pageSql = dialect.paginate(sql.toString(), 0, Math.max(1, batchSize));
        return jdbc.query(pageSql, params, (rs, rowNum) ->
                new RowCipher(rs.getObject("pk_value"), rs.getString("cipher_value")));
    }

    public int updateIfMatch(LowcodeRuntimeDataSourceContext context,
                             String tableName,
                             String primaryKeyColumn,
                             Object primaryKeyValue,
                             String fieldColumn,
                             String oldCipher,
                             String newCipher,
                             Set<String> columns,
                             Long tenantId) {
        validateIdentifier(tableName);
        validateIdentifier(primaryKeyColumn);
        validateIdentifier(fieldColumn);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        NamedParameterJdbcTemplate jdbc = jdbcTemplateProvider.namedJdbcTemplate(context);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("primaryKeyValue", primaryKeyValue)
                .addValue("oldCipher", oldCipher)
                .addValue("newCipher", newCipher);
        String pk = dialect.quote(primaryKeyColumn);
        String field = dialect.quote(fieldColumn);
        StringBuilder sql = new StringBuilder()
                .append("UPDATE ")
                .append(dialect.quote(tableName))
                .append(" SET ")
                .append(field)
                .append(" = :newCipher WHERE ")
                .append(pk)
                .append(" = :primaryKeyValue AND ")
                .append(field)
                .append(" = :oldCipher");
        appendRuntimeConditions(sql, params, dialect, context, columns, tenantId);
        return jdbc.update(sql.toString(), params);
    }

    public <T> T executeInTransaction(LowcodeRuntimeDataSourceContext context, Supplier<T> callback) {
        DataSource dataSource = jdbcTemplateProvider.resolveDataSource(context);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> callback.get());
    }

    public void validateIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier) || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new BusinessException("非法标识符: " + identifier);
        }
    }

    public void validateRuntimeConditions(LowcodeRuntimeDataSourceContext context,
                                          Set<String> columns,
                                          Long tenantId) {
        LowcodeTenantStrategy tenantStrategy = context == null ? null : context.getTenantStrategy();
        if (tenantId != null && enabled(tenantStrategy == null ? null : tenantStrategy.getMode())) {
            requireRuntimeColumn(columns, StringUtils.defaultIfBlank(
                    tenantStrategy == null ? null : tenantStrategy.getColumnName(), "tenant_id"), "租户字段列不存在");
        }

        LowcodeLogicDeleteStrategy logicDeleteStrategy = context == null ? null : context.getLogicDeleteStrategy();
        if (enabled(logicDeleteStrategy == null ? null : logicDeleteStrategy.getMode())) {
            requireRuntimeColumn(columns, StringUtils.defaultIfBlank(
                    logicDeleteStrategy == null ? null : logicDeleteStrategy.getColumnName(), "del_flag"), "逻辑删除字段列不存在");
        }
    }

    private void appendRuntimeConditions(StringBuilder sql,
                                         MapSqlParameterSource params,
                                         RuntimeDatabaseDialect dialect,
                                         LowcodeRuntimeDataSourceContext context,
                                         Set<String> columns,
                                         Long tenantId) {
        validateRuntimeConditions(context, columns, tenantId);
        LowcodeTenantStrategy tenantStrategy = context == null ? null : context.getTenantStrategy();
        if (tenantId != null && enabled(tenantStrategy == null ? null : tenantStrategy.getMode())) {
            String tenantColumn = StringUtils.defaultIfBlank(
                    tenantStrategy == null ? null : tenantStrategy.getColumnName(), "tenant_id");
            sql.append(" AND ").append(dialect.quote(tenantColumn)).append(" = :tenantId");
            params.addValue("tenantId", tenantId);
        }

        LowcodeLogicDeleteStrategy logicDeleteStrategy = context == null ? null : context.getLogicDeleteStrategy();
        if (enabled(logicDeleteStrategy == null ? null : logicDeleteStrategy.getMode())) {
            String logicColumn = StringUtils.defaultIfBlank(
                    logicDeleteStrategy == null ? null : logicDeleteStrategy.getColumnName(), "del_flag");
            sql.append(" AND ").append(dialect.quote(logicColumn)).append(" = :logicActiveValue");
            params.addValue("logicActiveValue", StringUtils.defaultIfBlank(
                    logicDeleteStrategy == null ? null : logicDeleteStrategy.getActiveValue(), "0"));
        }
    }

    private void requireRuntimeColumn(Set<String> columns, String columnName, String message) {
        validateIdentifier(columnName);
        if (!containsColumn(columns, columnName)) {
            throw new BusinessException(message + ": " + columnName);
        }
    }

    private boolean enabled(String mode) {
        return !"NONE".equalsIgnoreCase(StringUtils.defaultString(mode));
    }

    private boolean containsColumn(Set<String> columns, String columnName) {
        return columns != null && columns.contains(StringUtils.defaultString(columnName).toLowerCase(Locale.ROOT));
    }

    public record RowCipher(Object primaryKey, String cipherText) {
    }
}
