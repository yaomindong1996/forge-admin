package com.mdframe.forge.starter.orm.interceptor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将 MySQL/PG 风格的 {@code FOR UPDATE} / {@code LIMIT} 改写为 SQL Server 可执行形态。
 * <p>仅做字符串级改写，供 {@link SqlServerDialectSqlInterceptor} 与单测复用。
 * <ul>
 *   <li>{@code LIMIT n}（无 OFFSET）→ 在对应 {@code SELECT} 后注入 {@code TOP (n)}（子查询也可用）</li>
 *   <li>{@code LIMIT n OFFSET m} / {@code LIMIT m,n} → {@code OFFSET/FETCH}（分页）</li>
 * </ul>
 */
public final class SqlServerDialectSqlRewriter {

    private static final Pattern FOR_UPDATE = Pattern.compile(
            "(?is)\\s+FOR\\s+UPDATE(?:\\s+OF\\s+[\\w.,\\s]+)?(?:\\s+NOWAIT|\\s+SKIP\\s+LOCKED)?\\s*$");

    /** LIMIT offset, limit */
    private static final Pattern LIMIT_OFFSET_COMMA = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)\\s*,\\s*(\\?|\\d+)");

    /** LIMIT count OFFSET offset — 占位符顺序与 OFFSET/FETCH 相反，需交换绑定 */
    private static final Pattern LIMIT_OFFSET_KEYWORD = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)\\s+OFFSET\\s+(\\?|\\d+)");

    /** LIMIT count（排除 LIMIT x,y / LIMIT x OFFSET y） */
    private static final Pattern LIMIT_ONLY = Pattern.compile(
            "(?is)\\s+LIMIT\\s+(\\?|\\d+)(?!\\s*,)(?!\\s+OFFSET\\b)");

    private static final Pattern HAS_ORDER_BY = Pattern.compile("(?is)\\border\\s+by\\b");

    private static final Pattern SELECT_HEAD = Pattern.compile("(?is)\\bSELECT\\s+(?:DISTINCT\\s+|ALL\\s+)?");

    private static final Pattern FROM_TABLE = Pattern.compile(
            "(?is)\\bFROM\\s+((?:\\[[^\\]]+\\]|[\\w.]+))(?:\\s+(?:AS\\s+)?((?:\\[[^\\]]+\\]|\\w+)))?");

    /** MySQL 微秒差再除 1000 ≈ 毫秒；SQL Server 直接按毫秒差。 */
    private static final Pattern TIMESTAMPDIFF_MILLIS = Pattern.compile(
            "(?is)TIMESTAMPDIFF\\s*\\(\\s*MICROSECOND\\s*,\\s*([^,]+)\\s*,\\s*([^)]+)\\)\\s*DIV\\s+1000");

    public record RewriteResult(String sql, boolean swapLastTwoLimitOffsetParams, int[] parameterPermutation) {
        public static RewriteResult of(String sql) {
            return new RewriteResult(sql, false, null);
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
        String withLock = rewriteForUpdate(rewriteTimestampDiff(sql));
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

    static String rewriteTimestampDiff(String sql) {
        return TIMESTAMPDIFF_MILLIS.matcher(sql).replaceAll("DATEDIFF(MILLISECOND, $1, $2)");
    }

    static RewriteResult rewriteLimit(String sql) {
        boolean swap = false;
        String current = sql;
        int[] permutation = null;
        while (true) {
            LimitMatch match = findRightmostLimit(current);
            if (match == null) {
                break;
            }
            if (match.kind == LimitKind.ONLY) {
                TopRewrite top = rewriteLimitOnlyAsTop(current, match);
                current = top.sql;
                permutation = compose(permutation, top.step);
            } else {
                String before = current.substring(0, match.start);
                String after = current.substring(match.end);
                String scope = before.substring(Math.max(0, before.lastIndexOf('(')));
                if (!HAS_ORDER_BY.matcher(scope).find()) {
                    before = before + " ORDER BY (SELECT NULL)";
                }
                if (match.swap) {
                    swap = true;
                    permutation = compose(permutation, swapLastTwo(countPlaceholders(current)));
                }
                current = before + match.replacement + after;
            }
        }
        return new RewriteResult(current, swap, permutation);
    }

    /**
     * {@code LIMIT n} → 在所属 SELECT 后注入 {@code TOP (n)}，并删除 LIMIT。
     * SQL Server 标量子查询可用 TOP，不可用 OFFSET/FETCH。
     * 占位符会从语句尾移到 TOP，返回相对当前 SQL 的参数下标置换。
     */
    private static TopRewrite rewriteLimitOnlyAsTop(String sql, LimitMatch match) {
        int selectEnd = findEnclosingSelectHeadEnd(sql, match.start);
        if (selectEnd < 0) {
            String before = sql.substring(0, match.start);
            String after = sql.substring(match.end);
            String scope = before.substring(Math.max(0, before.lastIndexOf('(')));
            if (!HAS_ORDER_BY.matcher(scope).find()) {
                before = before + " ORDER BY (SELECT NULL)";
            }
            return new TopRewrite(before + " OFFSET 0 ROWS FETCH NEXT " + match.limitToken + " ROWS ONLY" + after, null);
        }
        String head = sql.substring(0, selectEnd);
        String afterSelect = sql.substring(selectEnd, match.start);
        if (Pattern.compile("(?is)^\\s*TOP\\s*\\(").matcher(afterSelect).lookingAt()
                || Pattern.compile("(?is)^\\s*TOP\\s+").matcher(afterSelect).lookingAt()) {
            return new TopRewrite(head + afterSelect + sql.substring(match.end), null);
        }
        int[] step = null;
        if ("?".equals(match.limitToken)) {
            int from = countPlaceholders(sql.substring(0, match.start));
            int insertAt = countPlaceholders(head);
            step = movePlaceholder(countPlaceholders(sql), from, insertAt);
        }
        return new TopRewrite(head + "TOP (" + match.limitToken + ") " + afterSelect + sql.substring(match.end), step);
    }

    private record TopRewrite(String sql, int[] step) {
    }

    /** newIndex → oldIndex。把 from 插到 insertAt。 */
    private static int[] movePlaceholder(int count, int from, int insertAt) {
        if (from == insertAt || count <= 0) {
            return null;
        }
        int[] step = new int[count];
        int w = 0;
        for (int i = 0; i < count; i++) {
            if (i == insertAt) {
                step[w++] = from;
            }
            if (i != from) {
                step[w++] = i;
            }
        }
        return w == count ? step : null;
    }

    private static int[] swapLastTwo(int count) {
        if (count < 2) {
            return null;
        }
        int[] step = new int[count];
        for (int i = 0; i < count; i++) {
            step[i] = i;
        }
        int tmp = step[count - 1];
        step[count - 1] = step[count - 2];
        step[count - 2] = tmp;
        return step;
    }

    private static int[] compose(int[] existing, int[] step) {
        if (step == null) {
            return existing;
        }
        if (existing == null) {
            return step;
        }
        int[] next = new int[step.length];
        for (int i = 0; i < step.length; i++) {
            next[i] = existing[step[i]];
        }
        return next;
    }

    private static int countPlaceholders(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    /**
     * 从 limitPos 向前找所属查询的 SELECT 头结束位置（DISTINCT/ALL 之后）。
     * 取「到 LIMIT 括号深度为 0」的最右侧 SELECT，以正确区分外层与子查询。
     */
    private static int findEnclosingSelectHeadEnd(String sql, int limitPos) {
        Matcher m = SELECT_HEAD.matcher(sql);
        m.region(0, Math.max(0, limitPos));
        int bestEnd = -1;
        while (m.find()) {
            if (parenDepth(sql, m.end(), limitPos) == 0) {
                bestEnd = m.end();
            }
        }
        return bestEnd;
    }

    private static int parenDepth(String sql, int from, int to) {
        int depth = 0;
        for (int i = from; i < to; i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
        }
        return depth;
    }

    private static LimitMatch findRightmostLimit(String sql) {
        LimitMatch best = null;
        Matcher comma = LIMIT_OFFSET_COMMA.matcher(sql);
        while (comma.find()) {
            String offset = comma.group(1);
            String limit = comma.group(2);
            best = new LimitMatch(LimitKind.OFFSET_FETCH, comma.start(), comma.end(),
                    " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY",
                    false, limit);
        }
        Matcher keyword = LIMIT_OFFSET_KEYWORD.matcher(sql);
        while (keyword.find()) {
            if (best != null && keyword.start() < best.start) {
                continue;
            }
            String limit = keyword.group(1);
            String offset = keyword.group(2);
            boolean swapParams = "?".equals(limit) && "?".equals(offset);
            best = new LimitMatch(LimitKind.OFFSET_FETCH, keyword.start(), keyword.end(),
                    " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY",
                    swapParams, limit);
        }
        Matcher only = LIMIT_ONLY.matcher(sql);
        while (only.find()) {
            if (best != null && only.start() >= best.start && only.start() < best.end) {
                continue;
            }
            if (best != null && only.start() < best.start) {
                continue;
            }
            String limit = only.group(1);
            best = new LimitMatch(LimitKind.ONLY, only.start(), only.end(), null, false, limit);
        }
        return best;
    }

    private enum LimitKind { ONLY, OFFSET_FETCH }

    private record LimitMatch(LimitKind kind, int start, int end, String replacement, boolean swap, String limitToken) {
    }

    private static boolean isJoinKeyword(String token) {
        String t = token.replace("[", "").replace("]", "").toUpperCase(Locale.ROOT);
        return t.equals("WHERE") || t.equals("INNER") || t.equals("LEFT") || t.equals("RIGHT")
                || t.equals("FULL") || t.equals("JOIN") || t.equals("CROSS") || t.equals("OUTER")
                || t.equals("ON") || t.equals("GROUP") || t.equals("ORDER") || t.equals("LIMIT")
                || t.equals("HAVING") || t.equals("UNION") || t.equals("FOR");
    }
}
