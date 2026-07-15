package com.mdframe.forge.plugin.generator.util;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 下载代码命名和输出选项的统一规范化工具。
 */
public final class LowcodeCodegenOptionUtils {

    public static final String DEFAULT_BACKEND_BASE_PATH = "backend/src/main/java";
    public static final String DEFAULT_MAPPER_XML_BASE_PATH = "backend/src/main/resources/mapper";
    public static final String DEFAULT_FRONTEND_BASE_PATH = "frontend/src/views";
    public static final String DEFAULT_FRONTEND_API_BASE_PATH = "frontend/src/api";
    public static final List<String> DEFAULT_STRIP_TABLE_PREFIXES = List.of("sys_", "ai_", "t_", "tb_");
    private static final Set<String> JAVA_RESERVED_WORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "record", "sealed", "permits", "var", "yield");

    private LowcodeCodegenOptionUtils() {
    }

    public static String normalizeEntityPrefix(String value) {
        String prefix = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(prefix)) {
            return "";
        }
        if (!prefix.matches("^[A-Za-z][A-Za-z0-9_]*$")) {
            throw new BusinessException("实体类前缀只能包含字母、数字和下划线，且必须以字母开头");
        }
        String normalized = toPascalCase(prefix);
        if (!isJavaIdentifier(normalized)) {
            throw new BusinessException("实体类前缀不能生成合法 Java 标识符: " + value);
        }
        return normalized;
    }

    public static List<String> resolveStripTablePrefixes(List<String> requested,
                                                          Object existing,
                                                          Collection<String> defaults) {
        if (requested != null) {
            return normalizeTablePrefixes(requested);
        }
        List<String> configured = toStringList(existing);
        if (configured != null) {
            return normalizeTablePrefixes(configured);
        }
        return normalizeTablePrefixes(defaults == null ? DEFAULT_STRIP_TABLE_PREFIXES : defaults);
    }

    public static List<String> normalizeTablePrefixes(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String prefix = StringUtils.trimToNull(value);
            if (prefix != null) {
                result.add(prefix);
            }
        }
        return new ArrayList<>(result);
    }

    public static String buildClassName(String tableName,
                                        String entityPrefix,
                                        Collection<String> stripTablePrefixes) {
        String physicalName = StringUtils.trimToNull(tableName);
        if (physicalName == null) {
            throw new BusinessException("代码生成表名不能为空");
        }
        String stripped = stripFirstTablePrefix(physicalName, stripTablePrefixes);
        String className = normalizeEntityPrefix(entityPrefix) + toPascalCase(stripped);
        if (!isJavaIdentifier(className)) {
            throw new BusinessException("表 " + physicalName + " 不能生成合法 Java 类名: " + className);
        }
        return className;
    }

    public static String normalizeOutputPath(String value, String defaultValue, String label) {
        String normalized = StringUtils.defaultIfBlank(value, defaultValue);
        if (StringUtils.isBlank(normalized)) {
            throw new BusinessException(label + "不能为空");
        }
        normalized = normalized.trim().replace('\\', '/').replaceAll("/+$", "");
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*")) {
            throw new BusinessException(label + "必须是下载包内的相对路径");
        }
        String[] segments = normalized.split("/");
        List<String> safeSegments = new ArrayList<>();
        for (String segment : segments) {
            if (StringUtils.isBlank(segment) || ".".equals(segment) || "..".equals(segment)) {
                throw new BusinessException(label + "不能包含空目录、. 或 .. 路径段");
            }
            safeSegments.add(segment);
        }
        return String.join("/", safeSegments);
    }

    public static String toPascalCase(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String[] parts = value.split("[^A-Za-z0-9]+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    private static String stripFirstTablePrefix(String tableName, Collection<String> prefixes) {
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (StringUtils.isNotEmpty(prefix) && tableName.startsWith(prefix)) {
                    return tableName.substring(prefix.length());
                }
            }
        }
        return tableName;
    }

    private static boolean isJavaIdentifier(String value) {
        if (StringUtils.isBlank(value) || JAVA_RESERVED_WORDS.contains(value)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(value.charAt(0))) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> toStringList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>();
            for (Object item : collection) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        if (value instanceof String text) {
            if (StringUtils.isBlank(text)) {
                return new ArrayList<>();
            }
            return List.of(text.split(","));
        }
        throw new BusinessException("表前缀配置必须是字符串数组");
    }
}
