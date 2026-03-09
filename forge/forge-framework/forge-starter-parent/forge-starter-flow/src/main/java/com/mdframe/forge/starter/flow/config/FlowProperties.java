package com.mdframe.forge.starter.flow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Flowable 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forge.flow")
public class FlowProperties {

    /**
     * 流程引擎名称
     */
    private String processEngineName = "default";

    /**
     * 部署资源位置
     */
    private String[] deploymentResources = new String[]{"classpath*:/processes/**/*.bpmn20.xml"};

    /**
     * 异步执行器
     */
    private boolean asyncExecutorActivate = true;

    /**
     * 数据库表自动更新
     */
    private String databaseSchemaUpdate = "true";

    /**
     * 历史级别 (full, audit, none)
     */
    private String historyLevel = "full";

    /**
     * 任务并行数
     */
    private int asyncExecutorThreadPoolQueueCapacity = 100;

    /**
     * 定时任务线程池大小
     */
    private int asyncExecutorCorePoolSize = 4;
}