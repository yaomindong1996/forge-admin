package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;

/** 发布预检与实际执行共用的在线 DDL 白名单。 */
final class LowcodeOnlineDdlPolicy {

    private LowcodeOnlineDdlPolicy() {
    }

    static boolean containsUnsafeOnlineDdl(List<String> statements) {
        if (statements == null) {
            return false;
        }
        return statements.stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(statement -> !isSafeOnlineDdl(statement));
    }

    static void assertSafeDdl(String ddl) {
        if (isSafeOnlineDdl(ddl)) {
            return;
        }
        throw new BusinessException("数据库差异包含高风险或非追加式 DDL（如改类型族、缩短长度），"
                + "仅允许预览和导出脚本后人工审核执行");
    }

    static boolean isSafeOnlineDdl(String ddl) {
        if (StringUtils.isBlank(ddl)) {
            return false;
        }
        String normalized = ddl.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("CREATE TABLE IF NOT EXISTS") || normalized.startsWith("CREATE TABLE ")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE") && normalized.contains(" ADD COLUMN ")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE") && normalized.contains(" ADD (")) {
            return true;
        }
        if (normalized.startsWith("ALTER TABLE")
                && (normalized.contains(" ADD KEY ") || normalized.contains(" ADD UNIQUE KEY "))) {
            return true;
        }
        if (normalized.startsWith("CREATE INDEX ") || normalized.startsWith("CREATE UNIQUE INDEX ")) {
            return true;
        }
        return normalized.startsWith("COMMENT ON TABLE ") || normalized.startsWith("COMMENT ON COLUMN ");
    }
}
