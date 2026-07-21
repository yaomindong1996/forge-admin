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
     * 调度类型：CRON-周期执行 ONCE-仅执行一次
     */
    private String scheduleType;
    
    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 一次性任务本地触发时间
     */
    private LocalDateTime fireOnceTime;

    /**
     * IANA时区
     */
    private String timezone;
    
    /**
     * 任务参数
     */
    private String jobParam;
    
    /**
     * 任务状态：0-停止 1-运行 2-已结束
     */
    private Integer status;
    
    /**
     * 执行模式：BEAN-Bean模式 HANDLER-Handler模式
     */
    private String executeMode;

    /**
     * 调用方式：SINGLE-单一执行器 FLOW-流程编排
     */
    private String invokeMode;

    private String flowModelKey;

    private Integer flowModelVersion;

    private String flowDeploymentId;

    private String flowProcessDefinitionId;

    /**
     * 并发策略：ALLOW-允许并行 SKIP_IF_RUNNING-运行中跳过
     */
    private String concurrentPolicy;

    /**
     * 错过触发策略：FIRE_ONCE_NOW-立即补偿一次 DO_NOTHING-不补偿
     */
    private String misfirePolicy;

    /**
     * 是否显式声明任务可幂等重试：0-否 1-是
     */
    private Integer idempotentFlag;
    
    /**
     * 失败重试次数
     */
    private Integer retryCount;

    private Integer alarmEnabled;

    private String alarmChannels;

    private String alarmRecipientUserIds;
    
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
