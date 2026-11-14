-- 待办/已办列表按租户、处理人、状态过滤并按时间排序。
-- 列表接口位于 @IgnoreTenant 边界，查询 SQL 自行携带 tenant_id 条件。

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND INDEX_NAME = 'idx_flow_task_tenant_assignee_status_create'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_flow_task ADD INDEX idx_flow_task_tenant_assignee_status_create (tenant_id, assignee, status, create_time)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND INDEX_NAME = 'idx_flow_task_tenant_assignee_status_complete'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_flow_task ADD INDEX idx_flow_task_tenant_assignee_status_complete (tenant_id, assignee, status, complete_time)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND INDEX_NAME = 'idx_flow_task_tenant_start_user_create'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_flow_task ADD INDEX idx_flow_task_tenant_start_user_create (tenant_id, start_user_id, create_time)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
