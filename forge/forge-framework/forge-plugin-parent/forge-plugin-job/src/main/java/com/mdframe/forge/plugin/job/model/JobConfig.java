package com.mdframe.forge.plugin.job.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务配置模型
 */
@Data
public class JobConfig {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务名称（唯一）
     */
    private String jobName;
    
    /**
     * 任务分组
     */
    private String jobGroup;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 执行器Bean名称
     */
    private String executorBean;
    
    /**
     * 执行器方法名
     */
    private String executorMethod;
    
    /**
     * 执行器Handler
     */
    private String executorHandler;
    
    /**
     * 执行器服务名（RPC模式）
     */
    private String executorService;
    
    /**
     * Cron表达式
     */
    private String cronExpression;
    
    /**
     * 任务参数
     */
    private String jobParam;
    
    /**
     * 任务状态：0-停止 1-运行
     */
    private Integer status;
    
    /**
     * 执行模式：BEAN-Bean模式 HANDLER-Handler模式
     */
    private String executeMode;
    
    /**
     * 失败重试次数
     */
    private Integer retryCount;
    
    /**
     * 告警邮箱
     */
    private String alarmEmail;
    
    /**
     * WebHook地址
     */
    private String webhookUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
