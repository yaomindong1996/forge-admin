package com.mdframe.forge.plugin.data.support;

import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SQL Server 数据集 / 元数据方言。
 * <p>
 * 连接解析出的「schemaName」在 SQL Server 场景下实际是 catalog（databaseName）；
 * 对象 schema 固定取当前 {@code SCHEMA_NAME()}（通常为 dbo）。
 */
@Component
public class SqlServerDialect implements DbDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("标识符不能为空");
        }
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String buildLimitSql(String sql, int limit) {
        int safeLimit = Math.max(0, limit);
        String base = sql;
        if (!hasOrderBy(sql)) {
            base = sql + " ORDER BY (SELECT NULL)";
        }
        return base + " OFFSET 0 ROWS FETCH NEXT " + safeLimit + " ROWS ONLY";
    }

    @Override
    public String getTableQuerySql(String schemaName, String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.TABLE_NAME AS tableName, t.TABLE_TYPE AS tableType, ");
        sql.append("ISNULL(CAST(ep.value AS nvarchar(500)), '') AS tableComment ");
        sql.append("FROM information_schema.TABLES t ");
        sql.append("LEFT JOIN sys.extended_properties ep ");
        sql.append("  ON ep.major_id = OBJECT_ID(QUOTENAME(t.TABLE_SCHEMA) + '.' + QUOTENAME(t.TABLE_NAME)) ");
        sql.append(" AND ep.minor_id = 0 AND ep.class = 1 AND ep.name = 'MS_Description' ");
        sql.append("WHERE t.TABLE_CATALOG = ? AND t.TABLE_SCHEMA = SCHEMA_NAME() ");
        if (hasKeyword(keyword)) {
            sql.append("AND (t.TABLE_NAME LIKE ? ESCAPE '\\' OR ISNULL(CAST(ep.value AS nvarchar(500)), '') LIKE ? ESCAPE '\\') ");
        }
        sql.append("ORDER BY t.TABLE_NAME");
        return sql.toString();
    }

    @Override
    public String getColumnQuerySql(String schemaName, String tableName) {
        return "SELECT c.COLUMN_NAME AS columnName, "
                + "CASE "
                + "  WHEN c.DATA_TYPE IN ('varchar', 'nvarchar', 'char', 'nchar') "
                + "       AND c.CHARACTER_MAXIMUM_LENGTH IS NOT NULL AND c.CHARACTER_MAXIMUM_LENGTH > 0 "
                + "    THEN c.DATA_TYPE + '(' + CAST(c.CHARACTER_MAXIMUM_LENGTH AS varchar(16)) + ')' "
                + "  WHEN c.DATA_TYPE IN ('varchar', 'nvarchar') AND c.CHARACTER_MAXIMUM_LENGTH = -1 "
                + "    THEN c.DATA_TYPE + '(max)' "
                + "  WHEN c.DATA_TYPE IN ('decimal', 'numeric') "
                + "    THEN c.DATA_TYPE + '(' + CAST(c.NUMERIC_PRECISION AS varchar(16)) "
                + "         + ',' + CAST(c.NUMERIC_SCALE AS varchar(16)) + ')' "
                + "  ELSE c.DATA_TYPE "
                + "END AS columnType, "
                + "ISNULL(CAST(ep.value AS nvarchar(500)), '') AS columnComment, "
                + "c.IS_NULLABLE AS nullable, "
                + "CASE WHEN kcu.COLUMN_NAME IS NOT NULL THEN 'PRI' ELSE '' END AS primaryKey "
                + "FROM information_schema.COLUMNS c "
                + "LEFT JOIN information_schema.TABLE_CONSTRAINTS tc "
                + "  ON tc.TABLE_CATALOG = c.TABLE_CATALOG AND tc.TABLE_SCHEMA = c.TABLE_SCHEMA "
                + " AND tc.TABLE_NAME = c.TABLE_NAME AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY' "
                + "LEFT JOIN information_schema.KEY_COLUMN_USAGE kcu "
                + "  ON kcu.TABLE_CATALOG = tc.TABLE_CATALOG AND kcu.TABLE_SCHEMA = tc.TABLE_SCHEMA "
                + " AND kcu.TABLE_NAME = tc.TABLE_NAME AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                + " AND kcu.COLUMN_NAME = c.COLUMN_NAME "
                + "LEFT JOIN sys.extended_properties ep "
                + "  ON ep.major_id = OBJECT_ID(QUOTENAME(c.TABLE_SCHEMA) + '.' + QUOTENAME(c.TABLE_NAME)) "
                + " AND ep.minor_id = c.ORDINAL_POSITION AND ep.class = 1 AND ep.name = 'MS_Description' "
                + "WHERE c.TABLE_CATALOG = ? AND c.TABLE_SCHEMA = SCHEMA_NAME() AND c.TABLE_NAME = ? "
                + "ORDER BY c.ORDINAL_POSITION";
    }

    @Override
    public void bindTableQuery(PreparedStatement statement, String schemaName, String keyword) throws SQLException {
        statement.setString(1, resolveCatalog(schemaName));
        if (hasKeyword(keyword)) {
            String pattern = toContainsPattern(keyword);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
        }
    }

    @Override
    public void bindColumnQuery(PreparedStatement statement, String schemaName, String tableName) throws SQLException {
        statement.setString(1, resolveCatalog(schemaName));
        statement.setString(2, tableName == null ? "" : tableName);
    }

    private static String resolveCatalog(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return "";
        }
        return schemaName.trim();
    }

    private static boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isEmpty();
    }

    private static boolean hasOrderBy(String sql) {
        return sql != null && sql.toLowerCase().matches("(?s).*\\border\\s+by\\b.*");
    }

    /** LIKE 通配符按字面量处理，避免关键字里的 % / _ 扩大匹配。 */
    static String toContainsPattern(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
