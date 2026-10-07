package com.mdframe.forge.starter.plugin.migration;

import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class PluginFlywayFactoryTest {

    private final PluginFlywayFactory factory = new PluginFlywayFactory();

    @Test
    void should_copy_shared_configuration_without_mutating_main() {
        DataSource source = mock(DataSource.class);
        Callback callback = mock(Callback.class);
        FluentConfiguration main = Flyway.configure(getClass().getClassLoader()).dataSource(source)
                .locations("classpath:main/migrations").table("acme_schema_history")
                .baselineVersion("1.0.0").baselineOnMigrate(true).encoding(StandardCharsets.UTF_16)
                .defaultSchema("BUSINESS").schemas("BUSINESS", "AUDIT").placeholderReplacement(false)
                .placeholders(Map.of("table", "configured_value")).target("2.0.0")
                .ignoreMigrationPatterns("*:future", "repeatable:missing").outOfOrder(true)
                .validateOnMigrate(false).validateMigrationNaming(true).connectRetries(2)
                .sqlMigrationPrefix("M").sqlMigrationSeparator("___").sqlMigrationSuffixes(".ddl")
                .callbacks(callback).executeInTransaction(false).cleanDisabled(true);
        Configuration plugin = factory.configuration(main, PluginMigrationPlan.from(main.getTable(), "order-print"));
        assertThat(plugin.getDataSource()).isSameAs(source);
        assertThat(plugin.getClassLoader()).isSameAs(main.getClassLoader());
        assertThat(plugin.getDefaultSchema()).isEqualTo("BUSINESS");
        assertThat(plugin.getSchemas()).containsExactly("BUSINESS", "AUDIT");
        assertThat(plugin.getEncoding()).isEqualTo(StandardCharsets.UTF_16);
        assertThat(plugin.isPlaceholderReplacement()).isFalse();
        assertThat(plugin.getPlaceholders()).containsExactlyEntriesOf(main.getPlaceholders());
        assertThat(plugin.getCallbacks()).containsExactly(callback);
        assertThat(plugin.getTarget()).isEqualTo(MigrationVersion.fromVersion("2.0.0"));
        assertThat(plugin.getIgnoreMigrationPatterns()).containsExactly(main.getIgnoreMigrationPatterns());
        assertThat(plugin.isOutOfOrder()).isTrue();
        assertThat(plugin.isValidateOnMigrate()).isFalse();
        assertThat(plugin.isValidateMigrationNaming()).isTrue();
        assertThat(plugin.getConnectRetries()).isEqualTo(2);
        assertThat(plugin.getSqlMigrationPrefix()).isEqualTo("M");
        assertThat(plugin.getSqlMigrationSeparator()).isEqualTo("___");
        assertThat(plugin.getSqlMigrationSuffixes()).containsExactly(".ddl");
        assertThat(plugin.isExecuteInTransaction()).isFalse();
        assertThat(plugin.isCleanDisabled()).isTrue();
        assertThat(plugin.isBaselineOnMigrate()).isTrue();
        assertThat(plugin.getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("0"));
        assertThat(plugin.getTable()).isEqualTo("acme_plugin_order_print_history");
        assertThat(plugin.getLocations()).extracting(Object::toString)
                .containsExactly("classpath:db/plugin/order-print");
        assertThat(main.getTable()).isEqualTo("acme_schema_history");
        assertThat(main.getLocations()).extracting(Object::toString).containsExactly("classpath:main/migrations");
        assertThat(main.getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("1.0.0"));
    }

    @Test
    void should_exclude_explicit_main_migration_sources_but_keep_main_untouched() {
        JavaMigration migration = mock(JavaMigration.class);
        ResourceProvider resources = mock(ResourceProvider.class);
        MigrationResolver resolver = mock(MigrationResolver.class);
        FluentConfiguration main = Flyway.configure().dataSource(mock(DataSource.class))
                .table("forge_schema_history").javaMigrations(migration).resourceProvider(resources)
                .resolvers(resolver).skipDefaultResolvers(true).javaMigrationClassProvider(() -> List.of())
                .baselineOnMigrate(false);
        Configuration plugin = factory.configuration(main, PluginMigrationPlan.from(main.getTable(), "hello"));
        assertThat(plugin.getJavaMigrations()).isEmpty();
        assertThat(plugin.getJavaMigrationClassProvider().getClasses()).isEmpty();
        assertThat(plugin.getResolvers()).isEmpty();
        assertThat(plugin.getResourceProvider()).isNull();
        assertThat(plugin.isSkipDefaultResolvers()).isFalse();
        assertThat(plugin.isBaselineOnMigrate()).isFalse();
        assertThat(main.getJavaMigrations()).containsExactly(migration);
        assertThat(main.getResolvers()).containsExactly(resolver);
        assertThat(main.getResourceProvider()).isSameAs(resources);
        assertThat(main.isSkipDefaultResolvers()).isTrue();
    }

    @Test
    void should_stop_with_plugin_context_when_resource_scan_fails() {
        ClassLoader broken = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                if (name.startsWith("db/plugin/")) {
                    throw new IOException("fixture classpath failure");
                }
                return super.getResources(name);
            }
        };
        FluentConfiguration main = Flyway.configure(broken).table("example_schema_history");
        PluginDescriptor plugin = new PluginDescriptor("hello", "测试", "1.0.0", PluginEdition.COMMUNITY,
                ">=1.2.0", List.of(), null, null);
        assertThatIllegalStateException().isThrownBy(() -> factory.plan(main, List.of(plugin)))
                .withMessageContaining("hello").withCauseInstanceOf(IOException.class);
    }
}
