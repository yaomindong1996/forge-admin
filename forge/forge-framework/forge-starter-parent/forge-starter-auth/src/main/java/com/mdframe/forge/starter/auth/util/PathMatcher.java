package com.mdframe.forge.starter.auth.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 路径匹配工具类
 * 支持通配符匹配：
 * - * 匹配单层路径
 * - ** 匹配多层路径
 * - ? 匹配单个字符
 */
public class PathMatcher {

    /**
     * 判断路径是否匹配模式
     *
     * @param pattern 模式，如：/system/**、/system/user/*、sys:user:*
     * @param path    实际路径，如：/system/user/add、sys:user:add
     * @return 是否匹配
     */
    public static boolean match(String pattern, String path) {
        if (StrUtil.isBlank(pattern) || StrUtil.isBlank(path)) {
            return false;
        }

        // 完全匹配
        if (pattern.equals(path)) {
            return true;
        }

        // 通配符 * 匹配所有
        if ("*".equals(pattern) || "*:*:*".equals(pattern)) {
            return true;
        }

        // 转换为正则表达式
        String regex = convertToRegex(pattern);
        return Pattern.matches(regex, path);
    }

    /**
     * 将通配符模式转换为正则表达式
     *
     * @param pattern 通配符模式
     * @return 正则表达式
     */
    private static String convertToRegex(String pattern) {
        // 转义特殊字符
        String regex = pattern
                .replace(".", "\\.")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("+", "\\+")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|");

        // 替换通配符
        // ** 匹配任意层级（包括多层）
        regex = regex.replace("**", "DOUBLE_STAR_PLACEHOLDER");
        // * 匹配单层（不包含分隔符）
        regex = regex.replace("*", "[^/:]*");
        // 还原 **
        regex = regex.replace("DOUBLE_STAR_PLACEHOLDER", ".*");
        // ? 匹配单个字符
        regex = regex.replace("?", ".");

        // 添加边界匹配
        return "^" + regex + "$";
    }

    /**
     * 判断路径是否匹配任意一个模式
     *
     * @param patterns 模式列表
     * @param path     实际路径
     * @return 是否匹配
     */
    public static boolean matchAny(Iterable<String> patterns, String path) {
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
