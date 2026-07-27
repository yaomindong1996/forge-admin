package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceContext;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialectFactory;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeJdbcTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 低代码受控 DDL 仓储。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LowcodeDdlRepository {

    private static final Pattern TYPE_LENGTH_PATTERN = Pattern.compile("\\((\\d+)");

    private final RuntimeJdbcTemplateProvider jdbcTemplateProvider;
    private final RuntimeDatabaseDialectFactory dialectFactory;

    public boolean tableExists(String tableName) {
        return tableExists(LowcodeRuntimeDataSourceContext.master(tableName), tableName);
    }

    public boolean tableExists(LowcodeRuntimeDataSourceContext context, String tableName) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.jdbcTemplate(context);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        Integer count = jdbcTemplate.queryForObject(dialect.tableExistsSql(), Integer.class, tableName);
        return count != null && count > 0;
    }

    public Set<String> listColumns(String tableName) {
        return listColumns(LowcodeRuntimeDataSourceContext.master(tableName), tableName);
    }

    public Set<String> listColumns(LowcodeRuntimeDataSourceContext context, String tableName) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.jdbcTemplate(context);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        List<String> columns = jdbcTemplate.queryForList(dialect.listColumnsSql(), String.class, tableName);
        return columns.stream()
                .map(this::normalizeIdentifier)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public Map<String, ColumnMetadata> listColumnMetadata(String tableName) {
        return listColumnMetadata(LowcodeRuntimeDataSourceContext.master(tableName), tableName);
    }

    public Map<String, ColumnMetadata> listColumnMetadata(LowcodeRuntimeDataSourceContext context, String tableName) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.jdbcTemplate(context);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        List<ColumnMetadata> columns = jdbcTemplate.query(dialect.listColumnMetadataSql(), (rs, rowNum) -> new ColumnMetadata(
                normalizeIdentifier(rs.getString("column_name")),
                rs.getString("column_type"),
                rs.getString("is_nullable"),
                rs.getObject("column_default"),
                rs.getString("extra"),
                rs.getString("column_comment"),
                rs.getString("generation_expression")
        ), tableName);
        return columns.stream().collect(Collectors.toMap(ColumnMetadata::columnName, column -> column));
    }

    public Set<String> listIndexes(String tableName) {
        return listIndexes(LowcodeRuntimeDataSourceContext.master(tableName), tableName);
    }

    public Set<String> listIndexes(LowcodeRuntimeDataSourceContext context, String tableName) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.jdbcTemplate(context);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        List<String> indexes = jdbcTemplate.queryForList(dialect.listIndexesSql(), String.class, tableName);
        return indexes.stream()
                .map(this::normalizeIdentifier)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public boolean hasAutoIncrementPrimaryId(String tableName) {
        return hasAutoIncrementPrimaryId(LowcodeRuntimeDataSourceContext.master(tableName), tableName);
    }

    public boolean hasAutoIncrementPrimaryId(LowcodeRuntimeDataSourceContext context, String tableName) {
        List<PrimaryKeyMetadata> primaryKeys = listPrimaryKeys(context, tableName);
        if (primaryKeys.size() != 1) {
            return false;
        }
        PrimaryKeyMetadata primaryKey = primaryKeys.get(0);
        return "id".equalsIgnoreCase(primaryKey.columnName())
                && isBigIntegerPrimaryType(primaryKey.dataType())
                && primaryKey.autoIncrement();
    }

    public boolean hasSinglePrimaryKey(LowcodeRuntimeDataSourceContext context, String tableName) {
        return listPrimaryKeys(context, tableName).size() == 1;
    }

    /**
     * 返回字符列容量；-1 表示 text/clob 等可变大字段，0 表示非字符或无法安全判断。
     */
    public int characterCapacity(ColumnMetadata column) {
        if (column == null || column.columnType() == null) {
            return 0;
        }
        String type = column.columnType().toLowerCase(Locale.ROOT).trim();
        if (type.contains("text") || type.contains("clob")) {
            return -1;
        }
        if (!(type.contains("char") || type.contains("varchar") || type.contains("string"))) {
            return 0;
        }
        Matcher matcher = TYPE_LENGTH_PATTERN.matcher(type);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public List<PrimaryKeyMetadata> listPrimaryKeys(LowcodeRuntimeDataSourceContext context, String tableName) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.jdbcTemplate(context);
        RuntimeDatabaseDialect dialect = dialectFactory.resolve(context);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dialect.primaryKeyMetadataSql(), tableName);
        List<PrimaryKeyMetadata> primaryKeys = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String columnName = normalizeIdentifier(text(row.get("column_name")));
            String dataType = text(row.get("data_type"));
            String extra = text(row.get("extra"));
            primaryKeys.add(new PrimaryKeyMetadata(columnName, dataType, isAutoIncrement(extra)));
        }
        return primaryKeys;
    }

    public void executeDdl(String ddl) {
        executeDdl(LowcodeRuntimeDataSourceContext.master(null), ddl);
    }

    public void executeDdl(LowcodeRuntimeDataSourceContext context, String ddl) {
        log.info("[LowcodeDdlRepository] 执行低代码受控DDL: datasourceId={}, tableName={}, ddl={}",
                context == null ? null : context.getDatasourceId(),
                context == null ? null : context.getTableName(),
                ddl);
        jdbcTemplateProvider.jdbcTemplate(context).execute(ddl);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean isAutoIncrement(String extra) {
        String normalized = text(extra).toLowerCase(Locale.ROOT);
        return normalized.contains("auto_increment")
                || normalized.contains("nextval")
                || "yes".equalsIgnoreCase(normalized);
    }

    private boolean isBigIntegerPrimaryType(String dataType) {
        String normalized = text(dataType).toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (normalized.contains("bigint")) {
            return true;
        }
        if (!normalized.startsWith("number(")) {
            return false;
        }
        int start = normalized.indexOf('(');
        int end = normalized.indexOf(')', start + 1);
        if (start < 0 || end <= start) {
            return false;
        }
        String[] parts = normalized.substring(start + 1, end).split(",");
        try {
            int precision = Integer.parseInt(parts[0]);
            int scale = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return precision >= 18 && scale == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String normalizeIdentifier(String value) {
        return text(value).toLowerCase(Locale.ROOT);
    }

    public record ColumnMetadata(String columnName, String columnType, String isNullable, Object columnDefault,
                                 String extra, String columnComment, String generationExpression) {
    }

    public record PrimaryKeyMetadata(String columnName, String dataType, boolean autoIncrement) {
    }
}
