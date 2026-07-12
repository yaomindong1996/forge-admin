-- Forge AI 中枢阶段 2.2 修复：Flow 服务最终办理幂等凭证。
-- 凭证与 sys_flow_task 状态在 Flow 服务同一事务内提交，用于跨服务失败后的安全重放与回填。

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND COLUMN_NAME = 'action_idempotency_key'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sys_flow_task ADD COLUMN action_idempotency_key varchar(128) DEFAULT NULL COMMENT ''受控流程动作幂等键'' AFTER complete_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND COLUMN_NAME = 'action_request_digest'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sys_flow_task ADD COLUMN action_request_digest varchar(71) DEFAULT NULL COMMENT ''受控流程动作规范请求SHA-256摘要'' AFTER action_idempotency_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND COLUMN_NAME = 'action_type'
);
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sys_flow_task ADD COLUMN action_type varchar(16) DEFAULT NULL COMMENT ''受控流程动作类型APPROVE/REJECT'' AFTER action_request_digest',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND INDEX_NAME = 'idx_flow_task_action_idempotency'
);
SET @sql = IF(@index_exists = 0,
    'ALTER TABLE sys_flow_task ADD INDEX idx_flow_task_action_idempotency (tenant_id, action_idempotency_key, action_type)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
