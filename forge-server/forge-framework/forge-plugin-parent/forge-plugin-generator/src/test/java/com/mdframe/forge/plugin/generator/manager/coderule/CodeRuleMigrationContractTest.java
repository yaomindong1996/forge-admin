package com.mdframe.forge.plugin.generator.manager.coderule;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRuleMigrationContractTest {

    private static final String MIGRATION = "V1.0.36__add_structured_code_rule_segments.sql";

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

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve("db/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/migration").resolve(MIGRATION);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/migration").resolve(MIGRATION);
    }
}
