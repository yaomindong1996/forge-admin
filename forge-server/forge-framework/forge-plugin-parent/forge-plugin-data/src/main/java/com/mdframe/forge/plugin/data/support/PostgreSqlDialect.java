package com.mdframe.forge.plugin.data.support;

import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class PostgreSqlDialect implements DbDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("标识符不能为空");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String buildLimitSql(String sql, int limit) {
        return sql + " LIMIT " + Math.max(0, limit);
    }

    @Override
    public String getTableQuerySql(String schemaName, String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.relname AS tableName, ");
        sql.append("CASE c.relkind WHEN 'r' THEN 'BASE TABLE' WHEN 'v' THEN 'VIEW' ELSE c.relkind::text END AS tableType, ");
        sql.append("COALESCE(obj_description(c.oid), '') AS tableComment ");
        sql.append("FROM pg_class c ");
        sql.append("JOIN pg_namespace n ON n.oid = c.relnamespace ");
        sql.append("WHERE n.nspname = ? AND c.relkind IN ('r', 'v', 'p') ");
        if (hasKeyword(keyword)) {
            sql.append("AND (c.relname LIKE ? ESCAPE '\\' OR COALESCE(obj_description(c.oid), '') LIKE ? ESCAPE '\\') ");
        }
        sql.append("ORDER BY c.relname");
        return sql.toString();
    }

    @Override
    public String getColumnQuerySql(String schemaName, String tableName) {
        return "SELECT a.attname AS columnName, "
                + "pg_catalog.format_type(a.atttypid, a.atttypmod) AS columnType, "
                + "COALESCE(col_description(a.attrelid, a.attnum), '') AS columnComment, "
                + "CASE WHEN a.attnotnull THEN 'NO' ELSE 'YES' END AS nullable, "
                + "CASE WHEN EXISTS ("
                + "  SELECT 1 FROM pg_constraint con "
                + "  WHERE con.conrelid = a.attrelid AND con.contype = 'p' AND a.attnum = ANY (con.conkey)"
                + ") THEN 'PRI' ELSE '' END AS primaryKey "
                + "FROM pg_attribute a "
                + "JOIN pg_class c ON c.oid = a.attrelid "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = ? AND c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped "
                + "ORDER BY a.attnum";
    }

    @Override
    public void bindTableQuery(PreparedStatement statement, String schemaName, String keyword) throws SQLException {
        statement.setString(1, resolveSchema(schemaName));
        if (hasKeyword(keyword)) {
            String pattern = toContainsPattern(keyword);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
        }
    }

    @Override
    public void bindColumnQuery(PreparedStatement statement, String schemaName, String tableName) throws SQLException {
        statement.setString(1, resolveSchema(schemaName));
        statement.setString(2, tableName == null ? "" : tableName.toLowerCase());
    }

    private static String resolveSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return "public";
        }
        return schemaName;
    }

    private static boolean hasKeyword(String keyword) {
        return keyword != null && !keyword.isEmpty();
    }

    /** LIKE 通配符按字面量处理，避免关键字里的 % / _ 扩大匹配。 */
    static String toContainsPattern(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
