-- 为低代码流程关联增加主动状态对账租约；即使终态回调完全丢失，也可从 Flow 服务恢复本地终态。
-- 关联表是运行态恢复记录，不使用逻辑删除。DEAD 保留失败类型并等待后续授权人工重放。
-- 回滚：先关闭 forge.business.flow-status-sync.enabled 并确认无 PROCESSING，再删除索引和本迁移新增列。

SET @flow_link_sync_status_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_status'
);
SET @add_flow_link_sync_status_sql = IF(
  @flow_link_sync_status_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_status` varchar(32) NOT NULL DEFAULT ''PENDING'' COMMENT ''主动状态对账状态'' AFTER `variables_snapshot`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_status_stmt FROM @add_flow_link_sync_status_sql;
EXECUTE add_flow_link_sync_status_stmt;
DEALLOCATE PREPARE add_flow_link_sync_status_stmt;

SET @flow_link_sync_retry_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_retry_count'
);
SET @add_flow_link_sync_retry_sql = IF(
  @flow_link_sync_retry_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_retry_count` int NOT NULL DEFAULT 0 COMMENT ''主动状态对账连续尝试次数'' AFTER `status_sync_status`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_retry_stmt FROM @add_flow_link_sync_retry_sql;
EXECUTE add_flow_link_sync_retry_stmt;
DEALLOCATE PREPARE add_flow_link_sync_retry_stmt;

SET @flow_link_sync_next_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_next_time'
);
SET @add_flow_link_sync_next_sql = IF(
  @flow_link_sync_next_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_next_time` datetime NULL COMMENT ''下次允许主动对账时间'' AFTER `status_sync_retry_count`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_next_stmt FROM @add_flow_link_sync_next_sql;
EXECUTE add_flow_link_sync_next_stmt;
DEALLOCATE PREPARE add_flow_link_sync_next_stmt;

SET @flow_link_sync_owner_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_lock_owner'
);
SET @add_flow_link_sync_owner_sql = IF(
  @flow_link_sync_owner_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_lock_owner` varchar(128) NULL COMMENT ''主动状态对账租约持有者'' AFTER `status_sync_next_time`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_owner_stmt FROM @add_flow_link_sync_owner_sql;
EXECUTE add_flow_link_sync_owner_stmt;
DEALLOCATE PREPARE add_flow_link_sync_owner_stmt;

SET @flow_link_sync_lock_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_lock_time'
);
SET @add_flow_link_sync_lock_time_sql = IF(
  @flow_link_sync_lock_time_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_lock_time` datetime NULL COMMENT ''主动状态对账租约时间'' AFTER `status_sync_lock_owner`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_lock_time_stmt FROM @add_flow_link_sync_lock_time_sql;
EXECUTE add_flow_link_sync_lock_time_stmt;
DEALLOCATE PREPARE add_flow_link_sync_lock_time_stmt;

SET @flow_link_sync_remote_status_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_remote_status'
);
SET @add_flow_link_sync_remote_status_sql = IF(
  @flow_link_sync_remote_status_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_remote_status` varchar(32) NULL COMMENT ''最近读取的Flow业务状态'' AFTER `status_sync_lock_time`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_remote_status_stmt FROM @add_flow_link_sync_remote_status_sql;
EXECUTE add_flow_link_sync_remote_status_stmt;
DEALLOCATE PREPARE add_flow_link_sync_remote_status_stmt;

SET @flow_link_sync_error_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_sync_error_type'
);
SET @add_flow_link_sync_error_sql = IF(
  @flow_link_sync_error_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_sync_error_type` varchar(128) NULL COMMENT ''最近主动对账失败类型'' AFTER `status_sync_remote_status`',
  'SELECT 1'
);
PREPARE add_flow_link_sync_error_stmt FROM @add_flow_link_sync_error_sql;
EXECUTE add_flow_link_sync_error_stmt;
DEALLOCATE PREPARE add_flow_link_sync_error_stmt;

SET @flow_link_synced_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND COLUMN_NAME = 'status_synced_time'
);
SET @add_flow_link_synced_time_sql = IF(
  @flow_link_synced_time_exists = 0,
  'ALTER TABLE `ai_business_flow_instance_link` ADD COLUMN `status_synced_time` datetime NULL COMMENT ''最近成功主动对账时间'' AFTER `status_sync_error_type`',
  'SELECT 1'
);
PREPARE add_flow_link_synced_time_stmt FROM @add_flow_link_synced_time_sql;
EXECUTE add_flow_link_synced_time_stmt;
DEALLOCATE PREPARE add_flow_link_synced_time_stmt;

UPDATE `ai_business_flow_instance_link`
SET `status_sync_status` = 'COMPLETED'
WHERE (`end_time` IS NOT NULL OR `result` IN ('APPROVED', 'REJECTED', 'CANCELED'))
  AND `status_sync_status` = 'PENDING';

SET @flow_link_sync_index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_flow_instance_link'
    AND INDEX_NAME = 'idx_ai_business_flow_status_sync'
);
SET @add_flow_link_sync_index_sql = IF(
  @flow_link_sync_index_exists = 0,
  'CREATE INDEX `idx_ai_business_flow_status_sync` ON `ai_business_flow_instance_link` (`status_sync_status`, `status_sync_next_time`, `status_sync_lock_time`, `flow_status`, `id`)',
  'SELECT 1'
);
PREPARE add_flow_link_sync_index_stmt FROM @add_flow_link_sync_index_sql;
EXECUTE add_flow_link_sync_index_stmt;
DEALLOCATE PREPARE add_flow_link_sync_index_stmt;
