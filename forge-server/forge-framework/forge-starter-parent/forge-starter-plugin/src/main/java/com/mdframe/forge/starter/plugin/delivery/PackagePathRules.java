package com.mdframe.forge.starter.plugin.delivery;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

final class PackagePathRules {
    private static final Set<String> EXCLUDED = Set.of(".git", "node_modules", "target", "logs", ".DS_Store",
            ".flattened-pom.xml", ".forge-plugin-owned.json");

    private PackagePathRules() {
    }

    static String validate(String name) {
        require(!name.isEmpty() && name.length() <= 1024 && !name.startsWith("/"), "ZIP 路径非法");
        require(!name.matches("(?s).*[\\\\:\\x00-\\x1f\\x7f].*"), "ZIP 路径含非法字符");
        String[] parts = name.split("/", -1);
        require(parts.length <= 32, "ZIP 目录层级超过限制");
        for (String part : parts) {
            require(!part.isEmpty() && !part.equals(".") && !part.equals("..") && !part.endsWith(".")
                    && !part.endsWith(" "), "ZIP 含越界或非法路径段");
            require(!part.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?"), "ZIP 含保留路径段");
            String normalized = part.toLowerCase(Locale.ROOT);
            require(EXCLUDED.stream().noneMatch(value -> value.toLowerCase(Locale.ROOT).equals(normalized))
                    && !normalized.startsWith(".env") && !normalized.equals("application-dev.yml"),
                    "ZIP 含本地配置、保留文件或构建产物，请清理后上传");
        }
        return name;
    }

    static String normalized(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
