package com.mdframe.forge.plugin.data.support;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 解析拉表/列元数据用的库名（MySQL catalog / 其它库 schema）。
 * <p>
 * 优先级：连接上显式配置的 Schema → 已打开连接的 {@link Connection#getCatalog()} /
 * {@link Connection#getSchema()} → 最后才从 JDBC URL 字符串兜底。
 * URL 截取不可靠（如 {@code serverTimezone=Asia/Shanghai} 含 {@code /}），仅作兜底。
 */
public final class JdbcUrlSchemaExtractor {

    private JdbcUrlSchemaExtractor() {
    }

    /**
     * @param configuredSchema 连接配置里的 schemaName，可空
     * @param connection       已打开的 JDBC 连接，可空（空则跳过 catalog/schema）
     * @param jdbcUrl          JDBC URL，可空
     */
    public static String resolve(String configuredSchema, Connection connection, String jdbcUrl) {
        if (configuredSchema != null && !configuredSchema.isBlank()) {
            return configuredSchema.trim();
        }
        if (connection != null) {
            try {
                String catalog = connection.getCatalog();
                if (catalog != null && !catalog.isBlank()) {
                    return catalog.trim();
                }
            }
            catch (SQLException ignored) {
                // 部分驱动不支持，继续尝试 schema / URL
            }
            try {
                String schema = connection.getSchema();
                if (schema != null && !schema.isBlank()) {
                    return schema.trim();
                }
            }
            catch (SQLException ignored) {
                // fall through
            }
        }
        return extract(jdbcUrl);
    }

    /**
     * 从 JDBC URL 解析库名/schema 段（MySQL/Postgres 等 path 形态）。仅作兜底。
     * <p>
     * 不能用整串 {@code indexOf('?')} / {@code lastIndexOf('/')}：
     * 查询参数里的 {@code Asia/Shanghai} 或密码里的 {@code ?} 会导致截取错乱甚至越界。
     */
    public static String extract(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return null;
        }
        int schemeSep = jdbcUrl.indexOf("://");
        int searchFrom = schemeSep >= 0 ? schemeSep + 3 : 0;
        // 主机段之后的第一个 '/' 才是库名起点，避免查询参数里的 '/' 干扰
        int start = jdbcUrl.indexOf('/', searchFrom);
        if (start < 0 || start + 1 >= jdbcUrl.length()) {
            return null;
        }
        int end = jdbcUrl.indexOf('?', start + 1);
        if (end < 0) {
            end = jdbcUrl.length();
        }
        if (start + 1 >= end) {
            return null;
        }
        String schema = jdbcUrl.substring(start + 1, end).trim();
        return schema.isEmpty() ? null : schema;
    }
}
