-- 超时扫描按 due_date + id 稳定游标读取活动任务，避免每轮全表排序。
SET @idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_task'
      AND INDEX_NAME = 'idx_flow_task_timeout_cursor'
);

SET @create_idx_sql = IF(
    @idx_exists = 0,
    'CREATE INDEX idx_flow_task_timeout_cursor ON sys_flow_task (due_date, id, status, tenant_id)',
    'SELECT 1'
);

PREPARE create_idx_stmt FROM @create_idx_sql;
EXECUTE create_idx_stmt;
DEALLOCATE PREPARE create_idx_stmt;
