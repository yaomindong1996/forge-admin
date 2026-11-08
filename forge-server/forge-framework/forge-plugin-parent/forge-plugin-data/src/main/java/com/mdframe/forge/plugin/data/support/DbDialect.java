package com.mdframe.forge.plugin.data.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface DbDialect {

    String quoteIdentifier(String identifier);

    String buildLimitSql(String sql, int limit);

    /**
     * 表清单 SQL。库名与关键字只通过 {@link #bindTableQuery} 绑定，禁止拼进语句。
     */
    String getTableQuerySql(String schemaName, String keyword);

    /**
     * 列清单 SQL。库名与表名只通过 {@link #bindColumnQuery} 绑定，禁止拼进语句。
     */
    String getColumnQuerySql(String schemaName, String tableName);

    void bindTableQuery(PreparedStatement statement, String schemaName, String keyword) throws SQLException;

    void bindColumnQuery(PreparedStatement statement, String schemaName, String tableName) throws SQLException;
}