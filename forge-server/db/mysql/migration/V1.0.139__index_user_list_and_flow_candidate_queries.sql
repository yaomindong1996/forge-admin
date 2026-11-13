-- 用户列表相关子查询按 user_id 回表；候选任务按租户+状态+时间分页。

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user_org'
      AND INDEX_NAME = 'idx_sys_user_org_user_tenant'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_user_org ADD INDEX idx_sys_user_org_user_tenant (user_id, tenant_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user_post'
      AND INDEX_NAME = 'idx_sys_user_post_user_tenant'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_user_post ADD INDEX idx_sys_user_post_user_tenant (user_id, tenant_id)',
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
      AND INDEX_NAME = 'idx_flow_task_tenant_status_create'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE sys_flow_task ADD INDEX idx_flow_task_tenant_status_create (tenant_id, status, create_time)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
