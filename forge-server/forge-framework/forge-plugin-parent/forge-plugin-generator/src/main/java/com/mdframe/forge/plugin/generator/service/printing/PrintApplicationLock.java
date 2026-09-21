package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 应用行是打印变更与应用版本提交共同的事务互斥点。 */
@Component
@RequiredArgsConstructor
public class PrintApplicationLock {
    private final BusinessApplicationMapper applications;

    public AiBusinessApplication lock(Long tenantId, Long applicationId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("应用打印写操作必须在同一数据库事务中执行");
        }
        if (tenantId == null || tenantId <= 0 || applicationId == null || applicationId <= 0) {
            throw PrintFailure.denied();
        }
        AiBusinessApplication application = applications.lockEntityById(tenantId, applicationId);
        if (application == null) {
            throw PrintFailure.denied();
        }
        return application;
    }
}
