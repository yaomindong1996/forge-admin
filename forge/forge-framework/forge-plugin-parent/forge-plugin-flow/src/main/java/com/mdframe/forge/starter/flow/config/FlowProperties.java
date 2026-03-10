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
     * 是否启用流程模块
     */
    private boolean enabled = true;

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
     * 数据库表自动更新策略
     * true: 自动创建/更新表
     * false: 不自动创建
     * create-drop: 启动时创建，关闭时删除
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

    /**
     * 流程图字体
     */
    private String activityFontName = "宋体";
    private String annotationFontName = "宋体";
    private String labelFontName = "宋体";

    /**
     * 邮件配置
     */
    private MailProperties mail = new MailProperties();

    /**
     * 任务超时天数（默认）
     */
    private int defaultTaskDueDays = 3;

    /**
     * 流程实例超时小时数（默认）
     */
    private int defaultProcessTimeoutHours = 24 * 7; // 7天

    @Data
    public static class MailProperties {
        private boolean enabled = false;
        private String host = "smtp.example.com";
        private String port = "25";
        private String username = "";
        private String password = "";
        private String from = "noreply@example.com";
    }
}