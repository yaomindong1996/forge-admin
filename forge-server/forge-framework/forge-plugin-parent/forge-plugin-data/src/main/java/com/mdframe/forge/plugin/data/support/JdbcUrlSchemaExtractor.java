package com.mdframe.forge.plugin.data.support;

/**
 * 从 JDBC URL 解析库名/schema 段（MySQL/Postgres 等 path 形态）。
 * <p>
 * 不能用整串 {@code indexOf('?')}：账号密码里若含 {@code ?}，会早于库名前的 {@code /}，
 * 导致 {@code substring(begin, end)} 起止颠倒抛 {@link StringIndexOutOfBoundsException}。
 */
public final class JdbcUrlSchemaExtractor {

    private JdbcUrlSchemaExtractor() {
    }

    /**
     * @return 库名；无法解析时返回 null
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
