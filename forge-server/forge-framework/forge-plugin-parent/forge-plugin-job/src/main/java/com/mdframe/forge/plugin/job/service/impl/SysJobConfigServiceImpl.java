package com.mdframe.forge.plugin.job.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.job.dto.JobConfigQuery;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.manager.JobScheduleCoordinator;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.scheduler.JobScheduleException;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import com.mdframe.forge.plugin.job.service.ISysJobConfigService;
import com.mdframe.forge.plugin.job.service.JobCronService;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.plugin.job.service.JobFlowOrchestrationService;
import com.mdframe.forge.plugin.job.service.JobManagementSecurityService;
import com.mdframe.forge.plugin.job.support.JobConfigValidator;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;
import com.mdframe.forge.plugin.job.vo.JobExecutorCatalogVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 任务配置Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysJobConfigServiceImpl extends ServiceImpl<SysJobConfigMapper, SysJobConfig> implements ISysJobConfigService {

    private final JobScheduler jobScheduler;

    private final JobScheduleCoordinator scheduleCoordinator;

    private final JobConfigValidator jobConfigValidator;

    private final JobExecutorCatalogService executorCatalogService;

    private final JobCronService jobCronService;

    private final JobManagementSecurityService managementSecurityService;

    private final JobFlowOrchestrationService jobFlowOrchestrationService;

    @Override
    public Page<JobConfigVO> selectJobPage(Page<JobConfigVO> page, JobConfigQuery query) {
        Page<JobConfigVO> result = this.baseMapper.selectJobPage(page, query);
        result.getRecords().forEach(this::enrichDisplayFields);
        return result;
    }

    @Override
    public JobConfigVO selectJobDetail(Long id) {
        JobConfigVO detail = this.baseMapper.selectJobDetail(id);
        if (detail != null) {
            enrichDisplayFields(detail);
        }
        return detail;
    }

    @Override
    public void addJob(JobConfigSaveRequest request) {
        jobConfigValidator.validateCreate(request);
        managementSecurityService.assertCanManageTarget(request);
        SysJobConfig jobConfig = new SysJobConfig();
        BeanUtil.copyProperties(request, jobConfig);
        jobFlowOrchestrationService.applyBinding(request, jobConfig);
        jobConfig.setSyncStatus(JobScheduleCoordinator.SYNC_PENDING);
        jobConfig.setSyncError(null);
        jobConfig.setSyncTime(null);
        jobConfig.setVersion(0);
        if (!this.save(jobConfig)) {
            throw new BusinessException("任务配置保存失败");
        }
        synchronizeDesiredState(jobConfig.getId(), "任务配置");
    }
    
    @Override
    public void updateJob(JobConfigSaveRequest request) {
        SysJobConfig current = requireConfig(request == null ? null : request.getId());
        jobConfigValidator.validateUpdate(request, current);
        managementSecurityService.assertCanManageTarget(request);
        managementSecurityService.assertCanManageProtectedTask(current);
        BeanUtil.copyProperties(request, current,
                "id", "jobName", "jobGroup", "syncStatus", "syncError", "syncTime",
                "createTime", "updateTime", "delFlag");
        jobFlowOrchestrationService.applyBinding(request, current);
        markPending(current);
        updateDesiredState(current);
        synchronizeDesiredState(current.getId(), "任务配置");
    }
    
    @Override
    public void deleteJob(Long id) {
        SysJobConfig jobConfig = requireConfig(id);
        managementSecurityService.assertCanManageProtectedTask(jobConfig);
        jobConfig.setSyncStatus(JobScheduleCoordinator.DELETE_PENDING);
        jobConfig.setSyncError(null);
        jobConfig.setSyncTime(null);
        updateDesiredState(jobConfig);
        synchronizeDesiredState(id, "删除请求");
    }
    
    @Override
    public void startJob(Long id) {
        SysJobConfig jobConfig = requireConfig(id);
        if (Integer.valueOf(2).equals(jobConfig.getStatus())) {
            throw new BusinessException("已结束任务不能直接启用，请编辑执行计划后重新保存");
        }
        updateStatusAndSynchronize(id, 1);
    }
    
    @Override
    public void stopJob(Long id) {
        updateStatusAndSynchronize(id, 0);
    }
    
    @Override
    public void triggerJob(Long id) {
        SysJobConfig jobConfig = requireConfig(id);
        managementSecurityService.assertCanManageProtectedTask(jobConfig);
        if (!JobScheduleCoordinator.SYNCED.equals(jobConfig.getSyncStatus())) {
            throw new BusinessException("任务尚未同步到调度服务，请先重新同步");
        }
        jobScheduler.triggerJob(jobConfig.getJobName(), jobConfig.getJobGroup(), jobConfig.getId());
    }

    @Override
    public void retrySynchronization(Long id) {
        SysJobConfig jobConfig = requireConfig(id);
        managementSecurityService.assertCanManageProtectedTask(jobConfig);
        try {
            scheduleCoordinator.retrySynchronization(id);
        } catch (JobScheduleException exception) {
            throw new BusinessException("重新同步失败，请检查调度服务状态后再试", exception);
        }
    }
    
    @Override
    public void updateCron(Long id, String cronExpression) {
        SysJobConfig jobConfig = requireConfig(id);
        JobConfigSaveRequest request = new JobConfigSaveRequest();
        BeanUtil.copyProperties(jobConfig, request);
        request.setCronExpression(cronExpression);
        jobConfigValidator.validateUpdate(request, jobConfig);
        managementSecurityService.assertCanManageTarget(request);
        managementSecurityService.assertCanManageProtectedTask(jobConfig);
        jobConfig.setCronExpression(request.getCronExpression());
        jobFlowOrchestrationService.applyBinding(request, jobConfig);
        markPending(jobConfig);
        updateDesiredState(jobConfig);
        synchronizeDesiredState(id, "执行计划");
    }

    private void updateStatusAndSynchronize(Long id, int status) {
        SysJobConfig jobConfig = requireConfig(id);
        managementSecurityService.assertCanManageProtectedTask(jobConfig);
        jobConfig.setStatus(status);
        markPending(jobConfig);
        updateDesiredState(jobConfig);
        synchronizeDesiredState(id, "运行状态");
    }

    private SysJobConfig requireConfig(Long id) {
        if (id == null) {
            throw new BusinessException("任务ID不能为空");
        }
        SysJobConfig jobConfig = this.getById(id);
        if (jobConfig == null) {
            throw new BusinessException("定时任务不存在");
        }
        return jobConfig;
    }

    private void markPending(SysJobConfig jobConfig) {
        jobConfig.setSyncStatus(JobScheduleCoordinator.SYNC_PENDING);
        jobConfig.setSyncError(null);
        jobConfig.setSyncTime(null);
    }

    private void updateDesiredState(SysJobConfig jobConfig) {
        if (!this.updateById(jobConfig)) {
            throw new BusinessException("任务配置已被修改，请刷新后重试");
        }
    }

    private void synchronizeDesiredState(Long id, String savedSubject) {
        try {
            scheduleCoordinator.synchronize(id);
        } catch (JobScheduleException exception) {
            throw new BusinessException(savedSubject
                    + "已保存，但调度同步失败，请在列表中点击“重新同步”", exception);
        }
    }

    private void enrichDisplayFields(JobConfigVO config) {
        if ("FLOW".equals(config.getInvokeMode())) {
            config.setExecutionSummary(config.getFlowModelKey() == null
                    ? "未绑定流程"
                    : config.getFlowModelKey() + " · V" + config.getFlowModelVersion());
            enrichScheduleFields(config);
            return;
        }
        JobExecutorCatalogVO catalogItem = executorCatalogService.findByTarget(
                config.getExecuteMode(), config.getExecutorHandler(),
                config.getExecutorBean(), config.getExecutorMethod());
        if (catalogItem != null) {
            config.setExecutionSummary(catalogItem.getDisplayName());
        } else if ("RPC".equals(config.getExecuteMode())) {
            config.setExecutionSummary(joinTarget(config.getExecutorService(), config.getExecutorHandler(), "未配置远程服务"));
        } else if ("HANDLER".equals(config.getExecuteMode())) {
            config.setExecutionSummary(StringUtils.defaultIfBlank(config.getExecutorHandler(), "未配置任务处理器"));
        } else {
            config.setExecutionSummary(joinTarget(config.getExecutorBean(), config.getExecutorMethod(), "未配置本地服务方法"));
        }
        enrichScheduleFields(config);
    }

    private void enrichScheduleFields(JobConfigVO config) {
        config.setScheduleSummary(jobCronService.describe(config.getCronExpression()));
        if (JobScheduleCoordinator.SYNCED.equals(config.getSyncStatus())
                && Integer.valueOf(1).equals(config.getStatus())) {
            config.setNextFireTime(jobScheduler.nextFireTime(
                    config.getJobName(), config.getJobGroup(), config.getTimezone()));
        }
    }

    private String joinTarget(String first, String second, String fallback) {
        if (StringUtils.isBlank(first) && StringUtils.isBlank(second)) {
            return fallback;
        }
        return StringUtils.defaultString(first) + (StringUtils.isNotBlank(first) && StringUtils.isNotBlank(second) ? " · " : "")
                + StringUtils.defaultString(second);
    }
}
