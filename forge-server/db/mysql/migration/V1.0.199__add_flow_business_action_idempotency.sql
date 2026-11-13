-- 为流程实例级写动作增加最终幂等结果；首批用于发起人撤回响应丢失后的安全回放。
-- 回滚：确认无撤回请求处于结果未知状态后，删除索引及三个 action_* 字段。

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_business'
      AND COLUMN_NAME = 'action_idempotency_key'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD COLUMN `action_idempotency_key` varchar(128) DEFAULT NULL COMMENT ''受控流程实例动作幂等键'' AFTER `projection_sequence`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_business'
      AND COLUMN_NAME = 'action_request_digest'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD COLUMN `action_request_digest` varchar(71) DEFAULT NULL COMMENT ''受控流程实例动作规范请求SHA-256摘要'' AFTER `action_idempotency_key`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_business'
      AND COLUMN_NAME = 'action_type'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD COLUMN `action_type` varchar(16) DEFAULT NULL COMMENT ''受控流程实例动作类型'' AFTER `action_request_digest`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_business'
      AND INDEX_NAME = 'idx_flow_business_action_idempotency'
);
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE `sys_flow_business` ADD INDEX `idx_flow_business_action_idempotency` (`tenant_id`, `action_idempotency_key`, `action_type`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
