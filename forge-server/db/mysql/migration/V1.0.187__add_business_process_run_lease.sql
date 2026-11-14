-- 为业务流程运行实例增加数据库租约、心跳和 fencing token。
-- 已存在的 RUNNING 记录 lease_expire_time 为空，会被第一个恢复执行器安全接管。
-- 回滚时应先停止所有业务流程执行器；保留这些列不会影响旧版本读取，禁止在运行中直接删列。

SET @process_run_execution_token_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_process_run'
    AND COLUMN_NAME = 'execution_token'
);
SET @add_process_run_execution_token_sql = IF(
  @process_run_execution_token_exists = 0,
  'ALTER TABLE `ai_business_process_run` ADD COLUMN `execution_token` bigint NOT NULL DEFAULT 0 COMMENT ''单调递增执行栅栏令牌'' AFTER `flow_process_instance_id`',
  'SELECT 1'
);
PREPARE add_process_run_execution_token_stmt FROM @add_process_run_execution_token_sql;
EXECUTE add_process_run_execution_token_stmt;
DEALLOCATE PREPARE add_process_run_execution_token_stmt;

SET @process_run_lease_owner_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_process_run'
    AND COLUMN_NAME = 'lease_owner'
);
SET @add_process_run_lease_owner_sql = IF(
  @process_run_lease_owner_exists = 0,
  'ALTER TABLE `ai_business_process_run` ADD COLUMN `lease_owner` varchar(128) NULL COMMENT ''当前执行租约持有者'' AFTER `execution_token`',
  'SELECT 1'
);
PREPARE add_process_run_lease_owner_stmt FROM @add_process_run_lease_owner_sql;
EXECUTE add_process_run_lease_owner_stmt;
DEALLOCATE PREPARE add_process_run_lease_owner_stmt;

SET @process_run_lease_expire_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_process_run'
    AND COLUMN_NAME = 'lease_expire_time'
);
SET @add_process_run_lease_expire_time_sql = IF(
  @process_run_lease_expire_time_exists = 0,
  'ALTER TABLE `ai_business_process_run` ADD COLUMN `lease_expire_time` datetime(3) NULL COMMENT ''执行租约过期时间'' AFTER `lease_owner`',
  'SELECT 1'
);
PREPARE add_process_run_lease_expire_time_stmt FROM @add_process_run_lease_expire_time_sql;
EXECUTE add_process_run_lease_expire_time_stmt;
DEALLOCATE PREPARE add_process_run_lease_expire_time_stmt;

SET @process_run_heartbeat_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_process_run'
    AND COLUMN_NAME = 'heartbeat_time'
);
SET @add_process_run_heartbeat_time_sql = IF(
  @process_run_heartbeat_time_exists = 0,
  'ALTER TABLE `ai_business_process_run` ADD COLUMN `heartbeat_time` datetime(3) NULL COMMENT ''执行器最近心跳时间'' AFTER `lease_expire_time`',
  'SELECT 1'
);
PREPARE add_process_run_heartbeat_time_stmt FROM @add_process_run_heartbeat_time_sql;
EXECUTE add_process_run_heartbeat_time_stmt;
DEALLOCATE PREPARE add_process_run_heartbeat_time_stmt;

SET @process_run_lease_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_process_run'
    AND INDEX_NAME = 'idx_ai_business_process_run_lease'
);
SET @add_process_run_lease_index_sql = IF(
  @process_run_lease_index_exists = 0,
  'CREATE INDEX `idx_ai_business_process_run_lease` ON `ai_business_process_run` (`tenant_id`, `status`, `lease_expire_time`, `id`)',
  'SELECT 1'
);
PREPARE add_process_run_lease_index_stmt FROM @add_process_run_lease_index_sql;
EXECUTE add_process_run_lease_index_stmt;
DEALLOCATE PREPARE add_process_run_lease_index_stmt;
