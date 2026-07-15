package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication Phase 1 backfill contract")
class BusinessApplicationBackfillContractTest {

    private static final String MIGRATION = "V1.0.27__add_business_application_aggregate.sql";

    @Test
    @DisplayName("migration creates aggregate tables and nullable entry ownership")
    void migrationCreatesAggregateAndEntryOwnership() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS ai_business_application ("));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS ai_business_application_object ("));
        assertTrue(sql.contains("ADD COLUMN application_id bigint DEFAULT NULL"));
        assertTrue(sql.contains("idx_ai_business_app_application"));
        assertTrue(sql.contains("logic_delete_active"));
    }

    @Test
    @DisplayName("backfill uses tenant-scoped deterministic application codes and a legacy fallback")
    void backfillUsesDeterministicCodesAndLegacyFallback() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CONCAT('migrated_', LEFT(MD5(CONCAT(bo.tenant_id, ':', bo.suite_code, ':', bo.object_code)), 24))"));
        assertTrue(sql.contains("HAVING COUNT(DISTINCT association.application_id) = 1"));
        assertTrue(sql.contains("CONCAT('legacy_', LEFT(MD5(CONCAT(pending.tenant_id, ':', pending.suite_code)), 24))"));
        assertTrue(sql.contains("AND application_id IS NULL"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    @Test
    @DisplayName("duplicate relation candidates collapse before the unique association insert")
    void duplicateRelationCandidatesCollapseBeforeInsert() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CASE MIN(candidate.role_priority)"));
        assertTrue(sql.contains("GROUP BY candidate.tenant_id, candidate.application_id, candidate.object_id"));
        assertTrue(sql.contains("existing.object_id = candidate.object_id"));
    }

    @Test
    @DisplayName("migration does not interpolate Flyway placeholders or seed tenant zero")
    void migrationHasNoUnsafePlaceholderOrTenantZeroSeed() throws IOException {
        String sql = migrationSql();
        Pattern tenantZero = Pattern.compile("(?i)tenant_id\\s*(?:=|,|\\))\\s*0\\b");

        assertFalse(sql.contains("${"));
        assertFalse(tenantZero.matcher(sql).find());
        assertFalse(Pattern.compile("(?i)DELETE\\s+FROM\\s+ai_business_(?:application|application_object|app)")
                .matcher(sql).find());
    }

    @Test
    @DisplayName("READY backfill only changes untouched migrated applications")
    void readyBackfillOnlyChangesMigratedDrafts() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("application_row.design_status = 'DRAFT'"));
        assertTrue(sql.contains("JSON_UNQUOTE(JSON_EXTRACT(application_row.options, '$.migrationSource')) = 'BUSINESS_OBJECT'"));
        assertTrue(sql.contains("association.object_role = 'PRIMARY'"));
        assertTrue(sql.contains("entry_row.status = 1"));
    }

    private String migrationSql() throws IOException {
        Path migration = locateMigration();
        assertTrue(Files.isRegularFile(migration), "找不到 Phase 1 Flyway: " + migration);
        return Files.readString(migration);
    }

    private Path locateMigration() {
        String reactorRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (reactorRoot != null) {
            Path candidate = Path.of(reactorRoot).resolve("db/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/migration").resolve(MIGRATION);
    }
}
