package com.mdframe.forge.plugin.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务配置实体
 */
@Data
@TableName("sys_job_config")
public class SysJobConfig {
    
    @TableId(type = IdType.AUTO)
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

    /**
     * 保存时固定的已发布流程模型Key
     */
    private String flowModelKey;

    /**
     * 保存时固定的已发布流程模型版本
     */
    private Integer flowModelVersion;

    /**
     * 保存时固定的Flowable部署ID
     */
    private String flowDeploymentId;

    /**
     * 保存时固定的Flowable流程定义ID
     */
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

    /**
     * 连续失败次数，成功后清零，跳过不改变。
     */
    private Integer consecutiveFailures;

    /**
     * 最近一次推进连续失败统计的执行完成时间。
     */
    private LocalDateTime lastCompletionTime;

    /**
     * 最近一次推进连续失败统计的执行ID。
     */
    private Long lastCompletionExecutionId;

    /**
     * 是否启用最终失败告警：0-否 1-是
     */
    private Integer alarmEnabled;

    /**
     * 告警渠道，逗号分隔：WEB、EMAIL
     */
    private String alarmChannels;

    /**
     * 站内信接收用户ID，逗号分隔
     */
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
     * 数据库配置与Quartz运行态同步状态
     */
    private String syncStatus;

    /**
     * 最近一次同步错误
     */
    private String syncError;

    /**
     * 最近一次同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除标志：0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;
}
