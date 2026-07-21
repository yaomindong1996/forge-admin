package com.mdframe.forge.plugin.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.dto.JobLogQuery;
import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.service.ISysJobLogService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.log.context.OperationAuditContext;
import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * 任务日志REST接口
 */
@RestController
@RequestMapping("/job/log")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(prefix = "forge.job", name = "enable-api", havingValue = "true", matchIfMissing = true)
public class JobLogController {
    
    private final ISysJobLogService jobLogService;

    private final DynamicExportEngine dynamicExportEngine;

    /**
     * 分页查询日志
     */
    @GetMapping("/page")
    @SaCheckPermission(JobPermissions.LOG_LIST)
    @ApiEncrypt
    public RespInfo<Page<JobLogVO>> page(PageQuery pageQuery, JobLogQuery query) {
        Page<JobLogVO> page = jobLogService.selectLogPage(pageQuery.toPage(), query);
        return RespInfo.success(page);
    }
    
    /**
     * 查询日志详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission(JobPermissions.LOG_DETAIL)
    @ApiEncrypt
    public RespInfo<JobLogDetailVO> detail(@PathVariable Long id) {
        JobLogDetailVO log = jobLogService.selectLogDetail(id);
        return RespInfo.success(log);
    }

    /**
     * 按安全白名单导出日志。
     */
    @PostMapping("/export")
    @SaCheckPermission(JobPermissions.LOG_EXPORT)
    @OperationLog(module = "定时任务", type = OperationType.EXPORT, desc = "导出任务日志",
            saveRequestParams = false, saveResponseResult = false)
    public void export(@RequestBody(required = false) Map<String, Object> queryParams,
                       HttpServletResponse response) {
        dynamicExportEngine.export(response, "sys_job_log_export", queryParams);
    }
    
    /**
     * 清理日志
     */
    @DeleteMapping("/clean")
    @SaCheckPermission(JobPermissions.LOG_CLEAN)
    @OperationLog(module = "定时任务", type = OperationType.DELETE, desc = "清理任务日志",
            saveRequestParams = false, saveResponseResult = false)
    @ApiEncrypt
    public RespInfo<Integer> clean(
            @RequestParam(defaultValue = "30") @Min(0) @Max(3650) int days) {
        OperationAuditContext.setBeforeData(Map.of("retentionDays", days));
        int count = jobLogService.cleanLog(days);
        OperationAuditContext.setAfterData(Map.of("retentionDays", days, "cleanedCount", count));
        OperationAuditContext.setDiffData(Map.of("cleanedCount", count));
        return RespInfo.success(count);
    }
}
