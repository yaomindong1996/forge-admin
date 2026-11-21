package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerDialectTest {

    @Test
    void tableQueryBindsKeywordInsteadOfInterpolatingIt() throws Exception {
        SqlServerDialect dialect = new SqlServerDialect();
        String payload = "%' ) UNION SELECT username, password, 'x' FROM sys_user -- ";
        String sql = dialect.getTableQuerySql("demo", payload);

        assertThat(sql).contains("TABLE_CATALOG = ?");
        assertThat(sql).contains("LIKE ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain("sys_user");
        assertThat(sql).doesNotContain(payload);

        List<String> bound = new ArrayList<>();
        dialect.bindTableQuery(recordingStatement(bound), "demo", payload);
        assertThat(bound).containsExactly("demo", "%" + escaped(payload) + "%", "%" + escaped(payload) + "%");
    }

    @Test
    void columnQueryBindsTableNameInsteadOfInterpolatingIt() throws Exception {
        SqlServerDialect dialect = new SqlServerDialect();
        String tableName = "Orders' UNION SELECT user(), 'b', 'c', 'd', 'e' -- ";
        String sql = dialect.getColumnQuerySql("demo", tableName);

        assertThat(sql).contains("TABLE_CATALOG = ?");
        assertThat(sql).contains("TABLE_NAME = ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain(tableName);

        List<String> bound = new ArrayList<>();
        dialect.bindColumnQuery(recordingStatement(bound), "demo", tableName);
        assertThat(bound).containsExactly("demo", tableName);
    }

    @Test
    void quoteIdentifierEscapesBrackets() {
        SqlServerDialect dialect = new SqlServerDialect();
        assertThat(dialect.quoteIdentifier("orders")).isEqualTo("[orders]");
        assertThat(dialect.quoteIdentifier("a]b; DROP TABLE t")).isEqualTo("[a]]b; DROP TABLE t]");
    }

    @Test
    void buildLimitSqlUsesOffsetFetchAndInjectsOrderBy() {
        SqlServerDialect dialect = new SqlServerDialect();
        assertThat(dialect.buildLimitSql("SELECT 1", 10))
                .isEqualTo("SELECT 1 ORDER BY (SELECT NULL) OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
        assertThat(dialect.buildLimitSql("SELECT 1 ORDER BY id", 5))
                .isEqualTo("SELECT 1 ORDER BY id OFFSET 0 ROWS FETCH NEXT 5 ROWS ONLY");
    }

    private static String escaped(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static PreparedStatement recordingStatement(List<String> bound) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setString".equals(method.getName())) {
                        bound.add((String) args[1]);
                        return null;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "PreparedStatement";
                    }
                    return null;
                });
    }
}
