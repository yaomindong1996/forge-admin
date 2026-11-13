-- 为流程通知死信人工重放补充审计字段。
-- 回滚：停用人工重放接口后删除以下四列；已重放事件的投递结果仍保留在 Outbox 主记录中。

SET @replay_count_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_notify_outbox'
      AND COLUMN_NAME = 'replay_count'
);
SET @sql = IF(@replay_count_col = 0,
    'ALTER TABLE `sys_flow_notify_outbox` ADD COLUMN `replay_count` int NOT NULL DEFAULT 0 COMMENT ''人工重放次数'' AFTER `last_error`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @replayed_by_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_notify_outbox'
      AND COLUMN_NAME = 'replayed_by'
);
SET @sql = IF(@replayed_by_col = 0,
    'ALTER TABLE `sys_flow_notify_outbox` ADD COLUMN `replayed_by` varchar(64) DEFAULT NULL COMMENT ''最近人工重放操作人'' AFTER `replay_count`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @replayed_time_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_notify_outbox'
      AND COLUMN_NAME = 'replayed_time'
);
SET @sql = IF(@replayed_time_col = 0,
    'ALTER TABLE `sys_flow_notify_outbox` ADD COLUMN `replayed_time` datetime DEFAULT NULL COMMENT ''最近人工重放时间'' AFTER `replayed_by`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @replay_reason_col = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_notify_outbox'
      AND COLUMN_NAME = 'replay_reason'
);
SET @sql = IF(@replay_reason_col = 0,
    'ALTER TABLE `sys_flow_notify_outbox` ADD COLUMN `replay_reason` varchar(500) DEFAULT NULL COMMENT ''最近人工重放原因'' AFTER `replayed_time`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
