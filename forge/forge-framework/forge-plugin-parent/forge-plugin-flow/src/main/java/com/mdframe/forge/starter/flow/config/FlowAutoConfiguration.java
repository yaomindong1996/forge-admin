package com.mdframe.forge.starter.flow.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 流程引擎自动配置
 * flowable-spring-boot-starter 已自动配置 ProcessEngine
 */
@AutoConfiguration
@EnableConfigurationProperties(FlowProperties.class)
public class FlowAutoConfiguration {

    // ProcessEngine、RepositoryService、RuntimeService、TaskService、HistoryService
    // 由 flowable-spring-boot-starter 自动配置
    // 无需手动创建
}