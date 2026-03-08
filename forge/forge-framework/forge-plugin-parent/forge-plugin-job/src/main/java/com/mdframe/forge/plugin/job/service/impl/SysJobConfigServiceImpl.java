package com.mdframe.forge.plugin.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.service.ISysJobConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务配置Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobConfigServiceImpl extends ServiceImpl<SysJobConfigMapper, SysJobConfig> implements ISysJobConfigService {
    
    private final JobScheduler jobScheduler;
    
    @Override
    public Page<SysJobConfig> selectJobPage(Page<SysJobConfig> page, SysJobConfig query) {
        LambdaQueryWrapper<SysJobConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getJobName()), SysJobConfig::getJobName, query.getJobName())
                .eq(StringUtils.isNotBlank(query.getJobGroup()), SysJobConfig::getJobGroup, query.getJobGroup())
                .eq(StringUtils.isNotBlank(query.getExecuteMode()), SysJobConfig::getExecuteMode, query.getExecuteMode())
                .eq(query.getStatus() != null, SysJobConfig::getStatus, query.getStatus())
                .orderByDesc(SysJobConfig::getCreateTime);
        return this.page(page, wrapper);
    }
    
    @Override
    //@Transactional(rollbackFor = Exception.class)
    public boolean addJob(SysJobConfig jobConfig) {
        // 保存到数据库
        boolean saved = this.save(jobConfig);
        if (!saved) {
            return false;
        }
        
        // 注册到Quartz
        JobConfig config = convertToJobConfig(jobConfig);
        return jobScheduler.addJob(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJob(SysJobConfig jobConfig) {
        // 更新数据库
        boolean updated = this.updateById(jobConfig);
        if (!updated) {
            return false;
        }
        
        // 更新Quartz任务
        JobConfig config = convertToJobConfig(jobConfig);
        return jobScheduler.updateJob(config);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(Long id) {
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            return false;
        }
        
        // 从Quartz删除
        jobScheduler.deleteJob(jobConfig.getJobName(), jobConfig.getJobGroup());
        
        // 从数据库删除
        return this.removeById(id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startJob(Long id) {
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            return false;
        }
        
        // 恢复Quartz任务
        boolean success = jobScheduler.resumeJob(jobConfig.getJobName(), jobConfig.getJobGroup());
        if (success) {
            // 更新数据库状态
            jobConfig.setStatus(1);
            this.updateById(jobConfig);
        }
        return success;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopJob(Long id) {
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            return false;
        }
        
        // 暂停Quartz任务
        boolean success = jobScheduler.pauseJob(jobConfig.getJobName(), jobConfig.getJobGroup());
        if (success) {
            // 更新数据库状态
            jobConfig.setStatus(0);
            this.updateById(jobConfig);
        }
        return success;
    }
    
    @Override
    public boolean triggerJob(Long id) {
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            return false;
        }
        
        return jobScheduler.triggerJob(jobConfig.getJobName(), jobConfig.getJobGroup());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCron(Long id, String cronExpression) {
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            return false;
        }
        
        // 更新Quartz
        boolean success = jobScheduler.updateCron(jobConfig.getJobName(), jobConfig.getJobGroup(), cronExpression);
        if (success) {
            // 更新数据库
            jobConfig.setCronExpression(cronExpression);
            this.updateById(jobConfig);
        }
        return success;
    }
    
    /**
     * 转换为JobConfig
     */
    private JobConfig convertToJobConfig(SysJobConfig entity) {
        JobConfig config = new JobConfig();
        BeanUtil.copyProperties(entity, config);
        return config;
    }
}
