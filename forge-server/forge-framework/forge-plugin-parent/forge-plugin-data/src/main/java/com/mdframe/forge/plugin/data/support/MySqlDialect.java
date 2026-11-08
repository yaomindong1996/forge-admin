package com.mdframe.forge.plugin.data.support;

import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class MySqlDialect implements DbDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("标识符不能为空");
        }
        // 反引号必须成对转义，否则表名里的 ` 会提前结束标识符
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String buildLimitSql(String sql, int limit) {
        return sql + " LIMIT " + limit;
    }

    @Override
    public String getTableQuerySql(String schemaName, String keyword) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT TABLE_NAME AS tableName, TABLE_TYPE AS tableType, TABLE_COMMENT AS tableComment ");
        sql.append("FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? ");
        if (hasKeyword(keyword)) {
            sql.append("AND (TABLE_NAME LIKE ? ESCAPE '\\\\' OR TABLE_COMMENT LIKE ? ESCAPE '\\\\') ");
        }
        sql.append("ORDER BY TABLE_NAME");
        return sql.toString();
    }

    @Override
    public String getColumnQuerySql(String schemaName, String tableName) {
        return "SELECT COLUMN_NAME AS columnName, COLUMN_TYPE AS columnType, "
                + "COLUMN_COMMENT AS columnComment, IS_NULLABLE AS nullable, COLUMN_KEY AS primaryKey "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? "
                + "ORDER BY ORDINAL_POSITION";
    }

    @Override
    public void bindTableQuery(PreparedStatement statement, String schemaName, String keyword) throws SQLException {
        statement.setString(1, schemaName);
        if (hasKeyword(keyword)) {
            String pattern = toContainsPattern(keyword);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
        }
    }

    @Override
    public void bindColumnQuery(PreparedStatement statement, String schemaName, String tableName) throws SQLException {
        statement.setString(1, schemaName);
        statement.setString(2, tableName);
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