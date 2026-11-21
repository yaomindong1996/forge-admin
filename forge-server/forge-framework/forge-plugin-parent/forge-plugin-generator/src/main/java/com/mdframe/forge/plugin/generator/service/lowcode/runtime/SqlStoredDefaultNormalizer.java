package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一清洗数据库 COLUMN_DEFAULT / 字段默认值，避免 PG {@code 'DRAFT'::character varying}
 * 在对比与 DDL 回写时被二次加引号写成 {@code '''DRAFT''::...}。
 */
public final class SqlStoredDefaultNormalizer {

    private static final Pattern PG_CAST = Pattern.compile(
            "^'(.*)'::[\\w\\s.\\[\\]()]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MYSQL_BIT = Pattern.compile("^b'([01])'$", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTED = Pattern.compile("^'(.*)'$|^\"(.*)\"$", Pattern.DOTALL);
    private static final Pattern NUMERIC = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "tinyint", "smallint", "int", "integer", "bigint", "decimal", "numeric", "double", "float", "real", "number"
    );

    private SqlStoredDefaultNormalizer() {
    }

    /**
     * 去掉库侧装饰（类型 cast、外层引号、bit 字面量、括号），得到可对比/可再写出的语义值。
     */
    public static String stripDecorators(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty() || "NULL".equalsIgnoreCase(text)) {
            return null;
        }
        Matcher cast = PG_CAST.matcher(text);
        if (cast.matches()) {
            text = cast.group(1).replace("''", "'");
        }
        Matcher bit = MYSQL_BIT.matcher(text);
        if (bit.matches()) {
            return bit.group(1);
        }
        if (text.startsWith("(") && text.endsWith(")") && text.length() > 2) {
            text = text.substring(1, text.length() - 1).trim();
        }
        Matcher quoted = QUOTED.matcher(text);
        if (quoted.matches()) {
            text = StringUtils.defaultString(quoted.group(1), quoted.group(2));
            text = text.replace("''", "'");
        }
        text = text.trim();
        return text.isEmpty() || "NULL".equalsIgnoreCase(text) ? null : text;
    }

    /** 对比用归一化：表达式保留大写关键字形态，其余走 {@link #stripDecorators}。 */
    public static String normalizeForCompare(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        if (raw instanceof Number number) {
            return stripTrailingZero(number.toString());
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "NULL".equalsIgnoreCase(text)) {
            return null;
        }
        if (isExpressionDefault(text)) {
            return canonicalizeExpression(text);
        }
        String stripped = stripDecorators(text);
        if (stripped == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(stripped)) {
            return "1";
        }
        if ("false".equalsIgnoreCase(stripped)) {
            return "0";
        }
        if (NUMERIC.matcher(stripped).matches()) {
            try {
                return stripTrailingZero(new BigDecimal(stripped).stripTrailingZeros().toPlainString());
            } catch (NumberFormatException ignored) {
                return stripped;
            }
        }
        return stripped;
    }

    public static boolean equalsNormalized(Object expected, Object actual) {
        return Objects.equals(normalizeForCompare(expected), normalizeForCompare(actual));
    }

    /**
     * DDL 写出前准备：布尔→0/1，去掉库侧装饰；表达式原样返回；数字尽量转 Number。
     */
    public static Object prepareForDdl(Object raw) {
        return prepareForDdl(raw, null);
    }

    public static Object prepareForDdl(Object raw, String sqlType) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (raw instanceof Number) {
            return raw;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "NULL".equalsIgnoreCase(text)) {
            return null;
        }
        if (text.startsWith("$forge:")) {
            return null;
        }
        if (isExpressionDefault(text)) {
            return canonicalizeExpression(text);
        }
        String stripped = stripDecorators(text);
        if (stripped == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(stripped)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(stripped)) {
            return 0;
        }
        if (isNumericSqlType(sqlType) || NUMERIC.matcher(stripped).matches()) {
            if (NUMERIC.matcher(stripped).matches()) {
                if (stripped.contains(".")) {
                    return new BigDecimal(stripped);
                }
                try {
                    long number = Long.parseLong(stripped);
                    if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                        return (int) number;
                    }
                    return number;
                } catch (NumberFormatException ignored) {
                    return stripped;
                }
            }
        }
        return stripped;
    }

    public static boolean isExpressionDefault(String value) {
        String normalized = StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.equals("CURRENT_TIMESTAMP")
                || normalized.equals("CURRENT_TIMESTAMP()")
                || normalized.matches("CURRENT_TIMESTAMP\\(\\d+\\)")
                || normalized.equals("CURRENT_DATE")
                || normalized.equals("CURRENT_DATE()")
                || normalized.equals("CURRENT_TIME")
                || normalized.equals("CURRENT_TIME()")
                || normalized.equals("NOW()")
                || normalized.equals("LOCALTIMESTAMP")
                || normalized.equals("LOCALTIMESTAMP()")
                || normalized.equals("SYSDATE")
                || normalized.equals("SYSTIMESTAMP")
                || normalized.equals("GETDATE()")
                || normalized.equals("SYSDATETIME()")
                || normalized.startsWith("NEXTVAL(")
                || normalized.startsWith("B'")
                || normalized.startsWith("X'");
    }

    public static String normalizeTypeAlias(String sqlType) {
        String normalized = StringUtils.defaultString(sqlType).trim().toLowerCase(Locale.ROOT);
        int paren = normalized.indexOf('(');
        if (paren > 0) {
            normalized = normalized.substring(0, paren);
        }
        normalized = normalized.replaceAll("\\s+", " ");
        return switch (normalized) {
            case "character varying", "varchar" -> "varchar";
            case "character", "char", "bpchar" -> "char";
            case "integer", "int", "int4" -> "int";
            case "bigint", "int8" -> "bigint";
            case "smallint", "int2" -> "smallint";
            case "numeric", "decimal", "number" -> "decimal";
            case "double precision", "float8" -> "double";
            case "real", "float4" -> "float";
            case "timestamp without time zone", "timestamp" -> "timestamp";
            case "timestamp with time zone", "timestamptz" -> "timestamptz";
            case "boolean", "bool" -> "boolean";
            case "text" -> "text";
            default -> normalized;
        };
    }

    public static boolean sameTypeAlias(String expected, String actual) {
        return normalizeTypeAlias(expected).equals(normalizeTypeAlias(actual));
    }

    private static boolean isNumericSqlType(String sqlType) {
        String alias = normalizeTypeAlias(sqlType);
        return NUMERIC_TYPES.contains(alias) || alias.startsWith("decimal") || alias.startsWith("numeric");
    }

    private static String canonicalizeExpression(String value) {
        String text = value.trim();
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.startsWith("B'") || upper.startsWith("X'") || upper.startsWith("NEXTVAL(")) {
            return text;
        }
        return upper;
    }

    private static String stripTrailingZero(String number) {
        if (!number.contains(".")) {
            return number;
        }
        try {
            return new BigDecimal(number).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return number;
        }
    }
}
