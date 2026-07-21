package com.mdframe.forge.plugin.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.dto.JobConfigQuery;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.dto.JobCronPreviewRequest;
import com.mdframe.forge.plugin.job.manager.JobObservabilityManager;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.service.ISysJobConfigService;
import com.mdframe.forge.plugin.job.service.JobCronService;
import com.mdframe.forge.plugin.job.service.JobExecutorCatalogService;
import com.mdframe.forge.plugin.job.service.JobTimezoneCatalogService;
import com.mdframe.forge.plugin.job.vo.JobConfigVO;
import com.mdframe.forge.plugin.job.vo.JobCronPreviewVO;
import com.mdframe.forge.plugin.job.vo.JobExecutorCatalogVO;
import com.mdframe.forge.plugin.job.vo.JobTimezoneOptionVO;
import com.mdframe.forge.plugin.job.vo.JobOverviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理REST接口
 */
@RestController
@RequestMapping("/job/config")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "enable-api", havingValue = "true", matchIfMissing = true)
@ApiDecrypt
@ApiEncrypt
public class JobConfigController {
    
    private final ISysJobConfigService jobConfigService;

    private final JobExecutorCatalogService executorCatalogService;

    private final JobCronService jobCronService;

    private final JobTimezoneCatalogService timezoneCatalogService;

    private final JobObservabilityManager observabilityManager;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<Page<JobConfigVO>> page(PageQuery pageQuery, JobConfigQuery query) {
        Page<JobConfigVO> page = jobConfigService.selectJobPage(pageQuery.toPage(), query);
        return RespInfo.success(page);
    }
    
    /**
     * 查询任务详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<JobConfigVO> detail(@PathVariable Long id) {
        JobConfigVO config = jobConfigService.selectJobDetail(id);
        return RespInfo.success(config);
    }

    /**
     * 查询任务执行概览。
     */
    @GetMapping("/{id}/overview")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<JobOverviewVO> overview(@PathVariable Long id) {
        return RespInfo.success(observabilityManager.getOverview(id));
    }

    /**
     * 查询已注册的任务处理器目录。
     */
    @GetMapping("/executors")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<List<JobExecutorCatalogVO>> executors() {
        return RespInfo.success(executorCatalogService.listExecutors());
    }

    /**
     * 查询当前运行环境支持的 IANA 时区。
     */
    @GetMapping("/timezones")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<List<JobTimezoneOptionVO>> timezones() {
        return RespInfo.success(timezoneCatalogService.listTimezones(Clock.systemUTC()));
    }

    /**
     * 校验Cron并预览未来执行时间。
     */
    @PostMapping("/cron/preview")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<JobCronPreviewVO> previewCron(@RequestBody JobCronPreviewRequest request) {
        return RespInfo.success(jobCronService.preview(request, Clock.systemDefaultZone()));
    }
    
    /**
     * 添加任务
     */
    @PostMapping
    @SaCheckPermission(JobPermissions.CONFIG_ADD)
    @OperationLog(module = "定时任务", type = OperationType.ADD, desc = "新增定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> add(@RequestBody JobConfigSaveRequest request) {
        jobConfigService.addJob(request);
        Map<String, Object> afterData = buildAuditSnapshot(request);
        OperationAuditContext.setAfterData(afterData);
        OperationAuditContext.setDiffData(afterData);
        return RespInfo.success();
    }
    
    /**
     * 更新任务
     */
    @PutMapping
    @SaCheckPermission(JobPermissions.CONFIG_EDIT)
    @OperationLog(module = "定时任务", type = OperationType.UPDATE, desc = "编辑定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> update(@RequestBody JobConfigSaveRequest request) {
        Map<String, Object> beforeData = buildAuditSnapshot(
                request == null ? null : jobConfigService.selectJobDetail(request.getId()));
        OperationAuditContext.setBeforeData(beforeData);
        jobConfigService.updateJob(request);
        recordAfterSnapshot(beforeData, request.getId());
        return RespInfo.success();
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission(JobPermissions.CONFIG_REMOVE)
    @OperationLog(module = "定时任务", type = OperationType.DELETE, desc = "删除定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> delete(@PathVariable Long id) {
        Map<String, Object> beforeData = buildAuditSnapshot(jobConfigService.selectJobDetail(id));
        OperationAuditContext.setBeforeData(beforeData);
        jobConfigService.deleteJob(id);
        Map<String, Object> afterData = new LinkedHashMap<>();
        afterData.put("id", id);
        afterData.put("deleted", true);
        recordAuditDiff(beforeData, afterData);
        return RespInfo.success();
    }
    
    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    @SaCheckPermission(JobPermissions.CONFIG_START)
    @OperationLog(module = "定时任务", type = OperationType.UPDATE, desc = "启用定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> start(@PathVariable Long id) {
        Map<String, Object> beforeData = recordBeforeSnapshot(id);
        jobConfigService.startJob(id);
        recordAfterSnapshot(beforeData, id);
        return RespInfo.success();
    }
    
    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    @SaCheckPermission(JobPermissions.CONFIG_STOP)
    @OperationLog(module = "定时任务", type = OperationType.UPDATE, desc = "停用定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> stop(@PathVariable Long id) {
        Map<String, Object> beforeData = recordBeforeSnapshot(id);
        jobConfigService.stopJob(id);
        recordAfterSnapshot(beforeData, id);
        return RespInfo.success();
    }
    
    /**
     * 立即执行一次
     */
    @PostMapping("/{id}/trigger")
    @SaCheckPermission(JobPermissions.CONFIG_TRIGGER)
    @OperationLog(module = "定时任务", type = OperationType.OTHER, desc = "立即运行定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> trigger(@PathVariable Long id) {
        Map<String, Object> beforeData = recordBeforeSnapshot(id);
        jobConfigService.triggerJob(id);
        Map<String, Object> afterData = new LinkedHashMap<>(beforeData);
        afterData.put("triggerSubmitted", true);
        recordAuditDiff(beforeData, afterData);
        return RespInfo.success();
    }

    /**
     * 重新同步任务配置到Quartz
     */
    @PostMapping("/{id}/sync")
    @SaCheckPermission(JobPermissions.CONFIG_SYNC)
    @OperationLog(module = "定时任务", type = OperationType.UPDATE, desc = "重新同步定时任务",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> sync(@PathVariable Long id) {
        Map<String, Object> beforeData = recordBeforeSnapshot(id);
        jobConfigService.retrySynchronization(id);
        recordAfterSnapshot(beforeData, id);
        return RespInfo.success();
    }
    
    /**
     * 更新Cron表达式
     */
    @PostMapping("/{id}/cron")
    @SaCheckPermission(JobPermissions.CONFIG_EDIT)
    @OperationLog(module = "定时任务", type = OperationType.UPDATE, desc = "更新任务执行计划",
            saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> updateCron(@PathVariable Long id, @RequestParam String cronExpression) {
        Map<String, Object> beforeData = recordBeforeSnapshot(id);
        jobConfigService.updateCron(id, cronExpression);
        recordAfterSnapshot(beforeData, id);
        return RespInfo.success();
    }

    private Map<String, Object> recordBeforeSnapshot(Long id) {
        Map<String, Object> beforeData = buildAuditSnapshot(jobConfigService.selectJobDetail(id));
        OperationAuditContext.setBeforeData(beforeData);
        return beforeData;
    }

    private void recordAfterSnapshot(Map<String, Object> beforeData, Long id) {
        recordAuditDiff(beforeData, buildAuditSnapshot(jobConfigService.selectJobDetail(id)));
    }

    private void recordAuditDiff(Map<String, Object> beforeData, Map<String, Object> afterData) {
        OperationAuditContext.setAfterData(afterData);
        Map<String, Object> diffData = new LinkedHashMap<>();
        diffData.put("before", beforeData);
        diffData.put("after", afterData);
        OperationAuditContext.setDiffData(diffData);
    }

    private Map<String, Object> buildAuditSnapshot(JobConfigSaveRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (request == null) {
            data.put("exists", false);
            return data;
        }
        data.put("id", request.getId());
        data.put("jobName", request.getJobName());
        data.put("jobGroup", request.getJobGroup());
        data.put("scheduleType", request.getScheduleType());
        data.put("status", request.getStatus());
        data.put("invokeMode", request.getInvokeMode());
        data.put("executeMode", request.getExecuteMode());
        data.put("flowModelKey", request.getFlowModelKey());
        data.put("flowModelVersion", request.getFlowModelVersion());
        data.put("alarmEnabled", request.getAlarmEnabled());
        data.put("alarmChannels", request.getAlarmChannels());
        return data;
    }

    private Map<String, Object> buildAuditSnapshot(JobConfigVO config) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (config == null) {
            data.put("exists", false);
            return data;
        }
        data.put("id", config.getId());
        data.put("jobName", config.getJobName());
        data.put("jobGroup", config.getJobGroup());
        data.put("scheduleType", config.getScheduleType());
        data.put("status", config.getStatus());
        data.put("invokeMode", config.getInvokeMode());
        data.put("executeMode", config.getExecuteMode());
        data.put("flowModelKey", config.getFlowModelKey());
        data.put("flowModelVersion", config.getFlowModelVersion());
        data.put("flowProcessDefinitionId", config.getFlowProcessDefinitionId());
        data.put("alarmEnabled", config.getAlarmEnabled());
        data.put("alarmChannels", config.getAlarmChannels());
        data.put("syncStatus", config.getSyncStatus());
        return data;
    }
}
