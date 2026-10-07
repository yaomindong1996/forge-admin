package com.mdframe.forge.starter.plugin.migration;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PluginFlywayMigrationStrategyTest {

    private final PluginRegistry registry = mock(PluginRegistry.class);
    private final PluginFlywayFactory factory = mock(PluginFlywayFactory.class);
    private final Flyway main = mock(Flyway.class);
    private final Configuration configuration = mock(Configuration.class);
    private final PluginFlywayMigrationStrategy strategy = new PluginFlywayMigrationStrategy(registry, factory);

    @Test
    void should_complete_main_before_plugins() {
        Flyway first = mock(Flyway.class);
        Flyway second = mock(Flyway.class);
        PluginMigrationPlan alpha = PluginMigrationPlan.from("example_schema_history", "alpha");
        PluginMigrationPlan bravo = PluginMigrationPlan.from("example_schema_history", "bravo");
        when(main.getConfiguration()).thenReturn(configuration);
        when(factory.plan(configuration, List.of())).thenReturn(List.of(alpha, bravo));
        when(factory.create(configuration, alpha)).thenReturn(first);
        when(factory.create(configuration, bravo)).thenReturn(second);
        strategy.migrate(main);
        InOrder order = inOrder(main, factory, first, second);
        order.verify(main).migrate();
        order.verify(main).getConfiguration();
        order.verify(factory).plan(configuration, List.of());
        order.verify(factory).create(configuration, alpha);
        order.verify(first).migrate();
        order.verify(factory).create(configuration, bravo);
        order.verify(second).migrate();
    }

    @Test
    void should_not_scan_or_execute_plugins_when_main_fails() {
        RuntimeException failure = new IllegalStateException("main failure");
        when(main.migrate()).thenThrow(failure);
        assertThatThrownBy(() -> strategy.migrate(main)).isSameAs(failure);
        verifyNoInteractions(registry, factory);
    }

    @Test
    void should_stop_after_plugin_failure_with_context_and_cause() {
        PluginMigrationPlan alpha = PluginMigrationPlan.from("example_schema_history", "alpha");
        PluginMigrationPlan bravo = PluginMigrationPlan.from("example_schema_history", "bravo");
        RuntimeException failure = new IllegalStateException("plugin failure");
        Flyway first = mock(Flyway.class);
        when(main.getConfiguration()).thenReturn(configuration);
        when(factory.plan(configuration, List.of())).thenReturn(List.of(alpha, bravo));
        when(factory.create(configuration, alpha)).thenReturn(first);
        when(first.migrate()).thenThrow(failure);
        assertThatIllegalStateException().isThrownBy(() -> strategy.migrate(main))
                .withMessageContaining("alpha").withMessageContaining("example_plugin_alpha_history")
                .withCause(failure);
        verify(factory, never()).create(configuration, bravo);
        verify(main, never()).clean();
        verify(main, never()).repair();
    }

    @Test
    void should_only_migrate_main_when_no_plugin_migrations_exist() {
        when(main.getConfiguration()).thenReturn(configuration);
        when(factory.plan(configuration, List.of())).thenReturn(List.of());
        strategy.migrate(main);
        verify(main).migrate();
        verify(factory).plan(configuration, List.of());
    }
}
