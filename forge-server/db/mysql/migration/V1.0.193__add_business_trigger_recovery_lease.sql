-- 为业务触发器执行补齐不可变命令快照、恢复租约、退避和死信所需字段。
-- 历史 PENDING 记录没有完整事件/触发器快照，不能安全自动重放，迁移为 TODO 供人工核对。
-- 回滚：先停用 forge.business.trigger-recovery.enabled，再删除恢复索引和新增列；TODO 历史记录保留审计状态。

SET @trigger_snapshot_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND COLUMN_NAME = 'trigger_snapshot'
);
SET @sql = IF(@trigger_snapshot_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `trigger_snapshot` json DEFAULT NULL COMMENT ''触发器配置不可变快照'' AFTER `event_data`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @execution_digest_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND COLUMN_NAME = 'execution_digest'
);
SET @sql = IF(@execution_digest_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `execution_digest` char(64) DEFAULT NULL COMMENT ''事件与触发器快照联合摘要'' AFTER `trigger_snapshot`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @next_retry_time_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND COLUMN_NAME = 'next_retry_time'
);
SET @sql = IF(@next_retry_time_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `next_retry_time` datetime DEFAULT NULL COMMENT ''下次自动恢复时间'' AFTER `retry_count`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @lock_owner_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND COLUMN_NAME = 'lock_owner'
);
SET @sql = IF(@lock_owner_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `lock_owner` varchar(128) DEFAULT NULL COMMENT ''执行租约持有者'' AFTER `next_retry_time`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @lock_time_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND COLUMN_NAME = 'lock_time'
);
SET @sql = IF(@lock_time_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `lock_time` datetime DEFAULT NULL COMMENT ''执行租约获取时间'' AFTER `lock_owner`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ai_business_trigger_log
SET execute_status = 'TODO',
    error_message = COALESCE(NULLIF(error_message, ''), '历史执行记录缺少完整命令快照，禁止自动重放')
WHERE execute_status = 'PENDING'
  AND (trigger_snapshot IS NULL OR execution_digest IS NULL);

SET @recovery_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND INDEX_NAME = 'idx_ai_trigger_log_recovery'
);
SET @sql = IF(@recovery_idx = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD INDEX `idx_ai_trigger_log_recovery` (`execute_status`, `next_retry_time`, `lock_time`, `id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_dict_data
SET dict_label = '待人工处理',
    remark = '自动恢复不安全或命令快照无效，等待人工核对',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND dict_type = 'ai_business_trigger_execute_status'
  AND dict_value = 'TODO'
  AND dict_label = '待执行'
  AND remark = '等待执行';

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type,
    css_class, list_class, is_default, dict_status, remark,
    create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, 'N', 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
    SELECT 1 tenant_id, 0 dict_sort, '执行中' dict_label, 'PENDING' dict_value,
           'ai_business_trigger_execute_status' dict_type, 'info' list_class,
           '已认领或等待恢复的执行命令' remark
    UNION ALL
    SELECT 1, 6, '重试耗尽', 'DEAD', 'ai_business_trigger_execute_status', 'error',
           '自动重试达到上限，等待人工处理'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_dict_data data
    WHERE data.tenant_id = seed.tenant_id
      AND data.dict_type = seed.dict_type
      AND data.dict_value = seed.dict_value
);
