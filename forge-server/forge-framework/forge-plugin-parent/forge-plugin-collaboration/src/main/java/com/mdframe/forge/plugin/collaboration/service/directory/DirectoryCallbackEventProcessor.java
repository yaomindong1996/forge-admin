package com.mdframe.forge.plugin.collaboration.service.directory;

import com.mdframe.forge.plugin.collaboration.domain.entity.SocialCallbackEvent;
import com.mdframe.forge.plugin.collaboration.domain.model.DirectorySyncCommand;
import com.mdframe.forge.plugin.collaboration.service.CollaborationCallbackInboxService;
import com.mdframe.forge.starter.collaboration.model.DirectorySyncScope;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录回调事件消费器（Task 11）。
 * <p>
 * 从收件箱 CAS 领取事件并把成员/部门/标签变更归并为平台无关的范围同步命令：
 * 企微不提供单对象增量拉取，同一连接的通讯录事件合并为一次
 * {@code triggerSource=CALLBACK} 的目录同步，由编排器保证并发互斥与失败关闭。
 * 处理过程不解密事件正文、不打印个人资料。
 */
@Slf4j
@Service
@IgnoreTenant
@RequiredArgsConstructor
public class DirectoryCallbackEventProcessor {

    /** 企微通讯录事件前缀（含成员/部门/标签变更：change_contact.create_user 等） */
    private static final String DIRECTORY_EVENT_PREFIX = "change_contact";
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int ERROR_SUMMARY_MAX_LENGTH = 200;

    private final CollaborationCallbackInboxService inboxService;
    private final DirectorySyncOrchestrator syncOrchestrator;

    /**
     * 单租户处理结果
     *
     * @param claimed   本轮领取的事件数
     * @param processed 处理成功数（含非目录事件的幂等吞并）
     * @param failed    处理失败数（已按退避策略安排重试或丢弃）
     */
    public record ProcessResult(int claimed, int processed, int failed) {

        public static ProcessResult empty() {
            return new ProcessResult(0, 0, 0);
        }
    }

    /**
     * 领取并处理指定租户的待处理回调事件
     */
    public ProcessResult processTenant(Long tenantId, int batchSize, String workerId) {
        int size = batchSize > 0 ? batchSize : DEFAULT_BATCH_SIZE;
        int claimed = inboxService.claimPendingEvents(tenantId, size, workerId);
        if (claimed <= 0) {
            return ProcessResult.empty();
        }
        List<SocialCallbackEvent> events = inboxService.listClaimedEvents(tenantId, workerId);
        int processed = 0;
        int failed = 0;

        // 目录事件按连接归并；非目录事件直接吞并（一期仅消费通讯录事件）
        Map<Long, List<SocialCallbackEvent>> directoryEvents = new LinkedHashMap<>();
        for (SocialCallbackEvent event : events) {
            if (isDirectoryEvent(event.getEventType())) {
                directoryEvents.computeIfAbsent(event.getConnectionId(), k -> new ArrayList<>())
                        .add(event);
            } else {
                inboxService.markProcessed(event.getId(), tenantId, workerId);
                processed++;
            }
        }

        for (Map.Entry<Long, List<SocialCallbackEvent>> entry : directoryEvents.entrySet()) {
            Long connectionId = entry.getKey();
            List<SocialCallbackEvent> batch = entry.getValue();
            try {
                syncOrchestrator.synchronize(connectionId, new DirectorySyncCommand(
                        "INCREMENT", "CALLBACK", DirectorySyncScope.FULL, null, null));
                for (SocialCallbackEvent event : batch) {
                    inboxService.markProcessed(event.getId(), tenantId, workerId);
                }
                processed += batch.size();
                log.info("回调触发目录同步成功: connectionId={}, 归并事件数={}", connectionId, batch.size());
            } catch (BusinessException e) {
                // 并发互斥/校验失败等业务性失败，按收件箱退避重试
                failed += markBatchFailed(batch, tenantId, workerId, "SYNC_REJECTED", e.getMessage());
                log.warn("回调触发目录同步被拒绝: connectionId={}, reason={}", connectionId, e.getMessage());
            } catch (Exception e) {
                failed += markBatchFailed(batch, tenantId, workerId, "SYNC_ERROR", e.getMessage());
                log.error("回调触发目录同步异常: connectionId={}", connectionId, e);
            }
        }
        return new ProcessResult(claimed, processed, failed);
    }

    private int markBatchFailed(List<SocialCallbackEvent> batch, Long tenantId, String workerId,
                                String errorCode, String message) {
        String summary = truncate(message);
        for (SocialCallbackEvent event : batch) {
            inboxService.markFailed(event, workerId, errorCode, summary);
        }
        return batch.size();
    }

    private boolean isDirectoryEvent(String eventType) {
        return StringUtils.hasText(eventType) && eventType.startsWith(DIRECTORY_EVENT_PREFIX);
    }

    private String truncate(String message) {
        if (!StringUtils.hasText(message)) {
            return "处理失败";
        }
        return message.length() > ERROR_SUMMARY_MAX_LENGTH
                ? message.substring(0, ERROR_SUMMARY_MAX_LENGTH) : message;
    }
}
