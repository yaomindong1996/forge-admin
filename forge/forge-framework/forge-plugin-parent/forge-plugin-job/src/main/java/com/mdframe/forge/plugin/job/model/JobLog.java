package com.mdframe.forge.plugin.job.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行日志
 */
@Data
public class JobLog {
    
    /**
     * 日志ID
     */
    private Long id;
    
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
     * 开始执行时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束执行时间
     */
    private LocalDateTime endTime;
    
    /**
     * 执行耗时(ms)
     */
    private Long duration;
    
    /**
     * 执行状态：1-成功 0-失败
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
}
