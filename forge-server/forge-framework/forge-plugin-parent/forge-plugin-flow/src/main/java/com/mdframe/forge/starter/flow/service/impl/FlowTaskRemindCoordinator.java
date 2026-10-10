package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.plugin.message.domain.dto.MessageSendRequestDTO;
import com.mdframe.forge.plugin.message.service.MessageService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.security.FlowAccessGuard;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 任务催办：校验可见性与签收状态，按"同一人 + 同一任务"限频后给当前办理人发站内信。
 *
 * <p>限频依赖 Redis；Redis 不可用时放行（催办不改变流程状态，宁可多发也不阻断）。
 * 消息发送失败不释放限频 key，避免失败后被连续重试刷屏。</p>
 */
@Slf4j
final class FlowTaskRemindCoordinator {

    static final Duration REMIND_INTERVAL = Duration.ofMinutes(10);
    static final String REMIND_KEY_PREFIX = "forge:flow:remind:";

    private final TaskService taskService;
    private final FlowAccessGuard flowAccessGuard;
    private final MessageService messageService;
    private final StringRedisTemplate stringRedisTemplate;
    private final Supplier<Long> tenantIdSupplier;
    private final Supplier<Long> userIdSupplier;

    FlowTaskRemindCoordinator(TaskService taskService,
                              FlowAccessGuard flowAccessGuard,
                              MessageService messageService,
                              StringRedisTemplate stringRedisTemplate,
                              Supplier<Long> tenantIdSupplier,
                              Supplier<Long> userIdSupplier) {
        this.taskService = taskService;
        this.flowAccessGuard = flowAccessGuard;
        this.messageService = messageService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tenantIdSupplier = tenantIdSupplier;
        this.userIdSupplier = userIdSupplier;
    }

    void remind(String taskId) {
        flowAccessGuard.requireTaskVisible(taskId);
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException("任务不存在或已处理");
        }
        Long assigneeId = parseAssignee(task.getAssignee());
        if (assigneeId == null) {
            throw new BusinessException("任务尚未被签收，暂时无法催办");
        }
        if (!acquireRemindSlot(taskId)) {
            throw new BusinessException("已催办过，请 10 分钟后再试");
        }
        sendRemindMessage(task, assigneeId);
    }

    static String remindKey(Long tenantId, String taskId, Long userId) {
        return REMIND_KEY_PREFIX + tenantId + ":" + taskId + ":" + userId;
    }

    private Long parseAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(assignee.trim());
        } catch (NumberFormatException e) {
            log.warn("催办跳过：办理人ID无法解析，assignee={}", assignee);
            return null;
        }
    }

    private boolean acquireRemindSlot(String taskId) {
        if (stringRedisTemplate == null) {
            log.warn("Redis 未启用，催办不限频：taskId={}", taskId);
            return true;
        }
        String key = remindKey(tenantIdSupplier.get(), taskId, userIdSupplier.get());
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", REMIND_INTERVAL);
            return !Boolean.FALSE.equals(acquired);
        } catch (RuntimeException e) {
            log.warn("催办限频不可用，已放行：taskId={}, reason={}", taskId, e.getMessage());
            return true;
        }
    }

    private void sendRemindMessage(Task task, Long assigneeId) {
        if (messageService == null) {
            log.warn("消息服务未启用，无法发送催办通知：taskId={}", task.getId());
            return;
        }
        MessageSendRequestDTO request = new MessageSendRequestDTO();
        request.setTitle("流程催办提醒");
        request.setContent(String.format("您有一个待办任务需要处理：%s，请及时处理。", task.getName()));
        request.setType("SYSTEM");
        request.setChannel("WEB");
        request.setSendScope("USERS");
        request.setUserIds(new HashSet<>(Set.of(assigneeId)));
        try {
            messageService.send(request);
            log.info("催办消息发送成功：taskId={}, assignee={}", task.getId(), assigneeId);
        } catch (RuntimeException e) {
            log.error("发送催办消息失败：taskId={}", task.getId(), e);
        }
    }
}
