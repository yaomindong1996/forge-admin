package com.mdframe.forge.starter.plugin.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PluginMigrationPlanTest {

    @ParameterizedTest
    @CsvSource({"example_schema_history,hello,example_plugin_hello_history",
            "acme_schema_history,order-print,acme_plugin_order_print_history",
            "Example_2_schema_history,aa,Example_2_plugin_aa_history"})
    void should_derive_independent_history_when_main_table_has_project_prefix(String main, String id, String history) {
        PluginMigrationPlan plan = PluginMigrationPlan.from(main, id);
        assertThat(plan.historyTable()).isEqualTo(history);
        assertThat(plan.location()).isEqualTo("classpath:db/plugin/" + id);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"history", "_schema_history", "schema.forge_schema_history", "forge-schema_history",
            "1forge_schema_history", "forge_schema_history ", "forge_schema_history;DROP", "中文_schema_history"})
    void should_reject_unsafe_main_identifier(String main) {
        assertThatIllegalArgumentException().isThrownBy(() -> PluginMigrationPlan.from(main, "hello"))
                .withMessageContaining("hello");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"a", "Hello", "bad_id", "../hello", "hello/aa", "hello;DROP"})
    void should_reject_unsafe_plugin_id(String id) {
        assertThatIllegalArgumentException().isThrownBy(() -> PluginMigrationPlan.from("forge_schema_history", id));
    }

    @Test
    void should_accept_exact_mysql_limit_and_reject_overflow() {
        String id = "a".repeat(32);
        assertThat(PluginMigrationPlan.from("p".repeat(16) + "_schema_history", id).historyTable()).hasSize(64);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationPlan.from("p".repeat(17) + "_schema_history", id))
                .withMessageContaining("64");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PluginMigrationPlan.from("p".repeat(64) + "_schema_history", "hello"));
    }

    @Test
    void should_keep_neighbor_plugin_ids_distinct() {
        assertThat(PluginMigrationPlan.from("forge_schema_history", "order-print").historyTable())
                .isNotEqualTo(PluginMigrationPlan.from("forge_schema_history", "orderprint").historyTable());
    }
}
