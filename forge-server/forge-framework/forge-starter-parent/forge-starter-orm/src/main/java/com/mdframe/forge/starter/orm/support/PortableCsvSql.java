package com.mdframe.forge.starter.orm.support;

/**
 * 跨库 CSV/逗号分隔字段包含判断。
 * <p>
 * 不要用 MySQL {@code FIND_IN_SET} 或 PostgreSQL {@code string_to_array}；
 * 统一用 {@code CONCAT + LIKE}，MySQL / PostgreSQL / H2 等均可执行。
 * </p>
 * <p>
 * Mapper 中推荐写法（{@code column} 为列或表达式，{@code token} 为 {@code #{param}}）：
 * </p>
 * <pre>
 * CONCAT(',',
 *   REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(column, ''), ';', ','), '；', ','), '，', ','), '、', ','), ' ', ''),
 * ',') LIKE CONCAT('%,', #{token}, ',%')
 * </pre>
 */
public final class PortableCsvSql {

    private PortableCsvSql() {
    }

    /** 归一化分隔符后的两侧补逗号，便于 LIKE 精确命中整 token。 */
    public static final String NORMALIZE_SEPARATORS =
            "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(%s, ';', ','), '；', ','), '，', ','), '、', ','), ' ', '')";

    public static String containsPredicate(String columnExpression, String tokenPlaceholder) {
        String normalized = NORMALIZE_SEPARATORS.formatted("COALESCE(" + columnExpression + ", '')");
        return "CONCAT(',', " + normalized + ", ',') LIKE CONCAT('%,', " + tokenPlaceholder + ", ',%')";
    }
}
