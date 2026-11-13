-- 动态加签/减签请求幂等凭证。关系表已有 task + candidate 唯一键，
-- 通过任务行 FOR UPDATE 串行化同一任务的关系变更，避免重复推进 Flowable 候选集合。
SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL COMMENT ''动态加签/减签幂等键'' AFTER `reason`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'idempotency_key'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `request_digest` varchar(71) DEFAULT NULL COMMENT ''动态加签/减签规范请求摘要'' AFTER `idempotency_key`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'request_digest'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'CREATE INDEX `idx_flow_task_candidate_idempotency` ON `sys_flow_task_candidate` (`tenant_id`, `task_id`, `candidate_type`, `idempotency_key`)',
            'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND index_name = 'idx_flow_task_candidate_idempotency'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
