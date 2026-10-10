package com.mdframe.forge.starter.plugin.migration;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/** 在 Boot 的初始化器内完成全部迁移，依赖它的 Bean 不会提前访问插件表。 */
@Slf4j
@RequiredArgsConstructor
public final class PluginFlywayMigrationStrategy implements FlywayMigrationStrategy {

    private final PluginRegistry registry;
    private final PluginFlywayFactory factory;

    @Override
    public void migrate(Flyway flyway) {
        flyway.migrate();
        Configuration main = flyway.getConfiguration();
        // 先完整校验计划，再执行任何插件，避免后面的非法表名造成前面的插件半安装。
        for (PluginMigrationPlan plan : factory.plan(main, registry.getPlugins())) {
            migratePlugin(main, plan);
        }
    }

    private void migratePlugin(Configuration main, PluginMigrationPlan plan) {
        try {
            log.info("执行插件迁移，pluginId={}，historyTable={}", plan.pluginId(), plan.historyTable());
            factory.create(main, plan).migrate();
        } catch (RuntimeException exception) {
            log.error("插件迁移失败，pluginId={}，historyTable={}", plan.pluginId(), plan.historyTable(), exception);
            // MySQL DDL 可能已落库，不能通过自动 clean/repair 掩盖失败或删除客户数据。
            throw new IllegalStateException("插件 " + plan.pluginId() + " 迁移失败，历史表 " + plan.historyTable(), exception);
        }
    }
}
