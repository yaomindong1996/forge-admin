package com.mdframe.forge.starter.plugin.version;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SemVer 值对象：大整数避免溢出，构建元信息不参与优先级比较。 */
public record SemanticVersion(BigInteger major, BigInteger minor, BigInteger patch, List<String> prerelease)
        implements Comparable<SemanticVersion> {

    private static final Pattern VERSION = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?");

    public SemanticVersion {
        prerelease = List.copyOf(prerelease);
    }

    public static SemanticVersion parse(String value) {
        Matcher match = VERSION.matcher(value == null ? "" : value);
        if (!match.matches()) {
            throw new IllegalArgumentException("非法语义版本：" + value);
        }
        List<String> identifiers = match.group(4) == null ? List.of() : List.of(match.group(4).split("\\."));
        for (String identifier : identifiers) {
            if (isNumeric(identifier) && identifier.length() > 1 && identifier.startsWith("0")) {
                throw new IllegalArgumentException("预发布数字标识不得带前导零：" + value);
            }
        }
        return new SemanticVersion(new BigInteger(match.group(1)), new BigInteger(match.group(2)),
                new BigInteger(match.group(3)), identifiers);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int result = major.compareTo(other.major);
        if (result == 0) {
            result = minor.compareTo(other.minor);
        }
        if (result == 0) {
            result = patch.compareTo(other.patch);
        }
        return result != 0 ? result : comparePrerelease(other);
    }

    private int comparePrerelease(SemanticVersion other) {
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return Boolean.compare(prerelease.isEmpty(), other.prerelease.isEmpty());
        }
        int length = Math.min(prerelease.size(), other.prerelease.size());
        for (int index = 0; index < length; index++) {
            int result = compareIdentifier(prerelease.get(index), other.prerelease.get(index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    private static boolean isNumeric(String value) {
        return value.matches("[0-9]+");
    }
}
