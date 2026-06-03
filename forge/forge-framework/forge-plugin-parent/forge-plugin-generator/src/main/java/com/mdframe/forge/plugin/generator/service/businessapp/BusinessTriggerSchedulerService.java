package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.starter.job.annotation.ScheduledJob;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 业务定时触发器扫描任务。
 * <p>
 * 注册到系统任务调度中心，使用一个平台任务控制频率和批量，不为每条业务触发器创建秒级任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.business-trigger.schedule", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class BusinessTriggerSchedulerService {

    private static final String SCAN_LOCK_KEY = "forge:business-trigger:schedule:scan";
    private static final String RECORD_LOCK_PREFIX = "forge:business-trigger:schedule:record:";
    private static final int DEFAULT_TRIGGER_LIMIT = 100;
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int MAX_BATCH_SIZE = 200;
    private static final int DEFAULT_MIN_INTERVAL_MINUTES = 5;

    private final BusinessTriggerService triggerService;
    private final BusinessTriggerExecutor triggerExecutor;
    private final DynamicCrudService dynamicCrudService;
    private final BusinessAppMapper businessAppMapper;
    private final ObjectProvider<RedissonClient> redissonClientProvider;

    private volatile boolean localLockWarningLogged;

    @Value("${forge.business-trigger.schedule.max-triggers-per-run:100}")
    private Integer maxTriggersPerRun;

    @Value("${forge.business-trigger.schedule.cluster-lock-enabled:true}")
    private Boolean clusterLockEnabled;

    @Value("${forge.business-trigger.schedule.lock-wait-ms:0}")
    private Long lockWaitMs;

    @ScheduledJob(
            name = "lowcodeBusinessTriggerScanJob",
            group = "LOWCODE",
            cron = "0 0/5 * * * ?",
            description = "低代码定时触发器扫描任务，默认每5分钟执行一次"
    )
    public String scanScheduledTriggers() {
        if (!Boolean.TRUE.equals(clusterLockEnabled)) {
            doScanScheduledTriggers();
            return "SUCCESS";
        }

        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            logLocalLockWarning();
            doScanScheduledTriggers();
            return "SUCCESS";
        }

        RLock lock = redissonClient.getLock(SCAN_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(normalizeLockWaitMs(), TimeUnit.MILLISECONDS);
            if (!locked) {
                log.debug("[定时触发] 集群扫描锁已被其他节点持有，本轮跳过");
                return "SKIPPED_LOCKED";
            }
            doScanScheduledTriggers();
            return "SUCCESS";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[定时触发] 获取集群扫描锁被中断，本轮跳过");
            return "INTERRUPTED";
        } catch (Exception e) {
            log.warn("[定时触发] 获取或执行集群扫描锁失败，本轮跳过: {}", e.getMessage());
            return "FAILED: " + e.getMessage();
        } finally {
            unlockSafely(lock, locked, SCAN_LOCK_KEY);
        }
    }

    private void doScanScheduledTriggers() {
        List<AiBusinessTrigger> triggers = triggerService.selectActiveScheduleTriggers(
                maxTriggersPerRun == null ? DEFAULT_TRIGGER_LIMIT : maxTriggersPerRun);
        if (triggers == null || triggers.isEmpty()) {
            return;
        }
        for (AiBusinessTrigger trigger : triggers) {
            try {
                scanSingleTrigger(trigger);
            } catch (Exception e) {
                log.warn("[定时触发] 扫描触发器失败, triggerId={}, triggerName={}: {}",
                        trigger == null ? null : trigger.getId(),
                        trigger == null ? null : trigger.getTriggerName(),
                        e.getMessage());
            }
        }
    }

    private void scanSingleTrigger(AiBusinessTrigger trigger) {
        if (trigger == null || trigger.getTenantId() == null) {
            return;
        }
        ScheduledTriggerConfig config = readScheduleConfig(trigger);
        if (StringUtils.isBlank(config.dueField())) {
            log.warn("[定时触发] 跳过未配置到期字段的触发器, triggerId={}, triggerName={}",
                    trigger.getId(), trigger.getTriggerName());
            triggerService.touchScheduleScanTime(trigger.getTenantId(), trigger.getId());
            return;
        }
        if (trigger.getLastExecuteTime() != null
                && trigger.getLastExecuteTime().isAfter(LocalDateTime.now().minusMinutes(config.minIntervalMinutes()))) {
            log.debug("[定时触发] 跳过未到最小扫描间隔的触发器, triggerId={}, minIntervalMinutes={}",
                    trigger.getId(), config.minIntervalMinutes());
            return;
        }

        try {
            TenantContextHolder.executeWithTenant(trigger.getTenantId(), () -> scanWithTenant(trigger, config));
        } finally {
            triggerService.touchScheduleScanTime(trigger.getTenantId(), trigger.getId());
        }
    }

    private void scanWithTenant(AiBusinessTrigger trigger, ScheduledTriggerConfig config) {
        String configKey = resolveConfigKey(trigger, config);
        if (StringUtils.isBlank(configKey)) {
            log.warn("[定时触发] 跳过缺少运行配置的触发器, triggerId={}, objectCode={}",
                    trigger.getId(), trigger.getObjectCode());
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime windowStart = today.minusDays(config.lookBehindDays()).atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(config.lookAheadDays()).plusDays(1).atStartOfDay().minusNanos(1);
        List<Map<String, Object>> rows = dynamicCrudService.selectScheduledCandidateRows(
                configKey, config.dueField(), windowStart, windowEnd, config.batchSize());
        if (rows == null || rows.isEmpty()) {
            return;
        }

        LocalDateTime dedupeSince = today.atStartOfDay();
        for (Map<String, Object> row : rows) {
            String recordId = readRecordId(row);
            if (StringUtils.isBlank(recordId)) {
                continue;
            }
            if (triggerService.hasSuccessOrTodoLogSince(trigger.getTenantId(), trigger.getId(), recordId,
                    BusinessEvent.SCHEDULED_DUE, dedupeSince)) {
                continue;
            }
            BusinessEvent event = buildScheduledEvent(trigger, configKey, recordId, row);
            if (!triggerExecutor.matchesCondition(trigger, event)) {
                continue;
            }
            executeScheduledEvent(trigger, event, today);
        }
    }

    private void executeScheduledEvent(AiBusinessTrigger trigger, BusinessEvent event, LocalDate today) {
        if (!Boolean.TRUE.equals(clusterLockEnabled)) {
            triggerExecutor.executeTrigger(trigger, event);
            return;
        }
        RedissonClient redissonClient = redissonClientProvider.getIfAvailable();
        if (redissonClient == null) {
            triggerExecutor.executeTrigger(trigger, event);
            return;
        }
        String lockKey = buildRecordLockKey(trigger, event, today);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(normalizeLockWaitMs(), TimeUnit.MILLISECONDS);
            if (!locked) {
                log.debug("[定时触发] 到期记录执行锁已被占用, triggerId={}, recordId={}",
                        trigger.getId(), event.getRecordId());
                return;
            }
            triggerExecutor.executeTrigger(trigger, event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[定时触发] 获取到期记录执行锁被中断, triggerId={}, recordId={}",
                    trigger.getId(), event.getRecordId());
        } catch (Exception e) {
            log.warn("[定时触发] 到期记录执行失败, triggerId={}, recordId={}: {}",
                    trigger.getId(), event.getRecordId(), e.getMessage());
        } finally {
            unlockSafely(lock, locked, lockKey);
        }
    }

    private String buildRecordLockKey(AiBusinessTrigger trigger, BusinessEvent event, LocalDate today) {
        return RECORD_LOCK_PREFIX
                + safeLockToken(trigger.getTenantId()) + ":"
                + safeLockToken(trigger.getId()) + ":"
                + safeLockToken(event.getRecordId()) + ":"
                + safeLockToken(today);
    }

    private String safeLockToken(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value).replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private long normalizeLockWaitMs() {
        if (lockWaitMs == null) {
            return 0L;
        }
        return Math.max(lockWaitMs, 0L);
    }

    private void unlockSafely(RLock lock, boolean locked, String lockKey) {
        if (!locked || lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("[定时触发] 释放分布式锁失败, lockKey={}: {}", lockKey, e.getMessage());
        }
    }

    private void logLocalLockWarning() {
        if (localLockWarningLogged) {
            return;
        }
        localLockWarningLogged = true;
        log.warn("[定时触发] 未检测到 RedissonClient，本轮仅依赖任务调度中心集群调度和日志去重；如需手动并发兜底请启用 Redis/Redisson");
    }

    private ScheduledTriggerConfig readScheduleConfig(AiBusinessTrigger trigger) {
        JSONObject condition = readJson(trigger.getEventCondition());
        JSONObject schedule = condition.getJSONObject("schedule");
        if (schedule == null) {
            schedule = condition;
        }
        JSONObject actionConfig = readJson(trigger.getActionConfig());
        String dueField = StringUtils.firstNonBlank(
                schedule.getString("dueField"),
                schedule.getString("field"),
                schedule.getString("reminderField"),
                actionConfig.getString("dueField")
        );
        String configKey = StringUtils.firstNonBlank(schedule.getString("configKey"), actionConfig.getString("configKey"));
        int batchSize = clamp(readInt(schedule, "batchSize", DEFAULT_BATCH_SIZE), 1, MAX_BATCH_SIZE);
        int minIntervalMinutes = Math.max(readInt(schedule, "minIntervalMinutes", DEFAULT_MIN_INTERVAL_MINUTES),
                DEFAULT_MIN_INTERVAL_MINUTES);
        int lookAheadDays = clamp(readInt(schedule, "lookAheadDays", 0), 0, 365);
        int lookBehindDays = clamp(readInt(schedule, "lookBehindDays", 0), 0, 30);
        return new ScheduledTriggerConfig(dueField, configKey, lookAheadDays, lookBehindDays,
                batchSize, minIntervalMinutes);
    }

    private String resolveConfigKey(AiBusinessTrigger trigger, ScheduledTriggerConfig config) {
        if (StringUtils.isNotBlank(config.configKey())) {
            return config.configKey();
        }
        AiBusinessApp app = businessAppMapper.selectRuntimeAppByObject(
                trigger.getTenantId(), trigger.getSuiteCode(), trigger.getObjectCode());
        return app == null ? null : app.getConfigKey();
    }

    private BusinessEvent buildScheduledEvent(AiBusinessTrigger trigger, String configKey,
                                              String recordId, Map<String, Object> row) {
        Map<String, Object> eventData = new LinkedHashMap<>(row);
        eventData.put("scheduledTriggerId", trigger.getId());
        eventData.put("scheduledTriggerName", trigger.getTriggerName());
        return BusinessEvent.builder()
                .eventType(BusinessEvent.SCHEDULED_DUE)
                .suiteCode(trigger.getSuiteCode())
                .objectCode(trigger.getObjectCode())
                .configKey(configKey)
                .recordId(recordId)
                .recordData(eventData)
                .tenantId(trigger.getTenantId())
                .build();
    }

    private String readRecordId(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Object value = row.get("id");
        if (value == null) {
            value = row.get("Id");
        }
        return value == null ? null : String.valueOf(value);
    }

    private JSONObject readJson(String json) {
        if (StringUtils.isBlank(json)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private int readInt(JSONObject source, String key, int fallback) {
        if (source == null || StringUtils.isBlank(key)) {
            return fallback;
        }
        Integer value = source.getInteger(key);
        return value == null ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private record ScheduledTriggerConfig(String dueField,
                                          String configKey,
                                          int lookAheadDays,
                                          int lookBehindDays,
                                          int batchSize,
                                          int minIntervalMinutes) {
    }
}
