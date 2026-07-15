package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationPublishRun;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRollbackDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishResultVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 按原运行单和原幂等边界恢复部分失败的发布或回滚。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationPublishRecoveryService {

    private final BusinessApplicationPublishRunService runService;
    private final BusinessApplicationPublishService publishService;
    private final BusinessApplicationRollbackService rollbackService;

    public BusinessApplicationPublishResultVO recover(Long applicationId, Long runId) {
        AiBusinessApplicationPublishRun run = runService.beginRecovery(applicationId, runId);
        if ("PUBLISH".equals(run.getOperationType())) {
            return publishService.resume(run, new BusinessApplicationPublishDTO());
        }
        if ("ROLLBACK".equals(run.getOperationType())) {
            return rollbackService.resume(run, new BusinessApplicationRollbackDTO());
        }
        throw new BusinessException("不支持的应用发布运行类型");
    }
}
