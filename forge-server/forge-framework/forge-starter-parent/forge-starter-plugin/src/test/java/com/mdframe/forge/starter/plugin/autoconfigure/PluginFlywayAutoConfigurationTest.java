package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.migration.MigrationFixture;
import com.mdframe.forge.starter.plugin.migration.PluginFlywayMigrationStrategy;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PluginFlywayAutoConfigurationTest {

    @TempDir
    Path temporary;

    private final ApplicationContextRunner core = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(PluginAutoConfiguration.class, PluginFlywayAutoConfiguration.class));

    @Test
    void should_register_strategy_before_boot_initializer_and_wait_for_plugin_tables() throws Exception {
        try (MigrationFixture fixture = plugin()) {
            runner(fixture).withUserConfiguration(AfterMigrationConfiguration.class).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(FlywayMigrationStrategy.class)
                        .hasSingleBean(FlywayMigrationInitializer.class);
                assertThat(context.getBean(FlywayMigrationStrategy.class))
                        .isInstanceOf(PluginFlywayMigrationStrategy.class);
                assertThat(context.getBean(AfterMigration.class).rows()).isEqualTo(1);
                assertThat(context.getBean(Flyway.class).getConfiguration().getTable())
                        .isEqualTo("forge_schema_history");
            });
        }
    }

    @Test
    void should_back_off_when_flyway_is_disabled() {
        core.withPropertyValues("spring.flyway.enabled=false").run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean(FlywayMigrationStrategy.class));
    }

    @Test
    void should_back_off_when_flyway_is_absent() {
        core.withClassLoader(new FilteredClassLoader("org.flywaydb")).run(context ->
                assertThat(context).hasNotFailed().doesNotHaveBean("pluginFlywayMigrationStrategy"));
    }

    @Test
    void should_use_customer_strategy_in_real_boot_initializer() throws Exception {
        try (MigrationFixture fixture = plugin()) {
            AtomicInteger invocations = new AtomicInteger();
            FlywayMigrationStrategy custom = main -> {
                invocations.incrementAndGet();
                main.migrate();
            };
            runner(fixture).withBean(FlywayMigrationStrategy.class, () -> custom).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(FlywayMigrationStrategy.class);
                assertThat(context.getBean(FlywayMigrationStrategy.class)).isSameAs(custom);
                assertThat(invocations).hasValue(1);
                assertThat(fixture.tableExists(MigrationFixture.historyTable("hello"))).isFalse();
            });
        }
    }

    @Test
    void should_back_off_for_customer_auto_configuration_ordered_before_default() {
        core.withConfiguration(AutoConfigurations.of(CustomerMigrationConfiguration.class)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(FlywayMigrationStrategy.class);
            assertThat(context).doesNotHaveBean(PluginFlywayMigrationStrategy.class);
            assertThat(context.getBean(FlywayMigrationStrategy.class)).isSameAs(context.getBean("customerMigration"));
        });
    }

    @Test
    void should_fail_boot_startup_and_not_create_dependent_bean_when_plugin_fails() throws Exception {
        try (MigrationFixture fixture = plugin()) {
            fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO missing_table VALUES (1)");
            runner(fixture).withUserConfiguration(AfterMigrationConfiguration.class).run(context ->
                    assertThat(context).hasFailed().getFailure().hasStackTraceContaining("插件 hello 迁移失败"));
            assertThat(fixture.count("SELECT COUNT(*) FROM plugin_test_value")).isZero();
        }
    }

    @Test
    void should_preserve_boot_main_migration_when_no_plugins_are_installed() throws Exception {
        try (MigrationFixture fixture = new MigrationFixture(temporary)) {
            runner(fixture).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(FlywayMigrationStrategy.class);
                assertThat(fixture.tableExists("forge_schema_history")).isTrue();
                assertThat(fixture.tableExists("PLUGIN_TEST_VALUE")).isTrue();
            });
        }
    }

    private MigrationFixture plugin() throws Exception {
        MigrationFixture fixture = new MigrationFixture(temporary);
        fixture.addPlugin("hello");
        fixture.sql("hello", "V1.0.0__insert.sql", "INSERT INTO plugin_test_value VALUES (1)");
        return fixture;
    }

    private ApplicationContextRunner runner(MigrationFixture fixture) {
        return core.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
                .withClassLoader(fixture.loader()).withBean(DataSource.class, fixture::dataSource)
                .withPropertyValues("spring.flyway.locations=classpath:db/t2-main",
                        "spring.flyway.table=forge_schema_history", "spring.flyway.baseline-on-migrate=true",
                        "spring.flyway.baseline-version=1.0.0", "spring.flyway.placeholder-replacement=false");
    }

    record AfterMigration(int rows) {
    }

    @Configuration(proxyBeanMethods = false)
    static class AfterMigrationConfiguration {
        @Bean
        @DependsOn("flywayInitializer")
        AfterMigration afterMigration(DataSource dataSource) throws SQLException {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM plugin_test_value")) {
                result.next();
                return new AfterMigration(result.getInt(1));
            }
        }
    }

    @AutoConfiguration(before = PluginFlywayAutoConfiguration.class)
    static class CustomerMigrationConfiguration {
        @Bean
        FlywayMigrationStrategy customerMigration() {
            return Flyway::migrate;
        }
    }
}
