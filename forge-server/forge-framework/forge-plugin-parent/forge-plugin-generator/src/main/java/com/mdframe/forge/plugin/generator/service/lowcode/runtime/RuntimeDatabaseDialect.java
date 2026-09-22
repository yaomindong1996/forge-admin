package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 低代码运行时数据库方言。
 */
public interface RuntimeDatabaseDialect {

    record DdlColumn(String columnName,
                     String sqlType,
                     boolean required,
                     Object defaultValue,
                     String extra,
                     String comment,
                     boolean identity) {
    }

    record IndexDefinition(String indexName, List<String> columns, boolean unique) {
    }

    String dbType();

    boolean supports(String dbType);

    String quote(String identifier);

    String tableExistsSql();

    String listColumnsSql();

    String listColumnMetadataSql();

    String listIndexesSql();

    String primaryKeyMetadataSql();

    String paginate(String sql, long offset, long limit);

    default boolean supportsDdl() {
        return true;
    }

    default String defaultTestQuery() {
        return "SELECT 1";
    }

    default int maxIdentifierLength() {
        return 64;
    }

    default String ddlIdentifier(String identifier) {
        return quote(identifier);
    }

    default String resolveSqlType(String dataType, int length, String decimalPrecision) {
        return switch (dataType) {
            case "varchar" -> "varchar(" + length + ")";
            case "char" -> "char(" + length + ")";
            case "text", "longtext", "date", "datetime", "time" -> dataType;
            case "int" -> "int";
            case "bigint" -> "bigint";
            case "tinyint" -> "tinyint";
            case "decimal" -> "decimal(" + decimalPrecision + ")";
            default -> throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        };
    }

    default String columnDefinition(DdlColumn column) {
        StringBuilder definition = new StringBuilder();
        definition.append(ddlIdentifier(column.columnName())).append(" ").append(column.sqlType());
        if (column.identity()) {
            definition.append(" NOT NULL AUTO_INCREMENT");
        } else {
            definition.append(column.required() ? " NOT NULL" : " NULL");
            appendDefaultValue(definition, column.defaultValue(), !column.required(), column.sqlType());
            appendExtra(definition, column.extra());
        }
        definition.append(" COMMENT '").append(escapeSqlComment(column.comment())).append("'");
        return definition.toString();
    }

    default String primaryKeyConstraint(String tableName, String columnName) {
        return "PRIMARY KEY (" + ddlIdentifier(columnName) + ")";
    }

    default String inlineIndexDefinition(IndexDefinition index) {
        String columns = index.columns().stream()
            .map(this::ddlIdentifier)
            .collect(Collectors.joining(", "));
        return (index.unique() ? "UNIQUE KEY" : "KEY") + " "
            + ddlIdentifier(index.indexName()) + " (" + columns + ")";
    }

    default String createTableSql(String tableName, List<String> definitions, String tableComment) {
        return "CREATE TABLE IF NOT EXISTS " + ddlIdentifier(tableName) + " (\n  "
            + String.join(",\n  ", definitions)
            + "\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='"
            + escapeSqlComment(tableComment) + "'";
    }

    default List<String> afterCreateTableSql(String tableName,
                                             String tableComment,
                                             List<DdlColumn> columns,
                                             List<IndexDefinition> indexes) {
        return List.of();
    }

    default String addColumnSql(String tableName, DdlColumn column) {
        return "ALTER TABLE " + ddlIdentifier(tableName) + " ADD COLUMN " + columnDefinition(column);
    }

    default List<String> afterAddColumnSql(String tableName, DdlColumn column) {
        return List.of();
    }

    default List<String> modifyColumnSql(String tableName, DdlColumn column) {
        return List.of("ALTER TABLE " + ddlIdentifier(tableName) + " MODIFY COLUMN " + columnDefinition(column));
    }

    default String addIndexSql(String tableName, IndexDefinition index) {
        return "ALTER TABLE " + ddlIdentifier(tableName) + " ADD " + inlineIndexDefinition(index);
    }

    default boolean sameSqlType(String expected, String actual) {
        return normalizeSqlType(expected).equals(normalizeSqlType(actual));
    }

    default String listImportTablesSql() {
        return """
            SELECT table_name, table_comment, create_time, update_time
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
              AND table_name NOT LIKE 'qrtz_%'
              AND table_name NOT LIKE 'gen_%'
            ORDER BY create_time DESC
            """;
    }

    default String importTableInfoSql() {
        return """
            SELECT table_name, table_comment, create_time, update_time
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    }

    default String listImportColumnsSql() {
        return """
            SELECT column_name,
                   column_comment,
                   column_type,
                   column_default,
                   (CASE WHEN column_key = 'PRI' THEN 1 ELSE 0 END) AS is_pk,
                   (CASE WHEN extra = 'auto_increment' THEN 1 ELSE 0 END) AS is_increment,
                   (CASE WHEN is_nullable = 'NO' AND column_key != 'PRI' THEN 1 ELSE 0 END) AS is_required
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    default int listImportColumnsTableNameParameterCount() {
        return 1;
    }

    private void appendDefaultValue(StringBuilder definition, Object defaultValue, boolean nullable, String sqlType) {
        if (defaultValue == null) {
            if (nullable) {
                definition.append(" DEFAULT NULL");
            }
            return;
        }
        Object normalized = normalizeDefaultValue(defaultValue, sqlType);
        if (normalized == null) {
            if (nullable) {
                definition.append(" DEFAULT NULL");
            }
            return;
        }
        String value = String.valueOf(normalized).trim();
        if ("NULL".equalsIgnoreCase(value)) {
            if (nullable) {
                definition.append(" DEFAULT NULL");
            }
            return;
        }
        if (isExpressionDefault(value) || isUnquotedDefault(normalized, sqlType)) {
            definition.append(" DEFAULT ").append(value);
            return;
        }
        definition.append(" DEFAULT '").append(escapeSqlComment(value)).append("'");
    }

    /**
     * 开关/布尔默认值写入 tinyint 时必须是 0/1，不能落成 DEFAULT 'true'。
     */
    private Object normalizeDefaultValue(Object defaultValue, String sqlType) {
        if (defaultValue instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        String text = String.valueOf(defaultValue).trim();
        if ("true".equalsIgnoreCase(text)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(text)) {
            return 0;
        }
        if (isNumericSqlType(sqlType) && text.length() >= 2
                && ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\"")))) {
            return text.substring(1, text.length() - 1);
        }
        return defaultValue;
    }

    private boolean isUnquotedDefault(Object defaultValue, String sqlType) {
        if (defaultValue instanceof Number) {
            return true;
        }
        if (!isNumericSqlType(sqlType)) {
            return false;
        }
        String text = String.valueOf(defaultValue).trim();
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isNumericSqlType(String sqlType) {
        String normalized = normalizeSqlType(sqlType);
        return normalized.equals("int")
            || normalized.equals("bigint")
            || normalized.equals("tinyint")
            || normalized.startsWith("decimal");
    }

    private void appendExtra(StringBuilder definition, String extra) {
        String normalized = Objects.toString(extra, "").trim();
        if (normalized.isEmpty()) {
            return;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.contains("ON UPDATE CURRENT_TIMESTAMP")) {
            definition.append(" ON UPDATE CURRENT_TIMESTAMP");
        }
    }

    private boolean isExpressionDefault(String value) {
        String normalized = Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
        return normalized.equals("CURRENT_TIMESTAMP")
            || normalized.equals("CURRENT_TIMESTAMP()")
            || normalized.equals("CURRENT_DATE")
            || normalized.equals("CURRENT_DATE()")
            || normalized.equals("CURRENT_TIME")
            || normalized.equals("CURRENT_TIME()")
            || normalized.startsWith("B'")
            || normalized.startsWith("X'");
    }

    private String normalizeSqlType(String value) {
        String normalized = Objects.toString(value, "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", "");
        if (normalized.startsWith("int(")) {
            return "int";
        }
        if (normalized.startsWith("bigint(")) {
            return "bigint";
        }
        if (normalized.startsWith("tinyint(")) {
            return "tinyint";
        }
        if (normalized.startsWith("datetime(")) {
            return "datetime";
        }
        return normalized;
    }

    private String escapeSqlComment(String value) {
        return Objects.toString(value, "").replace("'", "''");
    }
}
