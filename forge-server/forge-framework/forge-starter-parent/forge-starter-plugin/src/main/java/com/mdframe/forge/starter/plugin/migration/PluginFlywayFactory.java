package com.mdframe.forge.starter.plugin.migration;

import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 在相同数据源上使用独立迁移来源；不缓存状态，由 Flyway 锁与历史表保证多实例幂等。 */
@Slf4j
public final class PluginFlywayFactory {

    public List<PluginMigrationPlan> plan(Configuration main, List<PluginDescriptor> plugins) {
        List<PluginMigrationPlan> plans = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(main.getClassLoader());
        for (PluginDescriptor plugin : plugins) {
            if (hasSqlMigration(resolver, main, plugin.id())) {
                plans.add(PluginMigrationPlan.from(main.getTable(), plugin.id()));
            }
        }
        plans.sort(Comparator.comparing(PluginMigrationPlan::pluginId));
        return List.copyOf(plans);
    }

    public Flyway create(Configuration main, PluginMigrationPlan plan) {
        return configuration(main, plan).load();
    }

    FluentConfiguration configuration(Configuration main, PluginMigrationPlan plan) {
        // 配置复制会带入主 Java migrations/provider；仅换 locations 不足以隔离这些显式来源。
        return Flyway.configure(main.getClassLoader()).configuration(main)
                .dataSource(main.getDataSource())
                .locations(plan.location())
                .table(plan.historyTable())
                .baselineVersion("0")
                .javaMigrations()
                .javaMigrationClassProvider(List::of)
                .resolvers(new MigrationResolver[0])
                .resourceProvider(null)
                .skipDefaultResolvers(false);
    }

    private boolean hasSqlMigration(PathMatchingResourcePatternResolver resolver, Configuration main, String id) {
        try {
            for (Resource resource : resolver.getResources("classpath*:db/plugin/" + id + "/**/*")) {
                String name = resource.getFilename();
                if (name != null && Arrays.stream(main.getSqlMigrationSuffixes()).anyMatch(name::endsWith)) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            log.error("扫描插件迁移失败，pluginId={}", id, exception);
            throw new IllegalStateException("扫描插件迁移失败，插件 " + id, exception);
        }
    }
}
