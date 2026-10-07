package com.mdframe.forge.starter.plugin.version;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 插件协议限定为 AND 比较式，不隐式支持 npm 的通配、OR、caret 等语法。 */
public final class VersionRange {

    private static final Pattern COMPARISON = Pattern.compile("(>=|<=|>|<|=)?(.+)");

    private final List<Comparison> comparisons;

    private VersionRange(List<Comparison> comparisons) {
        this.comparisons = comparisons;
    }

    public static VersionRange parse(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("核心版本兼容范围不能为空");
        }
        // 先解析全部条件；不能短路匹配后漏掉后半段非法协议。
        List<Comparison> comparisons = Arrays.stream(expression.strip().split("\\s+"))
                .map(VersionRange::parseComparison).toList();
        return new VersionRange(comparisons);
    }

    public boolean contains(String version) {
        SemanticVersion current = SemanticVersion.parse(version);
        return comparisons.stream().allMatch(comparison -> comparison.matches(current));
    }

    private static Comparison parseComparison(String token) {
        Matcher matcher = COMPARISON.matcher(token);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("非法版本比较式：" + token);
        }
        String operator = matcher.group(1) == null ? "=" : matcher.group(1);
        return new Comparison(operator, SemanticVersion.parse(matcher.group(2)));
    }

    private record Comparison(String operator, SemanticVersion expected) {

        private boolean matches(SemanticVersion actual) {
            int result = actual.compareTo(expected);
            return switch (operator) {
                case ">=" -> result >= 0;
                case ">" -> result > 0;
                case "<=" -> result <= 0;
                case "<" -> result < 0;
                case "=" -> result == 0;
                default -> throw new IllegalArgumentException("不支持的版本运算符：" + operator);
            };
        }
    }
}
