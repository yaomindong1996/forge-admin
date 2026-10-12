package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.job.dto.JobConfigQuery;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;

/**
 * 任务配置Service
 */
public interface ISysJobConfigService extends IService<SysJobConfig> {
    
    /**
     * 分页查询任务列表
     */
    Page<JobConfigVO> selectJobPage(Page<JobConfigVO> page, JobConfigQuery query);

    /**
     * 查询任务详情
     */
    JobConfigVO selectJobDetail(Long id);
    
    /**
     * 添加任务并启动
     */
    void addJob(JobConfigSaveRequest request);
    
    /**
     * 更新任务
     */
    void updateJob(JobConfigSaveRequest request);
    
    /**
     * 删除任务
     */
    void deleteJob(Long id);
    
    /**
     * 启动任务
     */
    void startJob(Long id);
    
    /**
     * 停止任务
     */
    void stopJob(Long id);
    
    /**
     * 立即执行一次
     */
    void triggerJob(Long id);

    /**
     * 重新同步数据库期望状态到Quartz
     */
    void retrySynchronization(Long id);

    /**
     * 删除并重建数据库配置对应的 Quartz Job/Trigger。
     */
    void rebuild(Long id);
    
    /**
     * 更新Cron表达式
     */
    void updateCron(Long id, String cronExpression);

    /**
     * 按任务名与分组查询未删除任务，供外部模块维护自管理的定时任务
     */
    SysJobConfig findByJobKey(String jobName, String jobGroup);
}
