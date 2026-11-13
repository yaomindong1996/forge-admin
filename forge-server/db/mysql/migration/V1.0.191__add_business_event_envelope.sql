-- 为低代码业务事件补齐可信来源、协议版本、稳定事件 ID 和载荷摘要。
-- 触发器执行先写入 PENDING 认领记录，唯一索引保证同一事件的同一触发器最多执行一次。
-- 回滚：应用回滚后可保留新增列和索引；如必须删除，先确认没有依赖 event_id 的运行实例或审计记录。

SET @event_id_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log' AND COLUMN_NAME = 'event_id'
);
SET @sql = IF(@event_id_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `event_id` varchar(128) DEFAULT NULL COMMENT ''稳定业务事件ID'' AFTER `event_type`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @event_source_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log' AND COLUMN_NAME = 'event_source'
);
SET @sql = IF(@event_source_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `event_source` varchar(32) DEFAULT NULL COMMENT ''受信事件来源'' AFTER `event_id`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @event_version_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log' AND COLUMN_NAME = 'event_version'
);
SET @sql = IF(@event_version_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `event_version` int DEFAULT NULL COMMENT ''事件协议版本'' AFTER `event_source`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @event_digest_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log' AND COLUMN_NAME = 'event_digest'
);
SET @sql = IF(@event_digest_col = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD COLUMN `event_digest` char(64) DEFAULT NULL COMMENT ''事件载荷SHA-256摘要'' AFTER `event_version`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ai_business_trigger_log
SET event_id = CONCAT('LEGACY:', id),
    event_source = 'LEGACY',
    event_version = 0,
    event_digest = REPEAT('0', 64)
WHERE event_id IS NULL OR event_source IS NULL OR event_version IS NULL OR event_digest IS NULL;

SET @event_unique_idx = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_business_trigger_log'
      AND INDEX_NAME = 'uk_ai_business_trigger_log_event'
);
SET @sql = IF(@event_unique_idx = 0,
    'ALTER TABLE `ai_business_trigger_log` ADD UNIQUE INDEX `uk_ai_business_trigger_log_event` (`tenant_id`, `trigger_id`, `event_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE `ai_business_trigger_log` MODIFY COLUMN `event_id` varchar(128) NOT NULL COMMENT ''稳定业务事件ID'', MODIFY COLUMN `event_source` varchar(32) NOT NULL COMMENT ''受信事件来源'', MODIFY COLUMN `event_version` int NOT NULL COMMENT ''事件协议版本'', MODIFY COLUMN `event_digest` char(64) NOT NULL COMMENT ''事件载荷SHA-256摘要''';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
