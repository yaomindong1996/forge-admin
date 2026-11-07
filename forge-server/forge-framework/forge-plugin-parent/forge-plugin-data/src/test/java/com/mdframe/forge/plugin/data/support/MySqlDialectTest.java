package com.mdframe.forge.plugin.data.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlDialectTest {

    @Test
    void tableQueryBindsKeywordInsteadOfInterpolatingIt() throws Exception {
        MySqlDialect dialect = new MySqlDialect();
        String payload = "%' ) UNION SELECT username, password, 'x' FROM sys_user -- ";
        String sql = dialect.getTableQuerySql("forge_admin", payload);

        assertThat(sql).contains("TABLE_SCHEMA = ?");
        assertThat(sql).contains("LIKE ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain("sys_user");
        assertThat(sql).doesNotContain(payload);

        List<String> bound = new ArrayList<>();
        PreparedStatement statement = recordingStatement(bound);
        dialect.bindTableQuery(statement, "forge_admin", payload);

        assertThat(bound).containsExactly("forge_admin", "%" + escaped(payload) + "%", "%" + escaped(payload) + "%");
    }

    @Test
    void columnQueryBindsTableNameInsteadOfInterpolatingIt() throws Exception {
        MySqlDialect dialect = new MySqlDialect();
        String tableName = "orders' UNION SELECT user(), 'b', 'c', 'd', 'e' -- ";
        String sql = dialect.getColumnQuerySql("forge_admin", tableName);

        assertThat(sql).contains("TABLE_SCHEMA = ?");
        assertThat(sql).contains("TABLE_NAME = ?");
        assertThat(sql).doesNotContain("UNION");
        assertThat(sql).doesNotContain(tableName);

        List<String> bound = new ArrayList<>();
        dialect.bindColumnQuery(recordingStatement(bound), "forge_admin", tableName);
        assertThat(bound).containsExactly("forge_admin", tableName);
    }

    @Test
    void quoteIdentifierEscapesBackticks() {
        MySqlDialect dialect = new MySqlDialect();
        assertThat(dialect.quoteIdentifier("orders")).isEqualTo("`orders`");
        assertThat(dialect.quoteIdentifier("a`b; DROP TABLE t"))
                .isEqualTo("`a``b; DROP TABLE t`");
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
