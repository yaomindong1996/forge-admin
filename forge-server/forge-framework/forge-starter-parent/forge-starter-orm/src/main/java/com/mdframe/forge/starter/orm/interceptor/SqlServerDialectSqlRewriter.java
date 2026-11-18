package com.mdframe.forge.starter.orm.interceptor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 MySQL/PG 风格的 {@code FOR UPDATE} / {@code LIMIT} 改写为 SQL Server 可执行形态。
 * <p>仅做字符串级改写，供 {@link SqlServerDialectSqlInterceptor} 与单测复用。
 */
public final class SqlServerDialectSqlRewriter {

    private static final Pattern FOR_UPDATE = Pattern.compile(
            "(?is)\\s+FOR\\s+UPDATE(?:\\s+OF\\s+[\\w.,\\s]+)?(?:\\s+NOWAIT|\\s+SKIP\\s+LOCKED)?\\s*$");

    /** LIMIT offset, limit */
    private static final Pattern LIMIT_OFFSET_COMMA = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)\\s*,\\s*(\\?|\\d+)\\s*$");

    /** LIMIT count OFFSET offset — 占位符顺序与 OFFSET/FETCH 相反，需交换绑定 */
    private static final Pattern LIMIT_OFFSET_KEYWORD = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)\\s+OFFSET\\s+(\\?|\\d+)\\s*$");

    /** LIMIT count */
    private static final Pattern LIMIT_ONLY = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)\\s*$");

    private static final Pattern HAS_ORDER_BY = Pattern.compile("(?is)\\border\\s+by\\b");

    private static final Pattern FROM_TABLE = Pattern.compile(
            "(?is)\\bFROM\\s+((?:\\[[^\\]]+\\]|[\\w.]+))(?:\\s+(?:AS\\s+)?((?:\\[[^\\]]+\\]|\\w+)))?");

    public record RewriteResult(String sql, boolean swapLastTwoLimitOffsetParams) {
        public static RewriteResult of(String sql) {
            return new RewriteResult(sql, false);
        }
    }

    private SqlServerDialectSqlRewriter() {
    }

    public static String rewrite(String sql) {
        return rewriteDetailed(sql).sql();
    }

    public static RewriteResult rewriteDetailed(String sql) {
        if (sql == null || sql.isBlank()) {
            return RewriteResult.of(sql);
        }
        String withLock = rewriteForUpdate(sql);
        return rewriteLimit(withLock);
    }

    static String rewriteForUpdate(String sql) {
        Matcher forUpdate = FOR_UPDATE.matcher(sql);
        if (!forUpdate.find()) {
            return sql;
        }
        String without = forUpdate.replaceFirst("");
        if (without.toLowerCase(Locale.ROOT).contains(" with (updlock")) {
            return without.trim();
        }
        Matcher from = FROM_TABLE.matcher(without);
        if (!from.find()) {
            return without.trim();
        }
        String table = from.group(1);
        String alias = from.group(2);
        String replacement;
        if (alias != null && !alias.isBlank() && !isJoinKeyword(alias)) {
            replacement = "FROM " + table + " " + alias + " WITH (UPDLOCK, ROWLOCK)";
        } else {
            replacement = "FROM " + table + " WITH (UPDLOCK, ROWLOCK)";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(without, 0, from.start());
        sb.append(replacement);
        sb.append(without, from.end(), without.length());
        return sb.toString().trim();
    }

    static RewriteResult rewriteLimit(String sql) {
        Matcher comma = LIMIT_OFFSET_COMMA.matcher(sql);
        if (comma.find()) {
            // LIMIT offset, limit → OFFSET offset FETCH limit（占位顺序一致）
            String offset = comma.group(1);
            String limit = comma.group(2);
            String base = ensureOrderBy(comma.replaceFirst(""));
            return RewriteResult.of(base + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY");
        }
        Matcher keyword = LIMIT_OFFSET_KEYWORD.matcher(sql);
        if (keyword.find()) {
            // LIMIT count OFFSET offset → OFFSET offset FETCH count
            String limit = keyword.group(1);
            String offset = keyword.group(2);
            String base = ensureOrderBy(keyword.replaceFirst(""));
            boolean swap = "?".equals(limit) && "?".equals(offset);
            return new RewriteResult(
                    base + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY",
                    swap);
        }
        Matcher only = LIMIT_ONLY.matcher(sql);
        if (only.find()) {
            String limit = only.group(1);
            String base = ensureOrderBy(only.replaceFirst(""));
            return RewriteResult.of(base + " OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY");
        }
        return RewriteResult.of(sql);
    }

    private static String ensureOrderBy(String sql) {
        String trimmed = sql.trim();
        if (HAS_ORDER_BY.matcher(trimmed).find()) {
            return trimmed;
        }
        return trimmed + " ORDER BY (SELECT NULL)";
    }

    private static boolean isJoinKeyword(String token) {
        String t = token.replace("[", "").replace("]", "").toUpperCase(Locale.ROOT);
        return t.equals("WHERE") || t.equals("INNER") || t.equals("LEFT") || t.equals("RIGHT")
                || t.equals("FULL") || t.equals("JOIN") || t.equals("CROSS") || t.equals("OUTER")
                || t.equals("ON") || t.equals("GROUP") || t.equals("ORDER") || t.equals("LIMIT")
                || t.equals("HAVING") || t.equals("UNION") || t.equals("FOR");
    }
}
