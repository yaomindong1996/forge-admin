package com.mdframe.forge.plugin.job.service;

import com.mdframe.forge.plugin.job.constant.JobPermissions;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.vo.JobExecutorCatalogVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 对危险执行目标、代码登记任务和敏感日志执行 Service 层二次校验。
 */
@Service
@RequiredArgsConstructor
public class JobManagementSecurityService {

    private static final Set<String> DANGEROUS_MODES = Set.of("BEAN", "RPC");
    private static final String PROTECTED_SOURCE = "SCHEDULED_JOB";

    private final JobExecutorCatalogService executorCatalogService;

    public void assertCanManageTarget(JobConfigSaveRequest request) {
        if (request != null && DANGEROUS_MODES.contains(request.getExecuteMode())) {
            requireDangerousPermission("当前任务执行目标需要危险目标管理权限");
        }
    }

    public void assertCanManageProtectedTask(SysJobConfig config) {
        if (config == null) {
            return;
        }
        JobExecutorCatalogVO catalogItem = executorCatalogService.findByTarget(
                config.getExecuteMode(), config.getExecutorHandler(),
                config.getExecutorBean(), config.getExecutorMethod());
        if (catalogItem != null && PROTECTED_SOURCE.equals(catalogItem.getSource())) {
            requireDangerousPermission("代码登记的受保护任务需要危险目标管理权限");
        }
    }

    public void assertSensitiveLogAccess() {
        if (!SessionHelper.hasPermission(JobPermissions.LOG_DETAIL)) {
            throw new BusinessException(403, "无权查看任务执行敏感摘要");
        }
    }

    private void requireDangerousPermission(String message) {
        if (!SessionHelper.hasPermission(JobPermissions.CONFIG_DANGEROUS)) {
            throw new BusinessException(403, message);
        }
    }
}
