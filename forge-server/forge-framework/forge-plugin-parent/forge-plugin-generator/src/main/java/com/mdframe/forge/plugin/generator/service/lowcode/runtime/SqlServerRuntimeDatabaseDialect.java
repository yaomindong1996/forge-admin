package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * SQL Server 运行时方言（业务库 / 低代码动态数据源）。
 * <p>
 * 管理端选项值为 {@code SQLServer}；标识符使用 {@code [name]}；分页为 {@code OFFSET/FETCH}。
 */
@Component
public class SqlServerRuntimeDatabaseDialect implements RuntimeDatabaseDialect {

    @Override
    public String dbType() {
        return "SQLServer";
    }

    @Override
    public boolean supports(String dbType) {
        return StringUtils.equalsAnyIgnoreCase(dbType,
                "SQLServer", "sqlserver", "mssql", "Microsoft SQL Server");
    }

    @Override
    public String quote(String identifier) {
        return "[" + StringUtils.defaultString(identifier).replace("]", "]]") + "]";
    }

    @Override
    public int maxIdentifierLength() {
        return 128;
    }

    @Override
    public String tableExistsSql() {
        return """
            SELECT COUNT(1)
            FROM information_schema.tables
            WHERE table_schema = SCHEMA_NAME()
              AND table_name = ?
              AND table_type = 'BASE TABLE'
            """;
    }

    @Override
    public String listColumnsSql() {
        return """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = SCHEMA_NAME()
              AND table_name = ?
            ORDER BY ordinal_position
            """;
    }

    @Override
    public String listColumnMetadataSql() {
        return """
            SELECT c.column_name,
                   CASE
                       WHEN c.data_type IN ('varchar', 'nvarchar', 'char', 'nchar')
                            AND c.character_maximum_length IS NOT NULL
                            AND c.character_maximum_length > 0
                           THEN c.data_type + '(' + CAST(c.character_maximum_length AS varchar(16)) + ')'
                       WHEN c.data_type IN ('varchar', 'nvarchar')
                            AND c.character_maximum_length = -1
                           THEN c.data_type + '(max)'
                       WHEN c.data_type IN ('decimal', 'numeric')
                           THEN c.data_type + '(' + CAST(c.numeric_precision AS varchar(16))
                                + ',' + CAST(c.numeric_scale AS varchar(16)) + ')'
                       ELSE c.data_type
                   END AS column_type,
                   c.is_nullable,
                   c.column_default,
                   CASE WHEN COLUMNPROPERTY(OBJECT_ID(QUOTENAME(c.table_schema) + '.' + QUOTENAME(c.table_name)),
                                            c.column_name, 'IsIdentity') = 1
                        THEN 'identity' ELSE '' END AS extra,
                   ISNULL(CAST(ep.value AS nvarchar(500)), '') AS column_comment,
                   '' AS generation_expression
            FROM information_schema.columns c
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = OBJECT_ID(QUOTENAME(c.table_schema) + '.' + QUOTENAME(c.table_name))
             AND ep.minor_id = c.ordinal_position
             AND ep.name = 'MS_Description'
             AND ep.class = 1
            WHERE c.table_schema = SCHEMA_NAME()
              AND c.table_name = ?
            ORDER BY c.ordinal_position
            """;
    }

    @Override
    public String listIndexesSql() {
        return """
            SELECT i.name AS index_name
            FROM sys.indexes i
            INNER JOIN sys.tables t ON i.object_id = t.object_id
            INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
            WHERE s.name = SCHEMA_NAME()
              AND t.name = ?
              AND i.name IS NOT NULL
            """;
    }

    @Override
    public String primaryKeyMetadataSql() {
        return """
            SELECT kcu.column_name, c.data_type,
                   CASE WHEN COLUMNPROPERTY(OBJECT_ID(QUOTENAME(kcu.table_schema) + '.' + QUOTENAME(kcu.table_name)),
                                            kcu.column_name, 'IsIdentity') = 1
                        THEN 'identity' ELSE '' END AS extra
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
             AND tc.table_schema = kcu.table_schema
             AND tc.table_name = kcu.table_name
            JOIN information_schema.columns c
              ON c.table_schema = kcu.table_schema
             AND c.table_name = kcu.table_name
             AND c.column_name = kcu.column_name
            WHERE tc.constraint_type = 'PRIMARY KEY'
              AND tc.table_schema = SCHEMA_NAME()
              AND tc.table_name = ?
            ORDER BY kcu.ordinal_position
            """;
    }

    @Override
    public String paginate(String sql, long offset, long limit) {
        // SQL Server 要求 OFFSET/FETCH 前必须有 ORDER BY；调用方未带时注入稳定占位
        String base = sql;
        if (sql == null || !sql.toLowerCase(Locale.ROOT).matches("(?s).*\\border\\s+by\\b.*")) {
            base = sql + " ORDER BY (SELECT NULL)";
        }
        return base + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String stringAggregate(String column, String orderColumn, String delimiter) {
        return "STRING_AGG(CAST(" + column + " AS nvarchar(max)), '"
            + delimiter.replace("'", "''") + "') WITHIN GROUP (ORDER BY " + orderColumn + ")";
    }

    @Override
    public String resolveSqlType(String dataType, int length, String decimalPrecision) {
        return switch (dataType) {
            case "varchar" -> "nvarchar(" + length + ")";
            case "char" -> "nchar(" + length + ")";
            case "text", "longtext", "json" -> "nvarchar(max)";
            case "date" -> "date";
            case "datetime" -> "datetime2";
            case "time" -> "time";
            case "int" -> "int";
            case "bigint" -> "bigint";
            case "tinyint" -> "tinyint";
            case "decimal" -> "decimal(" + decimalPrecision + ")";
            default -> throw new IllegalArgumentException("不支持的数据类型: " + dataType);
        };
    }

    @Override
    public String columnDefinition(DdlColumn column) {
        StringBuilder definition = new StringBuilder();
        definition.append(ddlIdentifier(column.columnName())).append(" ").append(column.sqlType());
        if (column.identity()) {
            definition.append(" IDENTITY(1,1) NOT NULL");
        } else {
            appendDefaultValue(definition, column);
            definition.append(column.required() ? " NOT NULL" : " NULL");
        }
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
            statements.add(tableDescriptionSql(tableName, tableComment));
        }
        for (DdlColumn column : columns) {
            if (StringUtils.isNotBlank(column.comment())) {
                statements.add(columnDescriptionSql(tableName, column.columnName(), column.comment()));
            }
        }
        for (IndexDefinition index : indexes) {
            statements.add(addIndexSql(tableName, index));
        }
        return statements;
    }

    @Override
    public String addColumnSql(String tableName, DdlColumn column) {
        return "ALTER TABLE " + ddlIdentifier(tableName) + " ADD " + columnDefinition(column);
    }

    @Override
    public List<String> afterAddColumnSql(String tableName, DdlColumn column) {
        if (StringUtils.isBlank(column.comment())) {
            return List.of();
        }
        return List.of(columnDescriptionSql(tableName, column.columnName(), column.comment()));
    }

    @Override
    public List<String> modifyColumnSql(String tableName, DdlColumn column) {
        // 默认值约束需按名 DROP CONSTRAINT，此处仅改类型/空值；注释走扩展属性
        List<String> statements = new ArrayList<>();
        String nullability = column.required() ? " NOT NULL" : " NULL";
        statements.add("ALTER TABLE " + ddlIdentifier(tableName) + " ALTER COLUMN "
            + ddlIdentifier(column.columnName()) + " " + column.sqlType() + nullability);
        if (StringUtils.isNotBlank(column.comment())) {
            statements.add(columnDescriptionSql(tableName, column.columnName(), column.comment()));
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
        return normalizeSqlServerType(expected).equals(normalizeSqlServerType(actual));
    }

    @Override
    public String listImportTablesSql() {
        return """
            SELECT t.table_name,
                   ISNULL(CAST(ep.value AS nvarchar(500)), t.table_name) AS table_comment,
                   NULL AS create_time,
                   NULL AS update_time
            FROM information_schema.tables t
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = OBJECT_ID(QUOTENAME(t.table_schema) + '.' + QUOTENAME(t.table_name))
             AND ep.minor_id = 0
             AND ep.name = 'MS_Description'
             AND ep.class = 1
            WHERE t.table_schema = SCHEMA_NAME()
              AND t.table_type = 'BASE TABLE'
              AND t.table_name NOT LIKE 'qrtz_%'
              AND t.table_name NOT LIKE 'gen_%'
            ORDER BY t.table_name
            """;
    }

    @Override
    public String importTableInfoSql() {
        return """
            SELECT t.table_name,
                   ISNULL(CAST(ep.value AS nvarchar(500)), t.table_name) AS table_comment,
                   NULL AS create_time,
                   NULL AS update_time
            FROM information_schema.tables t
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = OBJECT_ID(QUOTENAME(t.table_schema) + '.' + QUOTENAME(t.table_name))
             AND ep.minor_id = 0
             AND ep.name = 'MS_Description'
             AND ep.class = 1
            WHERE t.table_schema = SCHEMA_NAME()
              AND t.table_name = ?
            """;
    }

    @Override
    public String listImportColumnsSql() {
        return """
            SELECT c.column_name,
                   ISNULL(CAST(ep.value AS nvarchar(500)), '') AS column_comment,
                   CASE
                       WHEN c.data_type IN ('varchar', 'nvarchar', 'char', 'nchar')
                            AND c.character_maximum_length IS NOT NULL
                            AND c.character_maximum_length > 0
                           THEN c.data_type + '(' + CAST(c.character_maximum_length AS varchar(16)) + ')'
                       WHEN c.data_type IN ('varchar', 'nvarchar')
                            AND c.character_maximum_length = -1
                           THEN c.data_type + '(max)'
                       WHEN c.data_type IN ('decimal', 'numeric')
                           THEN c.data_type + '(' + CAST(c.numeric_precision AS varchar(16))
                                + ',' + CAST(c.numeric_scale AS varchar(16)) + ')'
                       ELSE c.data_type
                   END AS column_type,
                   CASE WHEN kcu.column_name IS NOT NULL THEN 1 ELSE 0 END AS is_pk,
                   CASE WHEN COLUMNPROPERTY(OBJECT_ID(QUOTENAME(c.table_schema) + '.' + QUOTENAME(c.table_name)),
                                            c.column_name, 'IsIdentity') = 1 THEN 1 ELSE 0 END AS is_increment,
                   CASE WHEN c.is_nullable = 'NO' AND kcu.column_name IS NULL THEN 1 ELSE 0 END AS is_required,
                   c.column_default
            FROM information_schema.columns c
            LEFT JOIN information_schema.table_constraints tc
              ON tc.table_schema = c.table_schema
             AND tc.table_name = c.table_name
             AND tc.constraint_type = 'PRIMARY KEY'
            LEFT JOIN information_schema.key_column_usage kcu
              ON kcu.table_schema = tc.table_schema
             AND kcu.table_name = tc.table_name
             AND kcu.constraint_name = tc.constraint_name
             AND kcu.column_name = c.column_name
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = OBJECT_ID(QUOTENAME(c.table_schema) + '.' + QUOTENAME(c.table_name))
             AND ep.minor_id = c.ordinal_position
             AND ep.name = 'MS_Description'
             AND ep.class = 1
            WHERE c.table_schema = SCHEMA_NAME()
              AND c.table_name = ?
            ORDER BY c.ordinal_position
            """;
    }

    private void appendDefaultValue(StringBuilder definition, DdlColumn column) {
        if (column.defaultValue() == null) {
            return;
        }
        definition.append(" DEFAULT ").append(defaultLiteral(column));
    }

    private String defaultLiteral(DdlColumn column) {
        Object normalized = SqlStoredDefaultNormalizer.prepareForDdl(column.defaultValue(), column.sqlType());
        if (normalized == null) {
            return "NULL";
        }
        String value = Objects.toString(normalized, "").trim();
        if (SqlStoredDefaultNormalizer.isExpressionDefault(value)
                || isNumericType(column.sqlType())
                || normalized instanceof Number) {
            return value;
        }
        return "N'" + escapeSqlComment(value) + "'";
    }

    private boolean isNumericType(String sqlType) {
        String alias = SqlStoredDefaultNormalizer.normalizeTypeAlias(sqlType);
        return alias.equals("int")
            || alias.equals("bigint")
            || alias.equals("tinyint")
            || alias.equals("smallint")
            || alias.equals("decimal")
            || alias.equals("numeric")
            || alias.equals("double")
            || alias.equals("float");
    }

    private String normalizeObjectName(String value) {
        String normalized = StringUtils.defaultString(value)
            .replaceAll("[^a-zA-Z0-9_]", "_")
            .replaceAll("_+", "_");
        if (normalized.length() > maxIdentifierLength()) {
            return normalized.substring(0, maxIdentifierLength());
        }
        return normalized;
    }

    private String normalizeSqlServerType(String value) {
        String normalized = Objects.toString(value, "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", "");
        if (normalized.startsWith("varchar(")) {
            return normalized.replace("varchar", "nvarchar");
        }
        if (normalized.equals("datetime") || normalized.startsWith("datetime2")) {
            return "datetime2";
        }
        return normalized;
    }

    private String tableDescriptionSql(String tableName, String comment) {
        return "EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'"
            + escapeSqlComment(comment) + "', "
            + "@level0type=N'SCHEMA', @level0name=SCHEMA_NAME(), "
            + "@level1type=N'TABLE', @level1name=N'" + escapeSqlComment(tableName) + "'";
    }

    private String columnDescriptionSql(String tableName, String columnName, String comment) {
        return "EXEC sp_addextendedproperty @name=N'MS_Description', @value=N'"
            + escapeSqlComment(comment) + "', "
            + "@level0type=N'SCHEMA', @level0name=SCHEMA_NAME(), "
            + "@level1type=N'TABLE', @level1name=N'" + escapeSqlComment(tableName) + "', "
            + "@level2type=N'COLUMN', @level2name=N'" + escapeSqlComment(columnName) + "'";
    }

    private String escapeSqlComment(String value) {
        return StringUtils.defaultString(value).replace("'", "''");
    }
}
