package com.mdframe.forge.plugin.external.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowcodeQuerySourceMigrationContractTest {

    private static final String MIGRATION = "V1.0.102__add_external_api_lowcode_query_contract.sql";
    private static final String MOCK_MIGRATION = "V1.0.107__add_external_api_mock_query_sources.sql";

    @Test
    void shouldAddIdempotentDefaultClosedQueryContractColumnsAndIndex() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("information_schema.STATISTICS"));
        assertTrue(sql.contains("`lowcode_query_enabled` tinyint(1) NOT NULL DEFAULT 0"));
        assertTrue(sql.contains("`input_schema_json` text NULL"));
        assertTrue(sql.contains("`output_schema_json` text NULL"));
        assertTrue(sql.contains("idx_external_api_lowcode_query"));
        assertFalse(sql.contains("lowcode_query_enabled` tinyint(1) NOT NULL DEFAULT 1"));
        assertFalse(sql.contains("tenant_id = 0"));
        assertFalse(sql.contains("${"));
    }

    @Test
    void shouldAddMockExecutionModeAndPresaleDefaultSources() throws IOException {
        String sql = Files.readString(resolveMigration(MOCK_MIGRATION));

        assertTrue(sql.contains("`execution_mode` varchar(16) NOT NULL DEFAULT ''HTTP''"));
        assertTrue(sql.contains("`mock_response_json` text NULL"));
        assertTrue(sql.contains("idx_external_api_execution_mode"));
        assertTrue(sql.contains("'企业微信Mock' system_name"));
        assertTrue(sql.contains("'wecom/user-store'"));
        assertTrue(sql.contains("'member/member-by-mobile'"));
        assertTrue(sql.contains("'product/product-by-barcode'"));
        assertTrue(sql.contains("'payment/static-code'"));
        assertTrue(sql.contains("'MOCK'"));
        assertTrue(sql.contains("'mock-local'"));
        assertFalse(sql.contains("http://"));
        assertFalse(sql.contains("https://"));
        assertFalse(sql.contains("tenant_id = 0"));
        assertFalse(sql.contains("${"));
    }

    private Path resolveMigration() {
        return resolveMigration(MIGRATION);
    }

    private Path resolveMigration(String migration) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/mysql/migration").resolve(migration);
            if (Files.exists(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/mysql/migration").resolve(migration);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到迁移脚本: " + migration);
    }
}
