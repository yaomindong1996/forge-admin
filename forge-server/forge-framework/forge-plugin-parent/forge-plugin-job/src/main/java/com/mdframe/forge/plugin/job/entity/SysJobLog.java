package com.mdframe.forge.plugin.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体
 */
@Data
@TableName("sys_job_log")
public class SysJobLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务配置ID
     */
    private Long jobConfigId;
    
    /**
     * 任务名称
     */
    private String jobName;
    
    /**
     * 任务分组
     */
    private String jobGroup;
    
    /**
     * 执行器Handler
     */
    private String executorHandler;
    
    /**
     * 任务参数
     */
    private String jobParam;
    
    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 触发类型：UNKNOWN、SCHEDULED、MANUAL
     */
    private String triggerType;

    /**
     * Quartz 原计划触发时间
     */
    private LocalDateTime scheduledFireTime;

    /**
     * Quartz 执行实例ID
     */
    private String fireInstanceId;

    /**
     * Flowable流程实例ID
     */
    private String processInstanceId;
    
    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 运行中任务最近心跳时间。
     */
    private LocalDateTime heartbeatTime;
    
    /**
     * 结束执行时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时(ms)
     */
    private Long duration;
    
    /**
     * 执行状态：0-失败 1-成功 2-运行中 3-已跳过
     */
    private Integer status;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 异常信息
     */
    private String exceptionMsg;
    
    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 删除标志：0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;
}
