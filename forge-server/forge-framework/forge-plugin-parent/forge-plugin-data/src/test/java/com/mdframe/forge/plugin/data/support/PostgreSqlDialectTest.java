package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDialectTest {

    @Test
    void tableQueryBindsKeywordInsteadOfInterpolatingIt() throws Exception {
        PostgreSqlDialect dialect = new PostgreSqlDialect();
        String payload = "%' ) UNION SELECT username, password, 'x' FROM sys_user -- ";
        String sql = dialect.getTableQuerySql("public", payload);

        assertThat(sql).contains("n.nspname = ?");
        assertThat(sql).contains("LIKE ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain("sys_user");
        assertThat(sql).doesNotContain(payload);

        List<String> bound = new ArrayList<>();
        dialect.bindTableQuery(recordingStatement(bound), "public", payload);
        assertThat(bound).containsExactly("public", "%" + escaped(payload) + "%", "%" + escaped(payload) + "%");
    }

    @Test
    void columnQueryBindsTableNameInsteadOfInterpolatingIt() throws Exception {
        PostgreSqlDialect dialect = new PostgreSqlDialect();
        String tableName = "Orders' UNION SELECT user(), 'b', 'c', 'd', 'e' -- ";
        String sql = dialect.getColumnQuerySql("public", tableName);

        assertThat(sql).contains("n.nspname = ?");
        assertThat(sql).contains("c.relname = ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain(tableName);

        List<String> bound = new ArrayList<>();
        dialect.bindColumnQuery(recordingStatement(bound), "public", tableName);
        assertThat(bound).containsExactly("public", tableName.toLowerCase());
    }

    @Test
    void quoteIdentifierEscapesDoubleQuotes() {
        PostgreSqlDialect dialect = new PostgreSqlDialect();
        assertThat(dialect.quoteIdentifier("orders")).isEqualTo("\"orders\"");
        assertThat(dialect.quoteIdentifier("a\"b; DROP TABLE t"))
                .isEqualTo("\"a\"\"b; DROP TABLE t\"");
    }

    @Test
    void buildLimitSqlAppendsLimit() {
        PostgreSqlDialect dialect = new PostgreSqlDialect();
        assertThat(dialect.buildLimitSql("SELECT 1", 10)).isEqualTo("SELECT 1 LIMIT 10");
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
