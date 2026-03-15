package com.mdframe.forge.starter.flow.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Flowable 流程引擎自动配置
 *
 * <p>Flowable 核心组件由 flowable-spring-boot-starter 自动配置，包括：</p>
 * <ul>
 *     <li>ProcessEngine - 流程引擎</li>
 *     <li>RepositoryService - 资源管理服务</li>
 *     <li/RuntimeService - 运行时服务</li>
 *     <li>TaskService - 任务服务</li>
 *     <li>HistoryService - 历史服务</li>
 *     <li>ManagementService - 管理服务</li>
 * </ul>
 *
 * <p>配置通过 application.yml 中的 flowable.* 前缀进行，例如：</p>
 * <pre>
 * flowable:
 *   database-schema-update: true
 *   async-executor-activate: true
 *   history-level: full
 * </pre>
 *
 * @see org.flowable.spring.boot.FlowableAutoConfiguration
 */
@AutoConfiguration
public class FlowAutoConfiguration {

    // Flowable 核心组件由 flowable-spring-boot-starter 自动配置
    // 无需手动创建 Bean
    
}
