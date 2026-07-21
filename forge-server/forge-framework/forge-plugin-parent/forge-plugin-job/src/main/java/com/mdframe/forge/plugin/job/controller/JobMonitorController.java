package com.mdframe.forge.plugin.job.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.manager.JobObservabilityManager;
import com.mdframe.forge.plugin.job.vo.JobMonitorSummaryVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务轻量监控接口。
 */
@RestController
@RequestMapping("/job/monitor")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "enable-api", havingValue = "true", matchIfMissing = true)
@ApiDecrypt
@ApiEncrypt
public class JobMonitorController {

    private final JobObservabilityManager observabilityManager;

    @GetMapping("/summary")
    @SaCheckPermission(JobPermissions.CONFIG_LIST)
    public RespInfo<JobMonitorSummaryVO> summary() {
        return RespInfo.success(observabilityManager.getSummary());
    }
}
