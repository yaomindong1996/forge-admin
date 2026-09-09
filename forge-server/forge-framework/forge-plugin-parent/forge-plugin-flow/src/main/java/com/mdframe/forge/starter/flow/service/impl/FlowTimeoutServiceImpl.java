package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowNodeConfigService;
import com.mdframe.forge.starter.flow.service.FlowTimeoutService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * 流程超时处理服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTimeoutServiceImpl implements FlowTimeoutService {

    private static final int TIMEOUT_SCAN_BATCH = 100;
    private static final String TIMEOUT_SCAN_LOCK_KEY = "forge:flow:timeout:scan:lock";
    private static final Duration TIMEOUT_SCAN_LOCK_TTL = Duration.ofMinutes(4);
    private static final DefaultRedisScript<Long> RELEASE_SCAN_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    private static final DefaultRedisScript<Long> RENEW_SCAN_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end", Long.class);

    @Value("${forge.flow.timeout.time-zone:Asia/Shanghai}")
    private String timeoutTimeZone;

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final FlowNodeConfigService flowNodeConfigService;
    private final FlowTaskMapper flowTaskMapper;

    /** Redis 不可用时仍保证单 JVM 内不重入；跨实例协调优先使用 Redis。 */
    private final ReentrantLock localScanLock = new ReentrantLock();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 定时检查超时任务（每5分钟执行一次）
     */
    @Override
    @Scheduled(fixedRateString = "${forge.flow.timeout.scan-interval-ms:300000}")
    @IgnoreTenant
    public void checkAndHandleTimeoutTasks() {
        log.debug("开始检查超时任务...");
        
        TenantContextHolder.executeIgnore(() -> {
            String lockToken = UUID.randomUUID().toString();
            ScanLockLease lease = tryAcquireScanLock(lockToken);
            if (lease == null) {
                log.debug("已有流程超时扫描实例执行中，本轮跳过");
                return;
            }
            try {
                LocalDateTime scanTime = LocalDateTime.now();
                backfillMissingDueDates(scanTime);
                int checked = 0;
                LocalDateTime cursorDueDate = null;
                String cursorId = null;
                while (true) {
                    List<FlowTask> batch = flowTaskMapper.selectTimeoutCandidates(
                            scanTime, cursorDueDate, cursorId, TIMEOUT_SCAN_BATCH);
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }
                    if (!renewScanLock(lease)) {
                        log.warn("流程超时扫描锁租期续期失败，本轮提前结束");
                        break;
                    }
                    for (FlowTask localTask : batch) {
                        try {
                            if (localTask == null || localTask.getTaskId() == null
                                    || localTask.getTenantId() == null || localTask.getTenantId() <= 0) {
                                continue;
                            }
                            TenantContextHolder.executeWithTenant(localTask.getTenantId(), () -> {
                                Task runtimeTask = taskService.createTaskQuery()
                                        .taskId(localTask.getTaskId()).active().singleResult();
                                if (runtimeTask != null) {
                                    checkTaskTimeout(runtimeTask);
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            log.error("检查任务超时失败: taskId={}, tenantId={}, error={}",
                                    localTask == null ? null : localTask.getTaskId(),
                                    localTask == null ? null : localTask.getTenantId(), e.getMessage());
                        }
                    }
                    checked += batch.size();
                    FlowTask last = batch.get(batch.size() - 1);
                    cursorDueDate = last.getDueDate();
                    cursorId = last.getId();
                    if (batch.size() < TIMEOUT_SCAN_BATCH) {
                        break;
                    }
                }
                log.debug("超时任务检查完成，共检查 {} 个任务", checked);
            } catch (Exception e) {
                log.error("检查超时任务异常", e);
            } finally {
                releaseScanLock(lease);
            }
        });
        
    }

    private ScanLockLease tryAcquireScanLock(String lockToken) {
        if (!localScanLock.tryLock()) {
            return null;
        }
        if (stringRedisTemplate == null) {
            return new ScanLockLease(lockToken, false);
        }
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(TIMEOUT_SCAN_LOCK_KEY, lockToken, TIMEOUT_SCAN_LOCK_TTL);
            if (!Boolean.TRUE.equals(acquired)) {
                localScanLock.unlock();
                return null;
            }
            return new ScanLockLease(lockToken, true);
        } catch (Exception e) {
            // Redis 属于可选基础设施，降级到本 JVM 锁并保留可观测告警，避免完全停止超时处理。
            log.warn("获取流程超时分布式锁失败，降级为单 JVM 锁: {}", e.getMessage());
            return new ScanLockLease(lockToken, false);
        }
    }

    private boolean renewScanLock(ScanLockLease lease) {
        if (lease == null || !lease.redisAcquired() || stringRedisTemplate == null) {
            return true;
        }
        try {
            Long renewed = stringRedisTemplate.execute(
                    RENEW_SCAN_LOCK_SCRIPT,
                    Collections.singletonList(TIMEOUT_SCAN_LOCK_KEY),
                    lease.token(), String.valueOf(TIMEOUT_SCAN_LOCK_TTL.toMillis()));
            return Long.valueOf(1L).equals(renewed);
        } catch (Exception e) {
            log.warn("续期流程超时分布式锁失败: {}", e.getMessage());
            return false;
        }
    }

    private void releaseScanLock(ScanLockLease lease) {
        try {
            if (lease != null && lease.redisAcquired() && stringRedisTemplate != null) {
                stringRedisTemplate.execute(RELEASE_SCAN_LOCK_SCRIPT,
                        Collections.singletonList(TIMEOUT_SCAN_LOCK_KEY), lease.token());
            }
        } catch (Exception e) {
            log.warn("释放流程超时分布式锁失败: {}", e.getMessage());
        } finally {
            if (localScanLock.isHeldByCurrentThread()) {
                localScanLock.unlock();
            }
        }
    }

    private record ScanLockLease(String token, boolean redisAcquired) {
    }

    private void backfillMissingDueDates(LocalDateTime scanTime) {
        LocalDateTime cursorCreateTime = null;
        String cursorId = null;
        int backfilled = 0;
        int scanned = 0;
        while (scanned < TIMEOUT_SCAN_BATCH) {
            int limit = Math.min(TIMEOUT_SCAN_BATCH - scanned, TIMEOUT_SCAN_BATCH);
            List<FlowTask> candidates = flowTaskMapper.selectDueDateBackfillCandidates(
                    scanTime, cursorCreateTime, cursorId, limit);
            if (candidates == null || candidates.isEmpty()) {
                break;
            }
            for (FlowTask localTask : candidates) {
                if (localTask == null || localTask.getTaskId() == null
                        || localTask.getTenantId() == null || localTask.getTenantId() <= 0) {
                    continue;
                }
                try {
                    TenantContextHolder.executeWithTenant(localTask.getTenantId(), () -> {
                        Task runtimeTask = taskService.createTaskQuery()
                                .taskId(localTask.getTaskId()).active().singleResult();
                        if (runtimeTask == null) {
                            return null;
                        }
                        LocalDateTime dueDate = resolveDueDate(runtimeTask);
                        if (dueDate != null) {
                            taskService.setDueDate(runtimeTask.getId(),
                                    Date.from(dueDate.atZone(resolveTimeoutZone()).toInstant()));
                            flowTaskMapper.updateDueDateByTaskIdAndTenant(
                                    runtimeTask.getId(), localTask.getTenantId(), dueDate);
                            if (!dueDate.isAfter(scanTime)) {
                                checkTaskTimeout(runtimeTask);
                            }
                        }
                        return null;
                    });
                    backfilled++;
                } catch (Exception e) {
                    log.warn("回填流程任务截止时间失败: taskId={}, tenantId={}, error={}",
                            localTask.getTaskId(), localTask.getTenantId(), e.getMessage());
                }
            }
            FlowTask last = candidates.get(candidates.size() - 1);
            cursorCreateTime = last.getCreateTime();
            cursorId = last.getId();
            scanned += candidates.size();
            if (candidates.size() < limit) {
                break;
            }
        }
        if (backfilled > 0) {
            log.info("流程任务截止时间回填完成: count={}", backfilled);
        }
    }

    private LocalDateTime resolveDueDate(Task task) {
        if (task == null || task.getCreateTime() == null) {
            return null;
        }
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
        if (processInstance == null) {
            return null;
        }
        FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                processInstance.getProcessDefinitionKey(), task.getTaskDefinitionKey());
        if (nodeConfig == null) {
            return null;
        }
        Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(nodeConfig.getId());
        if (timeoutMillis == null || timeoutMillis <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(task.getCreateTime().toInstant(), resolveTimeoutZone())
                .plusNanos(timeoutMillis * 1_000_000L);
    }

    private ZoneId resolveTimeoutZone() {
        try {
            return ZoneId.of(timeoutTimeZone);
        } catch (Exception e) {
            log.warn("流程超时配置时区非法，回退 Asia/Shanghai: {}", timeoutTimeZone);
            return ZoneId.of("Asia/Shanghai");
        }
    }

    /**
     * 检查单个任务是否超时
     */
    private void checkTaskTimeout(Task task) {
        // 获取流程定义ID和节点ID
        String processDefinitionId = task.getProcessDefinitionId();
        String taskDefinitionKey = task.getTaskDefinitionKey();
        
        // 获取流程实例以获取模型ID
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        
        if (processInstance == null) {
            return;
        }
        
        // 从流程定义ID中提取模型Key
        String processKey = processDefinitionId.split(":")[0];
        
        // 获取节点配置
        FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                processInstance.getProcessDefinitionKey(), taskDefinitionKey);
        
        if (nodeConfig == null || nodeConfig.getTimeoutAction() == null
                || "none".equals(nodeConfig.getTimeoutAction())) {
            return;
        }
        
        // 计算超时时间
        Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(nodeConfig.getId());
        if (timeoutMillis == null || timeoutMillis <= 0) {
            return;
        }
        
        // 获取任务创建时间
        Date createTime = task.getCreateTime();
        if (createTime == null) {
            return;
        }
        
        // 计算是否超时
        long elapsed = System.currentTimeMillis() - createTime.getTime();
        if (elapsed >= timeoutMillis) {
            log.info("任务已超时: taskId={}, taskName={}, elapsed={}ms, timeout={}ms",
                    task.getId(), task.getName(), elapsed, timeoutMillis);
            handleTimeoutTask(task.getId(), nodeConfig.getTimeoutAction());
        }
    }

    @Override
    public boolean handleTimeoutTask(String taskId, String timeoutAction) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        try {
            switch (timeoutAction) {
                case "auto_pass":
                    // 自动通过
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("timeout_auto_pass", true);
                    variables.put("timeout_action", "auto_pass");
                    taskService.complete(taskId, variables);
                    log.info("超时任务自动通过: taskId={}", taskId);
                    break;
                    
                case "auto_reject":
                    // 自动拒绝
                    Map<String, Object> rejectVariables = new HashMap<>();
                    rejectVariables.put("timeout_auto_reject", true);
                    rejectVariables.put("timeout_action", "auto_reject");
                    taskService.complete(taskId, rejectVariables);
                    log.info("超时任务自动拒绝: taskId={}", taskId);
                    break;
                    
                case "notify":
                    if (!sendTimeoutNotification(taskId, "system")) {
                        log.warn("超时通知渠道尚不可用: taskId={}", taskId);
                        return false;
                    }
                    break;
                    
                default:
                    log.warn("未知的超时动作: {}", timeoutAction);
                    return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理超时任务失败: taskId={}, action={}", taskId, timeoutAction, e);
            return false;
        }
    }

    @Override
    public boolean sendTimeoutNotification(String taskId, String notifyType) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return false;
        }
        
        String assignee = task.getAssignee();
        if (assignee == null || assignee.isEmpty()) {
            // 如果没有指定办理人，获取候选人
            List<String> candidates = getCandidateUsers(taskId);
            if (candidates.isEmpty()) {
                log.warn("任务没有办理人或候选人: {}", taskId);
                return false;
            }
            assignee = String.join(",", candidates);
        }
        
        if (!Set.of("email", "sms", "system").contains(notifyType)) {
            log.warn("未知的通知类型: {}", notifyType);
            return false;
        }
        log.warn("流程超时通知渠道尚未接入，拒绝返回伪成功: taskId={}, notifyType={}, recipientCount={}",
                taskId, notifyType, assignee.split(",").length);
        return false;
    }

    @Override
    public List<String> getUpcomingTimeoutTasks(int advanceMinutes) {
        List<String> upcomingTaskIds = new ArrayList<>();
        int safeAdvanceMinutes = Math.max(1, Math.min(advanceMinutes, 7 * 24 * 60));
        Date now = new Date();
        Date deadline = new Date(now.getTime() + safeAdvanceMinutes * 60L * 1000);
        int first = 0;
        while (true) {
            List<Task> batch = taskService.createTaskQuery()
                    .active()
                    .taskDueAfter(now)
                    .taskDueBefore(deadline)
                    .orderByTaskDueDate()
                    .asc()
                    .listPage(first, TIMEOUT_SCAN_BATCH);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (Task task : batch) {
                upcomingTaskIds.add(task.getId());
            }
            if (batch.size() < TIMEOUT_SCAN_BATCH) {
                break;
            }
            first += TIMEOUT_SCAN_BATCH;
        }
        return upcomingTaskIds;
    }

    @Override
    public Long getRemainingTime(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }
        
        // 获取流程实例
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();
        
        if (processInstance == null) {
            return null;
        }
        
        // 获取节点配置
        FlowNodeConfig nodeConfig = flowNodeConfigService.getByModelAndNode(
                processInstance.getProcessDefinitionKey(), task.getTaskDefinitionKey());
        
        if (nodeConfig == null) {
            return null;
        }
        
        Long timeoutMillis = flowNodeConfigService.getTimeoutMillis(nodeConfig.getId());
        if (timeoutMillis == null || timeoutMillis <= 0) {
            return null;
        }
        
        Date createTime = task.getCreateTime();
        if (createTime == null) {
            return null;
        }
        
        long elapsed = System.currentTimeMillis() - createTime.getTime();
        return timeoutMillis - elapsed;
    }

    /**
     * 获取任务的候选人列表
     */
    private List<String> getCandidateUsers(String taskId) {
        // 获取候选用户
        List<String> candidateUsers = taskService.getIdentityLinksForTask(taskId).stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getUserId() != null)
                .map(link -> link.getUserId())
                .collect(Collectors.toList());
        
        return candidateUsers;
    }
}
