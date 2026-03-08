package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;

/**
 * 任务配置Service
 */
public interface ISysJobConfigService extends IService<SysJobConfig> {
    
    /**
     * 分页查询任务列表
     */
    Page<SysJobConfig> selectJobPage(Page<SysJobConfig> page, SysJobConfig query);
    
    /**
     * 添加任务并启动
     */
    boolean addJob(SysJobConfig jobConfig);
    
    /**
     * 更新任务
     */
    boolean updateJob(SysJobConfig jobConfig);
    
    /**
     * 删除任务
     */
    boolean deleteJob(Long id);
    
    /**
     * 启动任务
     */
    boolean startJob(Long id);
    
    /**
     * 停止任务
     */
    boolean stopJob(Long id);
    
    /**
     * 立即执行一次
     */
    boolean triggerJob(Long id);
    
    /**
     * 更新Cron表达式
     */
    boolean updateCron(Long id, String cronExpression);
}
