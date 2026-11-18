package com.mdframe.forge.plugin.capability.opengateway.job;

import com.mdframe.forge.plugin.capability.opengateway.mapper.AiCapabilityOpenapiIdempotencyMapper;
import com.mdframe.forge.plugin.job.executor.IJobExecutor;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.job.annotation.JobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 开放网关幂等快照留存清理 Handler。物理删除 {@code expires_at < CURRENT_TIMESTAMP} 的
 * 超期快照（跨租户、留存清理场景，spec 8-4 已说明；行级删除仍走逻辑删除规范）。
 * 参数为单批删除行数，空取默认值；分批循环直到删净，避免大事务。
 */
@Slf4j
@Component
@ConditionalOnClass(IJobExecutor.class)
@IgnoreTenant
@RequiredArgsConstructor
@JobHandler(value = "capabilityOpenapiIdempotencyClean",
        description = "开放网关幂等快照清理", group = "CAPABILITY")
public class OpenapiIdempotencyCleanJob implements IJobExecutor {

    private static final int DEFAULT_BATCH_SIZE = 500;
    private static final int MAX_BATCHES = 200;

    private final AiCapabilityOpenapiIdempotencyMapper idempotencyMapper;

    @Override
    public String execute(String param) {
        int batchSize = parseBatchSize(param);
        long total = 0;
        int batches = 0;
        while (batches < MAX_BATCHES) {
            int deleted = idempotencyMapper.deleteExpired(batchSize);
            total += deleted;
            batches++;
            if (deleted < batchSize) {
                break;
            }
        }
        String summary = String.format("幂等快照清理完成: batches=%d, deleted=%d", batches, total);
        log.info(summary);
        return summary;
    }

    private int parseBatchSize(String param) {
        if (param == null || param.isBlank()) {
            return DEFAULT_BATCH_SIZE;
        }
        try {
            int size = Integer.parseInt(param.trim());
            return size > 0 ? Math.min(size, 5000) : DEFAULT_BATCH_SIZE;
        }
        catch (NumberFormatException exception) {
            log.warn("幂等快照清理参数非法，使用默认批次大小: param={}", param);
            return DEFAULT_BATCH_SIZE;
        }
    }
}
