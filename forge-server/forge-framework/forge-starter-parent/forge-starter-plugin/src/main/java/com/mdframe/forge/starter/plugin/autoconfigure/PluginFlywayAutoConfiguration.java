package com.mdframe.forge.starter.plugin.autoconfigure;

import com.mdframe.forge.starter.plugin.descriptor.PluginRegistry;
import com.mdframe.forge.starter.plugin.migration.PluginFlywayFactory;
import com.mdframe.forge.starter.plugin.migration.PluginFlywayMigrationStrategy;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

/** 必须先登记策略，Boot 创建 flywayInitializer 时才能在同一阶段等待插件迁移。 */
@AutoConfiguration(after = PluginAutoConfiguration.class, before = FlywayAutoConfiguration.class)
@ConditionalOnClass({Flyway.class, FlywayMigrationStrategy.class})
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class PluginFlywayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
    public PluginFlywayMigrationStrategy pluginFlywayMigrationStrategy(PluginRegistry registry) {
        return new PluginFlywayMigrationStrategy(registry, new PluginFlywayFactory());
    }
}
