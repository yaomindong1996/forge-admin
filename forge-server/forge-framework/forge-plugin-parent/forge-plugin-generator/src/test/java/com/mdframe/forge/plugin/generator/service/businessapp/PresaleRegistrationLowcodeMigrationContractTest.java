package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Presale registration low-code migration contract")
class PresaleRegistrationLowcodeMigrationContractTest {

    private static final String MIGRATION = "V1.0.105__seed_presale_registration_lowcode_app.sql";
    private static final String FIX_MIGRATION = "V1.0.106__fix_presale_lowcode_runtime_schema.sql";
    private static final String MOBILE_VISIBILITY_FIX_MIGRATION = "V1.0.108__fix_presale_mobile_form_visibility.sql";
    private static final String H5_PAGE_SECTIONS_MIGRATION = "V1.0.109__add_presale_h5_page_sections.sql";

    @Test
    @DisplayName("seeds runtime tables, models, objects and master-detail relations")
    void seedsRuntimeDataModel() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ps_presale_order`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ps_presale_order_item`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ps_presale_operation_log`"));
        assertTrue(sql.contains("INSERT INTO ai_lowcode_model"));
        assertTrue(sql.contains("'children', JSON_ARRAY('ps_presale_order_item', 'ps_presale_operation_log')) model_schema"));
        assertTrue(sql.contains("INSERT INTO ai_business_object"));
        assertTrue(sql.contains("'预售登记主单，包含会员、收款和商品明细' description"));
        assertTrue(sql.contains("'presale_items'"));
        assertTrue(sql.contains("'operation_logs'"));
        assertTrue(sql.contains("'master-detail-crud'"));
    }

    @Test
    @DisplayName("seeds H5 scanning, conditional fields and governed query events")
    void seedsMobileFormRuntimeProtocol() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("'barcodeScanner'"));
        assertTrue(sql.contains("'runtimeRules'"));
        assertTrue(sql.contains("'fieldEvents'"));
        assertTrue(sql.contains("'SCAN_COMPLETE'"));
        assertTrue(sql.contains("'CURRENT_CHILDREN'"));
        assertTrue(sql.contains("`cash_amount` bigint"));
        assertTrue(sql.contains("'businessFieldType', 'MONEY', 'precision', 2"));
        assertTrue(sql.contains("'wecom/user-store'"));
        assertTrue(sql.contains("'member/member-by-mobile'"));
        assertTrue(sql.contains("'product/product-by-barcode'"));
        assertTrue(sql.contains("'payment/static-code'"));
    }

    @Test
    @DisplayName("seeds immutable command actions and their published relation snapshot")
    void seedsTransactionalActions() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("'submit_presale'"));
        assertTrue(sql.contains("'record_pickup'"));
        assertTrue(sql.contains("'record_return'"));
        assertTrue(sql.contains("'LOCAL_TRANSACTION'"));
        assertTrue(sql.contains("'TRANSITION_STATUS'"));
        assertTrue(sql.contains("'ASSERT_RECORD'"));
        assertTrue(sql.contains("'ADJUST_NUMBER'"));
        assertTrue(sql.contains("'numericConstraints'"));
        assertTrue(sql.contains("INSERT INTO ai_business_object_design_version"));
        assertTrue(sql.contains("'relationConfig', JSON_OBJECT('relationKey', 'presale_items'"));
    }

    @Test
    @DisplayName("seeds the document rule, application aggregate, runtime entry and disabled integrations")
    void seedsApplicationDeliveryMetadata() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("INSERT INTO ai_business_document_config"));
        assertTrue(sql.contains("'PS-{yyyyMMdd}-{seq4}'"));
        assertTrue(sql.contains("'documentNoField', 'presaleNo'"));
        assertTrue(sql.contains("INSERT INTO ai_business_application ("));
        assertTrue(sql.contains("INSERT INTO ai_business_application_object ("));
        assertTrue(sql.contains("INSERT INTO ai_business_app ("));
        assertTrue(sql.contains("'PRESALE_REGISTRATION_RUNTIME'"));
        assertTrue(sql.contains("'INTEGRATION', seed.source_key"));
        assertTrue(sql.contains("JSON_OBJECT('sourceKey', seed.source_key)"));
        assertTrue(sql.contains("0, seed.sort_order"));
    }

    @Test
    @DisplayName("keeps the migration free of secrets, arbitrary executable configuration and unsafe seeds")
    void keepsSeedConfigurationGoverned() throws IOException {
        String sql = migrationSql();
        String lower = sql.toLowerCase();

        assertFalse(lower.contains("http://"));
        assertFalse(lower.contains("https://"));
        assertFalse(lower.contains("'authorization'"));
        assertFalse(lower.contains("'headers'"));
        assertFalse(lower.contains("'credential'"));
        assertFalse(lower.contains("'password'"));
        assertFalse(lower.contains("'script'"));
        assertFalse(lower.contains("'sql'"));
        assertFalse(sql.contains("${"));
        assertFalse(lower.contains("logic_delete_active"));
        assertFalse(sql.matches("(?s).*tenant_id\\s*=\\s*0.*"));
        assertFalse(sql.matches("(?s).*SELECT\\s+0\\s+tenant_id.*"));
    }

    @Test
    @DisplayName("provides a checksum-safe repair migration for invalid model protocols")
    void repairsPublishedModelSnapshotsWithoutEditingTheSeed() throws IOException {
        String sql = repairMigrationSql();
        assertTrue(sql.contains("V1.0.106__fix_presale_lowcode_runtime_schema.sql") || sql.contains("修复预售登记"));
        assertTrue(sql.contains("UPDATE ai_lowcode_domain"));
        assertTrue(sql.contains("default_app_type = 'MASTER_DETAIL'"));
        assertTrue(sql.contains("'$.appType', 'MASTER_DETAIL'"));
        assertTrue(sql.contains("'$.appType', 'SINGLE'"));
        assertTrue(sql.contains("'$.children', JSON_ARRAY()"));
        assertTrue(sql.contains("'$.fields[2].systemField', false"));
        assertTrue(sql.contains("'$.fields[2].formVisible', false"));
        assertTrue(sql.contains("UPDATE ai_crud_config"));
        assertTrue(sql.contains("UPDATE ai_business_object_design_version"));
        assertTrue(sql.contains("UPDATE ai_crud_config_version"));
        assertTrue(sql.contains("object_code IN ('PS_PRESALE_ORDER_ITEM', 'PS_PRESALE_OPERATION_LOG')"));
        assertTrue(sql.contains("config_row.config_key IN ('ps_presale_order_item', 'ps_presale_operation_log')"));
        assertFalse(sql.toLowerCase().contains("tenant_id = 0"));
    }

    @Test
    @DisplayName("repairs mobile form visibility and designer field-driven hints")
    void repairsMobileFormVisibilityAndDesignerHints() throws IOException {
        String sql = mobileVisibilityRepairMigrationSql();

        assertTrue(sql.contains("V1.0.108__fix_presale_mobile_form_visibility.sql")
                || sql.contains("修复预售登记移动端表单展示"));
        assertTrue(sql.contains("'fieldCode', 'salesUserId'"));
        assertTrue(sql.contains("'visibility', JSON_OBJECT('hidden', true, 'readonly', true)"));
        assertTrue(sql.contains("'fieldCode', 'status'"));
        assertTrue(sql.contains("'defaultValue', 'STATIC_CODE'"));
        assertTrue(sql.contains("'__events'"));
        assertTrue(sql.contains("'targetId', 'cmp_static_payment_no'"));
        assertTrue(sql.contains("'targetId', 'cmp_cash_amount'"));
        assertTrue(sql.contains("'fieldCode', 'cashAmount'"));
        assertTrue(sql.contains("'visibility', JSON_OBJECT('hidden', false, 'readonly', false)"));
        assertTrue(sql.contains("'$.fields[3].formVisible', false"));
        assertTrue(sql.contains("'$.fields[12].formVisible', true"));
        assertTrue(sql.contains("'$.fields[13].formVisible', true"));
        assertTrue(sql.contains("'$.fields[14].formVisible', true"));
        assertTrue(sql.contains("'$.fields[16].formVisible', false"));
        assertFalse(sql.contains("'$.fields[14].formVisible', false"));
        assertTrue(sql.contains("'$.formDesignerSchema'"));
        assertTrue(sql.contains("UPDATE ai_crud_config"));
        assertTrue(sql.contains("UPDATE ai_business_object"));
        assertTrue(sql.contains("UPDATE ai_business_object_design_version"));
        assertTrue(sql.contains("UPDATE ai_crud_config_version"));
        assertTrue(sql.contains("'$.masterDetailConfig.children[1].relationName', '操作日志'"));
        assertFalse(sql.contains("${"));
        assertFalse(sql.toLowerCase().contains("tenant_id = 0"));
    }

    @Test
    @DisplayName("adds H5 page sections, pill payment and action bottom bar to all published snapshots")
    void addsH5PageSectionsToPublishedRuntimeSnapshots() throws IOException {
        String sql = h5PageSectionsMigrationSql();

        assertTrue(sql.contains("'sectionId', 'guide_info'"));
        assertTrue(sql.contains("'sectionId', 'presale_items'"));
        assertTrue(sql.contains("'displayMode', 'inline_grid'"));
        assertTrue(sql.contains("'sectionId', 'operation_logs'"));
        assertTrue(sql.contains("'displayMode', 'bottom_sheet'"));
        assertTrue(sql.contains("'componentKey', 'pillSelect'"));
        assertTrue(sql.contains("'actionCode', 'submit_presale'"));
        assertTrue(sql.contains("'displayCondition', 'status == DRAFT'"));
        assertTrue(sql.contains("'$.formDesignerSchema.pageSections'"));
        assertTrue(sql.contains("'$.zones[2].props.formDesignerSchema.pageSections'"));
        assertTrue(sql.contains("UPDATE ai_crud_config"));
        assertTrue(sql.contains("UPDATE ai_business_object"));
        assertTrue(sql.contains("UPDATE ai_business_object_design_version"));
        assertTrue(sql.contains("UPDATE ai_crud_config_version"));
        assertFalse(sql.contains("${"));
        assertFalse(sql.toLowerCase().contains("tenant_id = 0"));
    }

    private String migrationSql() throws IOException {
        Path migration = locateMigration();
        assertTrue(Files.isRegularFile(migration), "找不到预售登记 Flyway: " + migration);
        return Files.readString(migration, StandardCharsets.UTF_8);
    }

    private Path locateMigration() {
        String reactorRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (reactorRoot != null) {
            Path candidate = Path.of(reactorRoot).resolve("db/mysql/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/mysql/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            candidate = current.resolve("forge-server/db/mysql/migration").resolve(MIGRATION);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("db/mysql/migration").resolve(MIGRATION);
    }

    private String repairMigrationSql() throws IOException {
        Path migration = locateMigration().resolveSibling(FIX_MIGRATION);
        assertTrue(Files.isRegularFile(migration), "找不到预售登记修复 Flyway: " + migration);
        return Files.readString(migration, StandardCharsets.UTF_8);
    }

    private String mobileVisibilityRepairMigrationSql() throws IOException {
        Path migration = locateMigration().resolveSibling(MOBILE_VISIBILITY_FIX_MIGRATION);
        assertTrue(Files.isRegularFile(migration), "找不到预售登记移动端修复 Flyway: " + migration);
        return Files.readString(migration, StandardCharsets.UTF_8);
    }

    private String h5PageSectionsMigrationSql() throws IOException {
        Path migration = locateMigration().resolveSibling(H5_PAGE_SECTIONS_MIGRATION);
        assertTrue(Files.isRegularFile(migration), "找不到预售登记 H5 分区 Flyway: " + migration);
        return Files.readString(migration, StandardCharsets.UTF_8);
    }
}
