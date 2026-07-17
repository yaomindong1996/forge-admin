package com.mdframe.forge.plugin.generator.manager.coderule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRuleMigrationContractTest {

    private static final String MIGRATION = "V1.0.36__add_structured_code_rule_segments.sql";
    private static final String RUNTIME_MIGRATION = "V1.0.37__optimize_code_rule_runtime.sql";

    @Test
    void categoryBackfillShouldDependOnDataStateInsteadOfColumnCreationSession() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertFalse(sql.contains("@category_added"));
        assertTrue(sql.contains("category IS NULL"));
        assertTrue(sql.contains("category = ''"));
        assertTrue(sql.contains("category = 'COMMON'"));
    }

    @Test
    void shouldAddObjectBindingAfterRequiredColumnsAndSeedSceneDictionary() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'source_object_id'"));
        assertTrue(sql.contains("COLUMN_NAME = 'source_object_code'"));
        assertTrue(sql.contains("'sys_code_rule_scene'"));
        assertTrue(sql.contains("'ORDER', 'sys_code_rule_scene'"));
        assertTrue(sql.contains("'CONTRACT', 'sys_code_rule_scene'"));
        assertTrue(sql.lastIndexOf("COLUMN_NAME = 'del_flag'")
                < sql.lastIndexOf("CREATE INDEX idx_ai_code_rule_source_object"));
    }

    @Test
    void shouldPersistVariableSourceAndBackfillBoundVariablesAsLowCode() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("`variable_source` varchar(16) NOT NULL DEFAULT 'CUSTOM'"));
        assertTrue(sql.contains("COLUMN_NAME = 'variable_source'"));
        assertTrue(sql.contains("SET segment.variable_source = 'LOWCODE'"));
        assertTrue(sql.contains("segment.segment_type = 'VARIABLE'"));
        assertTrue(sql.contains("rule.source_object_id IS NOT NULL"));
    }

    @Test
    void ruleCodeShouldRemainUniqueAfterLogicalDeletion() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains(
                "CREATE UNIQUE INDEX uk_ai_code_rule_code ON ai_code_rule (tenant_id, rule_code)"));
        assertTrue(sql.contains("ALTER TABLE ai_code_rule DROP INDEX uk_ai_code_rule_code_active"));
    }

    @Test
    void runtimeMigrationShouldPreserveExistingRulesAndOptimizeActiveSegmentIndex() throws IOException {
        String sql = Files.readString(resolveMigration(RUNTIME_MIGRATION));

        assertTrue(sql.contains("COLUMN_NAME = 'legacy_compat_enabled'"));
        assertTrue(sql.contains("legacy_compat_enabled tinyint NOT NULL DEFAULT 1"));
        assertTrue(sql.contains("tenant_id, rule_id, del_flag, segment_order, id"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
    }

    private Path resolveMigration() {
        return resolveMigration(MIGRATION);
    }

    private Path resolveMigration(String migration) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve("db/migration").resolve(migration);
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/migration").resolve(migration);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/migration").resolve(migration);
    }
}
