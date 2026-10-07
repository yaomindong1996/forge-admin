package com.mdframe.forge.starter.plugin.migration;

import org.flywaydb.core.Flyway;
import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mdframe.forge.starter.plugin.migration.MigrationFixture.historyTable;
import static com.mdframe.forge.starter.plugin.migration.MigrationFixture.quotedHistory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginFlywayMigrationIntegrationTest {

    @TempDir
    Path temporary;

    @Test
    void should_install_on_nonempty_schema_and_not_repeat_on_restart() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.migrate();
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isEqualTo(1);
            assertThat(fixture.count("SELECT COUNT(*) FROM " + quotedHistory("hello")
                    + " WHERE \"version\" = '0' AND \"type\" = 'BASELINE'")).isEqualTo(1);
            assertThat(fixture.count("SELECT COUNT(*) FROM " + quotedHistory("hello")
                    + " WHERE \"version\" = '1.0.0' AND \"success\" = TRUE")).isEqualTo(1);
            fixture.migrate();
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isEqualTo(1);
            // H2 的 Flyway 实现还写入 TABLE 建表记录；只统计基线与实际迁移，不能将其误判为重复执行。
            assertThat(fixture.count("SELECT COUNT(*) FROM " + quotedHistory("hello")
                    + " WHERE \"type\" <> 'TABLE'")).isEqualTo(2);
        }
    }

    @Test
    void should_sort_plugins_isolate_same_versions_and_ignore_unregistered_resources() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("bravo");
            fixture.addPlugin("alpha");
            fixture.sql("alpha", "V1.0.0__first.sql", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.sql("bravo", "V1.0.0__second.sql", "UPDATE plugin_test_value SET id = 2 WHERE id = 1");
            fixture.sql("unknown", "V1.0.0__ignored.sql", "INSERT INTO missing_table VALUES (1)");
            fixture.sql("alpha", "../unknown/V1.0.0__ignored.sql", "INSERT INTO missing_table VALUES (1)");
            List<PluginDescriptor> plugins = fixture.registry().getPlugins();
            assertThat(new PluginFlywayFactory().plan(fixture.configuration(), List.of(plugins.get(1), plugins.get(0))))
                    .extracting(PluginMigrationPlan::pluginId).containsExactly("alpha", "bravo");
            fixture.migrate();
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value WHERE id = 2")).isEqualTo(1);
            assertThat(fixture.tableExists(historyTable("alpha"))).isTrue();
            assertThat(fixture.tableExists(historyTable("bravo"))).isTrue();
            assertThat(fixture.tableExists(historyTable("unknown"))).isFalse();
            fixture.migrate();
        }
    }

    @Test
    void should_skip_ui_only_plugins_and_preserve_arbitrary_main_history_names() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("ui-only");
            fixture.sql("ui-only", "README.txt", "no SQL migrations");
            Flyway main = fixture.configuration().table("custom_history").load();
            new PluginFlywayMigrationStrategy(fixture.registry(), new PluginFlywayFactory()).migrate(main);
            assertThat(fixture.tableExists("custom_history")).isTrue();
            assertThat(fixture.tableExists(historyTable("ui-only"))).isFalse();
        }
    }

    @Test
    void should_load_nested_migrations_from_jar() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("jar-plugin");
            fixture.sql("jar-plugin", "nested/V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (10)");
            fixture.packagePluginAsJar("jar-plugin");
            fixture.migrate();
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value WHERE id = 10")).isEqualTo(1);
            fixture.migrate();
        }
    }

    @Test
    void should_stop_later_plugins_and_keep_successful_history_after_failure() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("alpha");
            fixture.addPlugin("bravo");
            fixture.addPlugin("charlie");
            fixture.sql("alpha", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.sql("bravo", "V1.0.0__fail.sql", "INSERT INTO missing_table VALUES (1)");
            fixture.sql("charlie", "V1.0.0__never.sql", "INSERT INTO plugin_test_value VALUES (3)");
            assertThatIllegalStateException().isThrownBy(fixture::migrate).withMessageContaining("bravo");
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isEqualTo(1);
            assertThat(fixture.tableExists(historyTable("alpha"))).isTrue();
            assertThat(fixture.tableExists(historyTable("charlie"))).isFalse();
        }
    }

    @Test
    void should_validate_all_history_names_before_first_plugin() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("alpha");
            String id = "z".repeat(32);
            fixture.addPlugin(id);
            fixture.sql("alpha", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.sql(id, "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (2)");
            Flyway main = fixture.configuration().table("p".repeat(17) + "_schema_history").load();
            assertThatIllegalArgumentException().isThrownBy(() ->
                    new PluginFlywayMigrationStrategy(fixture.registry(), new PluginFlywayFactory()).migrate(main));
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isZero();
            assertThat(fixture.tableExists("p".repeat(17) + "_plugin_alpha_history")).isFalse();
        }
    }

    @Test
    void should_honor_finite_target_and_custom_sql_suffix() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__first.ddl", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.sql("hello", "V1.0.1__target.ddl", "SELECT 1");
            fixture.sql("hello", "V2.0.0__future.ddl", "INSERT INTO plugin_test_value VALUES (2)");
            fixture.sql("hello", "V1.0.0__ignored.sql", "INSERT INTO missing_table VALUES (1)");
            FluentConfiguration main = fixture.configuration().sqlMigrationSuffixes(".sql", ".ddl").target("1.0.1");
            // 主流需要 .sql，此用例先迁移主库，再将插件沿用的 SQL 后缀收敛为 .ddl。
            main.load().migrate();
            main.sqlMigrationSuffixes(".ddl");
            PluginFlywayFactory factory = new PluginFlywayFactory();
            for (PluginMigrationPlan plan : factory.plan(main, fixture.registry().getPlugins())) {
                factory.create(main, plan).migrate();
            }
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isEqualTo(1);
            assertThat(fixture.count("SELECT COUNT(*) FROM " + quotedHistory("hello")
                    + " WHERE \"version\" = '1.0.1' AND \"success\" = TRUE")).isEqualTo(1);
        }
    }

    @Test
    void should_fail_if_inherited_finite_target_is_missing_in_plugin() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            Flyway main = fixture.configuration().target("1.0.1").load();
            assertThatIllegalStateException().isThrownBy(() ->
                    new PluginFlywayMigrationStrategy(fixture.registry(), new PluginFlywayFactory()).migrate(main))
                    .withMessageContaining("hello").withStackTraceContaining("target version 1.0.1");
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isZero();
        }
    }

    @Test
    void should_not_force_baseline_when_customer_disables_it() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            Flyway main = fixture.configuration().baselineOnMigrate(false).load();
            assertThatIllegalStateException().isThrownBy(() ->
                    new PluginFlywayMigrationStrategy(fixture.registry(), new PluginFlywayFactory()).migrate(main))
                    .withMessageContaining("hello").withStackTraceContaining("non-empty schema");
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isZero();
        }
    }

    @Test
    void should_not_execute_main_programmatic_migration_again_in_plugin_history() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            V1_0_2__Count migration = new V1_0_2__Count();
            Flyway main = fixture.configuration().javaMigrations(migration).load();
            new PluginFlywayMigrationStrategy(fixture.registry(), new PluginFlywayFactory()).migrate(main);
            assertThat(migration.count).hasValue(1);
            assertThat(fixture.count("SELECT COUNT(*) FROM " + quotedHistory("hello")
                    + " WHERE \"version\" = '1.0.2'")).isZero();
        }
    }

    @Test
    void should_reject_changed_plugin_checksum_on_next_start() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            fixture.addPlugin("hello");
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
            fixture.migrate();
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (2)");
            assertThatThrownBy(fixture::migrate).hasMessageContaining("hello")
                    .hasStackTraceContaining("checksum mismatch");
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isEqualTo(1);
        }
    }

    static class V1_0_2__Count extends BaseJavaMigration {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public void migrate(Context context) {
            count.incrementAndGet();
        }
    }
}
