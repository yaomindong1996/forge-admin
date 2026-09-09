package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.domain.entity.SysMessage;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.flow.dto.FlowOverdueReminderConfig;
import com.mdframe.forge.starter.flow.entity.FlowOverdueReminderRecord;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.flow.mapper.FlowOverdueReminderRecordMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowOverdueReminderService;
import com.mdframe.forge.starter.flow.service.FlowTaskReceiverResolver;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程任务逾期提醒服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowOverdueReminderServiceImpl implements FlowOverdueReminderService {

    private static final String BIZ_TYPE = "FLOW_TASK_OVERDUE";
    private static final String MESSAGE_TYPE = "SYSTEM";
    private static final String SEND_SCOPE_USERS = "USERS";
    private static final String JUMP_URL_PREFIX = "/flow/todo?taskId=";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_BASE_MINUTES = 5;

    private final FlowTaskMapper flowTaskMapper;
    private final FlowOverdueReminderRecordMapper reminderRecordMapper;
    private final FlowOverdueReminderConfigResolver configResolver;
    private final FlowTaskReceiverResolver receiverResolver;
    private final MessageService messageService;

    @Value("${forge.flow.overdue-reminder.enabled:true}")
    private boolean enabled;

    @Value("${forge.flow.overdue-reminder.batch-size:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${forge.flow.overdue-reminder.scan-interval-ms:300000}")
    public void scheduledScan() {
        if (!enabled) {
            return;
        }
        scanAndSendOverdueReminders();
    }

    @Override
    public void scanAndSendOverdueReminders() {
        if (!enabled) {
            log.debug("流程任务逾期提醒扫描已关闭");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long size = Math.max(1, batchSize);
        long current = 1;
        int handled = 0;
        while (true) {
            IPage<FlowTask> page = selectOverduePage(current, size, now);
            List<FlowTask> tasks = page.getRecords();
            if (tasks == null || tasks.isEmpty()) {
                break;
            }
            for (FlowTask task : tasks) {
                try {
                    handleTask(task);
                    handled++;
                } catch (Exception e) {
                    log.warn("处理流程逾期提醒失败: taskId={}", task != null ? task.getTaskId() : null, e);
                }
            }
            if (tasks.size() < size || current >= page.getPages()) {
                break;
            }
            current++;
        }
        log.debug("流程任务逾期提醒扫描完成: handled={}", handled);
    }

    private IPage<FlowTask> selectOverduePage(long current, long size, LocalDateTime now) {
        return TenantContextHolder.executeIgnore(() ->
                flowTaskMapper.selectOverduePendingTasks(new Page<>(current, size), now));
    }

    private void handleTask(FlowTask task) {
        if (task == null || task.getDueDate() == null || task.getTaskId() == null) {
            return;
        }
        Long tenantId = resolveTenantId(task);
        if (tenantId == null || tenantId <= 0) {
            log.warn("流程任务逾期提醒缺少可信租户，跳过发送: taskId={}", task.getTaskId());
            return;
        }
        executeWithTenant(tenantId, () -> {
            FlowOverdueReminderConfig config = configResolver.resolve(task);
            if (!shouldSend(task, config)) {
                return;
            }
            sendOverdueReminder(task, config);
        });
    }

    @Override
    public void sendOverdueReminder(FlowTask task, FlowOverdueReminderConfig config) {
        if (!shouldSend(task, config)) {
            return;
        }

        Long tenantId = resolveTenantId(task);
        if (tenantId == null || tenantId <= 0) {
            return;
        }
        String reminderKey = nextReminderKey(tenantId, task, config);
        if (reminderKey == null) {
            return;
        }

        Set<Long> receiverIds = receiverResolver.resolveReceivers(task);
        List<String> channels = normalizeChannels(config.getChannels());
        for (String channel : channels) {
            FlowOverdueReminderRecord record = buildRecord(tenantId, task, config, reminderKey, channel, receiverIds);
            if (!claimRecord(record)) {
                continue;
            }
            if (receiverIds.isEmpty()) {
                markFailed(record, "未解析到逾期提醒接收人");
                log.warn("流程任务逾期提醒未解析到接收人: taskId={}, assignee={}, candidateUsers={}, candidateGroups={}",
                        task.getTaskId(), task.getAssignee(), task.getCandidateUsers(), task.getCandidateGroups());
                continue;
            }
            sendMessage(task, config, channel, reminderKey, receiverIds, record);
        }
    }

    private boolean shouldSend(FlowTask task, FlowOverdueReminderConfig config) {
        if (task == null || task.getDueDate() == null || task.getTaskId() == null) {
            return false;
        }
        // 完成、撤回、终结后的重试事件必须停止，避免已结束任务继续发提醒。
        if (task.getStatus() != null && !FlowTaskStatus.isActionable(task.getStatus())) {
            return false;
        }
        if (config == null || !config.isEnabled()) {
            return false;
        }
        if (isBlank(config.getTemplateCode())) {
            return false;
        }
        List<String> channels = normalizeChannels(config.getChannels());
        return !channels.isEmpty();
    }

    private String nextReminderKey(Long tenantId, FlowTask task, FlowOverdueReminderConfig config) {
        Integer countValue = reminderRecordMapper.countDistinctReminderKeysByTaskId(tenantId, task.getTaskId());
        int sentBatches = countValue == null ? 0 : countValue;
        if (FlowOverdueReminderConfig.REPEAT_INTERVAL.equals(config.getRepeatMode())) {
            int maxTimes = Math.max(1, config.getMaxTimes() == null ? 1 : config.getMaxTimes());
            if (sentBatches >= maxTimes) {
                return null;
            }
            FlowOverdueReminderRecord latest = reminderRecordMapper.selectLatestByTaskId(tenantId, task.getTaskId());
            if (latest != null && latest.getCreateTime() != null) {
                if (Integer.valueOf(2).equals(latest.getSendStatus())) {
                    if (latest.getNextRetryTime() != null
                            && latest.getNextRetryTime().isAfter(LocalDateTime.now())) {
                        return null;
                    }
                    return task.getTaskId() + ":" + (sentBatches + 1);
                }
                int interval = Math.max(30, config.getIntervalMinutes() == null ? 30 : config.getIntervalMinutes());
                LocalDateTime baseTime = latest.getSendTime() != null ? latest.getSendTime() : latest.getCreateTime();
                if (baseTime.plusMinutes(interval).isAfter(LocalDateTime.now())) {
                    return null;
                }
            }
            return task.getTaskId() + ":" + (sentBatches + 1);
        }

        if (sentBatches > 0) {
            return null;
        }
        return task.getTaskId() + ":once";
    }

    private FlowOverdueReminderRecord buildRecord(Long tenantId, FlowTask task, FlowOverdueReminderConfig config,
                                                  String reminderKey, String channel, Set<Long> receiverIds) {
        FlowOverdueReminderRecord record = new FlowOverdueReminderRecord();
        record.setTenantId(tenantId);
        record.setTaskId(task.getTaskId());
        record.setProcessInstanceId(task.getProcessInstanceId());
        record.setProcessDefKey(task.getProcessDefKey());
        record.setTaskDefKey(task.getTaskDefKey());
        record.setReminderKey(reminderKey);
        record.setChannel(channel);
        record.setTemplateCode(config.getTemplateCode());
        record.setReceiverUserIds(joinReceiverIds(receiverIds));
        record.setSendStatus(0);
        record.setRetryCount(0);
        return record;
    }

    private boolean claimRecord(FlowOverdueReminderRecord record) {
        try {
            reminderRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            FlowOverdueReminderRecord existing = reminderRecordMapper.selectByUniqueKey(
                    record.getTenantId(), record.getReminderKey(), record.getChannel());
            int claimed = reminderRecordMapper.claimRetry(record.getTenantId(), record.getReminderKey(),
                    record.getChannel(), LocalDateTime.now());
            if (claimed > 0 && existing != null) {
                record.setId(existing.getId());
                record.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1);
                log.debug("流程任务逾期提醒失败记录进入重试: reminderKey={}, channel={}",
                        record.getReminderKey(), record.getChannel());
                return true;
            }
            log.debug("流程任务逾期提醒记录已存在且当前不可重试: reminderKey={}, channel={}",
                    record.getReminderKey(), record.getChannel());
            return false;
        }
    }

    private void sendMessage(FlowTask task, FlowOverdueReminderConfig config, String channel,
                             String reminderKey, Set<Long> receiverIds, FlowOverdueReminderRecord record) {
        try {
            MessageSendRequestDTO request = new MessageSendRequestDTO();
            request.setTemplateCode(config.getTemplateCode());
            request.setType(MESSAGE_TYPE);
            request.setChannel(channel);
            request.setSendScope(SEND_SCOPE_USERS);
            request.setUserIds(receiverIds);
            request.setParams(buildMessageParams(task));

            SysMessage message = messageService.sendIfAbsent(request, BIZ_TYPE, reminderKey + ":" + channel);
            markSuccess(record, message != null ? message.getId() : null);
            log.info("流程任务逾期提醒已发送: taskId={}, reminderKey={}, channel={}, receivers={}",
                    task.getTaskId(), reminderKey, channel, receiverIds);
        } catch (Exception e) {
            markFailed(record, e.getMessage());
            log.warn("流程任务逾期提醒发送失败: taskId={}, reminderKey={}, channel={}",
                    task.getTaskId(), reminderKey, channel, e);
        }
    }

    private Map<String, Object> buildMessageParams(FlowTask task) {
        LocalDateTime now = LocalDateTime.now();
        long overdueMinutes = Math.max(0, Duration.between(task.getDueDate(), now).toMinutes());
        return Map.of(
                "taskId", safeText(task.getTaskId(), ""),
                "taskName", safeText(task.getTaskName(), task.getTitle()),
                "taskTitle", safeText(task.getTitle(), task.getTaskName()),
                "processName", safeText(task.getProcessName(), task.getProcessDefKey()),
                "processInstanceId", safeText(task.getProcessInstanceId(), ""),
                "startUserName", safeText(task.getStartUserName(), ""),
                "dueDate", task.getDueDate().format(DATE_TIME_FORMATTER),
                "overdueMinutes", overdueMinutes,
                "jumpUrl", JUMP_URL_PREFIX + task.getTaskId()
        );
    }

    private void markSuccess(FlowOverdueReminderRecord record, Long messageId) {
        record.setMessageId(messageId);
        record.setSendStatus(1);
        record.setSendTime(LocalDateTime.now());
        record.setErrorMessage(null);
        reminderRecordMapper.updateById(record);
    }

    private void markFailed(FlowOverdueReminderRecord record, String errorMessage) {
        record.setSendStatus(2);
        record.setSendTime(LocalDateTime.now());
        int retryCount = record.getRetryCount() == null ? 0 : record.getRetryCount();
        if (retryCount < MAX_RETRY_COUNT) {
            long backoffMinutes = RETRY_BASE_MINUTES * (1L << Math.min(retryCount, 4));
            record.setNextRetryTime(LocalDateTime.now().plusMinutes(backoffMinutes));
        } else {
            record.setNextRetryTime(null);
        }
        record.setErrorMessage(truncate(errorMessage, 1000));
        reminderRecordMapper.updateById(record);
    }

    private void executeWithTenant(Long tenantId, Runnable runnable) {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(false);
            TenantContextHolder.executeWithTenant(tenantId, runnable);
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private Long resolveTenantId(FlowTask task) {
        return task == null ? null : task.getTenantId();
    }

    private List<String> normalizeChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        return channels.stream()
                .filter(channel -> !isBlank(channel))
                .map(channel -> channel.trim().toUpperCase())
                .distinct()
                .toList();
    }

    private String joinReceiverIds(Set<Long> receiverIds) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return "";
        }
        return receiverIds.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String safeText(String value, String fallback) {
        return isBlank(value) ? (fallback == null ? "" : fallback) : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
