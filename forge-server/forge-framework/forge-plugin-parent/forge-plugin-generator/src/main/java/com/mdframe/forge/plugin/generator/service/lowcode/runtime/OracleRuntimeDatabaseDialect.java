package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Oracle 运行时方言。
 */
@Component
public class OracleRuntimeDatabaseDialect implements RuntimeDatabaseDialect {

    @Override
    public String dbType() {
        return "Oracle";
    }

    @Override
    public boolean supports(String dbType) {
        return StringUtils.equalsAnyIgnoreCase(dbType, "oracle", "oracle12c");
    }

    @Override
    public String quote(String identifier) {
        return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
    }

    @Override
    public String ddlIdentifier(String identifier) {
        return StringUtils.defaultString(identifier).toUpperCase(Locale.ROOT);
    }

    @Override
    public int maxIdentifierLength() {
        return 30;
    }

    @Override
    public String defaultTestQuery() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public String tableExistsSql() {
        return """
            SELECT COUNT(1)
            FROM all_tables
            WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND table_name = UPPER(?)
            """;
    }

    @Override
    public String listColumnsSql() {
        return """
            SELECT column_name
            FROM all_tab_columns
            WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND table_name = UPPER(?)
            ORDER BY column_id
            """;
    }

    @Override
    public String listColumnMetadataSql() {
        return """
            SELECT c.column_name,
                   CASE
                       WHEN c.data_type IN ('VARCHAR2', 'NVARCHAR2', 'CHAR', 'NCHAR')
                           THEN c.data_type || '(' || c.char_length || ')'
                       WHEN c.data_type = 'NUMBER' AND c.data_precision IS NOT NULL AND c.data_scale IS NOT NULL
                           THEN c.data_type || '(' || c.data_precision || ',' || c.data_scale || ')'
                       WHEN c.data_type = 'NUMBER' AND c.data_precision IS NOT NULL
                           THEN c.data_type || '(' || c.data_precision || ')'
                       ELSE c.data_type
                   END AS column_type,
                   CASE c.nullable WHEN 'N' THEN 'NO' ELSE 'YES' END AS is_nullable,
                   c.data_default AS column_default,
                   c.identity_column AS extra,
                   NVL(cc.comments, '') AS column_comment,
                   '' AS generation_expression
            FROM all_tab_columns c
            LEFT JOIN all_col_comments cc
              ON cc.owner = c.owner
             AND cc.table_name = c.table_name
             AND cc.column_name = c.column_name
            WHERE c.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND c.table_name = UPPER(?)
            ORDER BY c.column_id
            """;
    }

    @Override
    public String listIndexesSql() {
        return """
            SELECT index_name
            FROM all_indexes
            WHERE owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND table_name = UPPER(?)
            """;
    }

    @Override
    public String primaryKeyMetadataSql() {
        return """
            SELECT cols.column_name,
                   CASE
                       WHEN tab_cols.data_type = 'NUMBER' AND tab_cols.data_precision IS NOT NULL AND tab_cols.data_scale IS NOT NULL
                           THEN tab_cols.data_type || '(' || tab_cols.data_precision || ',' || tab_cols.data_scale || ')'
                       WHEN tab_cols.data_type = 'NUMBER' AND tab_cols.data_precision IS NOT NULL
                           THEN tab_cols.data_type || '(' || tab_cols.data_precision || ')'
                       ELSE tab_cols.data_type
                   END AS data_type,
                   tab_cols.identity_column AS extra
            FROM all_constraints cons
            JOIN all_cons_columns cols
              ON cons.owner = cols.owner
             AND cons.constraint_name = cols.constraint_name
            JOIN all_tab_columns tab_cols
              ON tab_cols.owner = cols.owner
             AND tab_cols.table_name = cols.table_name
             AND tab_cols.column_name = cols.column_name
            WHERE cons.constraint_type = 'P'
              AND cons.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND cons.table_name = UPPER(?)
            ORDER BY cols.position
            """;
    }

    @Override
    public String paginate(String sql, long offset, long limit) {
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String resolveSqlType(String dataType, int length, String decimalPrecision) {
        return switch (dataType) {
            case "varchar" -> "VARCHAR2(" + length + " CHAR)";
            case "char" -> "CHAR(" + length + " CHAR)";
            case "text", "longtext" -> "CLOB";
            case "date" -> "DATE";
            case "datetime" -> "TIMESTAMP";
            case "time" -> "VARCHAR2(16 CHAR)";
            case "int" -> "NUMBER(10)";
            case "bigint" -> "NUMBER(19)";
            case "tinyint" -> "NUMBER(3)";
            case "decimal" -> "NUMBER(" + decimalPrecision + ")";
            default -> throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        };
    }

    @Override
    public String columnDefinition(DdlColumn column) {
        StringBuilder definition = new StringBuilder();
        definition.append(ddlIdentifier(column.columnName())).append(" ").append(column.sqlType());
        if (column.identity()) {
            definition.append(" GENERATED BY DEFAULT AS IDENTITY");
        }
        appendDefaultValue(definition, column);
        definition.append(column.required() || column.identity() ? " NOT NULL" : " NULL");
        return definition.toString();
    }

    @Override
    public String primaryKeyConstraint(String tableName, String columnName) {
        return "CONSTRAINT " + ddlIdentifier(normalizeObjectName("pk_" + tableName))
            + " PRIMARY KEY (" + ddlIdentifier(columnName) + ")";
    }

    @Override
    public String inlineIndexDefinition(IndexDefinition index) {
        return null;
    }

    @Override
    public String createTableSql(String tableName, List<String> definitions, String tableComment) {
        return "CREATE TABLE " + ddlIdentifier(tableName) + " (\n  "
            + String.join(",\n  ", definitions)
            + "\n)";
    }

    @Override
    public List<String> afterCreateTableSql(String tableName,
                                            String tableComment,
                                            List<DdlColumn> columns,
                                            List<IndexDefinition> indexes) {
        List<String> statements = new ArrayList<>();
        if (StringUtils.isNotBlank(tableComment)) {
            statements.add("COMMENT ON TABLE " + ddlIdentifier(tableName)
                + " IS '" + escapeSqlComment(tableComment) + "'");
        }
        for (DdlColumn column : columns) {
            if (StringUtils.isNotBlank(column.comment())) {
                statements.add("COMMENT ON COLUMN " + ddlIdentifier(tableName) + "."
                    + ddlIdentifier(column.columnName()) + " IS '" + escapeSqlComment(column.comment()) + "'");
            }
        }
        for (IndexDefinition index : indexes) {
            statements.add(addIndexSql(tableName, index));
        }
        return statements;
    }

    @Override
    public String addColumnSql(String tableName, DdlColumn column) {
        return "ALTER TABLE " + ddlIdentifier(tableName) + " ADD (" + columnDefinition(column) + ")";
    }

    @Override
    public List<String> afterAddColumnSql(String tableName, DdlColumn column) {
        if (StringUtils.isBlank(column.comment())) {
            return List.of();
        }
        return List.of("COMMENT ON COLUMN " + ddlIdentifier(tableName) + "."
            + ddlIdentifier(column.columnName()) + " IS '" + escapeSqlComment(column.comment()) + "'");
    }

    @Override
    public List<String> modifyColumnSql(String tableName, DdlColumn column) {
        List<String> statements = new ArrayList<>();
        statements.add("ALTER TABLE " + ddlIdentifier(tableName) + " MODIFY (" + columnDefinition(column) + ")");
        if (StringUtils.isNotBlank(column.comment())) {
            statements.add("COMMENT ON COLUMN " + ddlIdentifier(tableName) + "."
                + ddlIdentifier(column.columnName()) + " IS '" + escapeSqlComment(column.comment()) + "'");
        }
        return statements;
    }

    @Override
    public String addIndexSql(String tableName, IndexDefinition index) {
        String columns = index.columns().stream()
            .map(this::ddlIdentifier)
            .collect(Collectors.joining(", "));
        return "CREATE " + (index.unique() ? "UNIQUE " : "") + "INDEX "
            + ddlIdentifier(normalizeObjectName(index.indexName())) + " ON "
            + ddlIdentifier(tableName) + " (" + columns + ")";
    }

    @Override
    public boolean sameSqlType(String expected, String actual) {
        return normalizeOracleType(expected).equals(normalizeOracleType(actual));
    }

    @Override
    public String listImportTablesSql() {
        return """
            SELECT t.table_name,
                   NVL(c.comments, t.table_name) AS table_comment,
                   CAST(NULL AS TIMESTAMP) AS create_time,
                   CAST(NULL AS TIMESTAMP) AS update_time
            FROM all_tables t
            LEFT JOIN all_tab_comments c
              ON c.owner = t.owner
             AND c.table_name = t.table_name
            WHERE t.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND t.table_name NOT LIKE 'QRTZ\\_%' ESCAPE '\\'
              AND t.table_name NOT LIKE 'GEN\\_%' ESCAPE '\\'
            ORDER BY t.table_name
            """;
    }

    @Override
    public String importTableInfoSql() {
        return """
            SELECT t.table_name,
                   NVL(c.comments, t.table_name) AS table_comment,
                   CAST(NULL AS TIMESTAMP) AS create_time,
                   CAST(NULL AS TIMESTAMP) AS update_time
            FROM all_tables t
            LEFT JOIN all_tab_comments c
              ON c.owner = t.owner
             AND c.table_name = t.table_name
            WHERE t.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND t.table_name = UPPER(?)
            """;
    }

    @Override
    public String listImportColumnsSql() {
        return """
            SELECT c.column_name,
                   NVL(cc.comments, '') AS column_comment,
                   CASE
                       WHEN c.data_type IN ('VARCHAR2', 'NVARCHAR2', 'CHAR', 'NCHAR')
                           THEN c.data_type || '(' || c.char_length || ')'
                       WHEN c.data_type = 'NUMBER' AND c.data_precision IS NOT NULL AND c.data_scale IS NOT NULL
                           THEN c.data_type || '(' || c.data_precision || ',' || c.data_scale || ')'
                       WHEN c.data_type = 'NUMBER' AND c.data_precision IS NOT NULL
                           THEN c.data_type || '(' || c.data_precision || ')'
                       ELSE c.data_type
                   END AS column_type,
                   CASE WHEN pk.column_name IS NOT NULL THEN 1 ELSE 0 END AS is_pk,
                   CASE WHEN c.identity_column = 'YES' THEN 1 ELSE 0 END AS is_increment,
                   CASE WHEN c.nullable = 'N' AND pk.column_name IS NULL THEN 1 ELSE 0 END AS is_required,
                   c.data_default AS column_default
            FROM all_tab_columns c
            LEFT JOIN all_col_comments cc
              ON cc.owner = c.owner
             AND cc.table_name = c.table_name
             AND cc.column_name = c.column_name
            LEFT JOIN (
                SELECT cols.owner, cols.table_name, cols.column_name
                FROM all_constraints cons
                JOIN all_cons_columns cols
                  ON cons.owner = cols.owner
                 AND cons.constraint_name = cols.constraint_name
                WHERE cons.constraint_type = 'P'
                  AND cons.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
                  AND cons.table_name = UPPER(?)
            ) pk
              ON pk.owner = c.owner
             AND pk.table_name = c.table_name
             AND pk.column_name = c.column_name
            WHERE c.owner = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')
              AND c.table_name = UPPER(?)
            ORDER BY c.column_id
            """;
    }

    @Override
    public int listImportColumnsTableNameParameterCount() {
        return 2;
    }

    private void appendDefaultValue(StringBuilder definition, DdlColumn column) {
        if (column.defaultValue() == null || isLargeText(column.sqlType())) {
            return;
        }
        Object normalized = normalizeDefaultValue(column.defaultValue());
        String value = String.valueOf(normalized).trim();
        if (StringUtils.isBlank(value) || "NULL".equalsIgnoreCase(value)) {
            return;
        }
        definition.append(" DEFAULT ");
        if (isExpressionDefault(value) || isNumericType(column.sqlType()) || normalized instanceof Number) {
            definition.append(value);
        } else {
            definition.append("'").append(escapeSqlComment(value)).append("'");
        }
    }

    private Object normalizeDefaultValue(Object defaultValue) {
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
        return defaultValue;
    }

    private boolean isNumericType(String sqlType) {
        return StringUtils.defaultString(sqlType).toUpperCase(Locale.ROOT).startsWith("NUMBER");
    }

    private boolean isLargeText(String sqlType) {
        return "CLOB".equalsIgnoreCase(StringUtils.defaultString(sqlType).trim());
    }

    private boolean isExpressionDefault(String value) {
        String normalized = StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
        return normalized.equals("CURRENT_TIMESTAMP")
            || normalized.equals("CURRENT_DATE")
            || normalized.equals("SYSDATE")
            || normalized.startsWith("SYSTIMESTAMP");
    }

    private String normalizeObjectName(String value) {
        String normalized = StringUtils.defaultString(value)
            .replaceAll("[^a-zA-Z0-9_]", "_")
            .replaceAll("_+", "_")
            .toLowerCase(Locale.ROOT);
        if (normalized.length() > maxIdentifierLength()) {
            return normalized.substring(0, maxIdentifierLength());
        }
        return normalized;
    }

    private String normalizeOracleType(String value) {
        String normalized = Objects.toString(value, "")
            .toUpperCase(Locale.ROOT)
            .replace(" CHAR", "")
            .replaceAll("\\s+", "");
        if (normalized.equals("INTEGER")) {
            return "NUMBER(10)";
        }
        if (normalized.equals("BIGINT")) {
            return "NUMBER(19)";
        }
        if (normalized.matches("NUMBER\\(\\d+,0\\)")) {
            return normalized.replace(",0)", ")");
        }
        if (normalized.equals("TIMESTAMP(6)")) {
            return "TIMESTAMP";
        }
        return normalized;
    }

    private String escapeSqlComment(String value) {
        return StringUtils.defaultString(value).replace("'", "''");
    }
}
