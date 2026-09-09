-- 保存 Flowable 多实例动态加签产生的子任务/执行，支持精确减签和审计追踪。
SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `child_task_id` varchar(64) DEFAULT NULL COMMENT ''动态加签子任务ID'' AFTER `parent_task_id`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'child_task_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `child_execution_id` varchar(64) DEFAULT NULL COMMENT ''动态加签子执行ID'' AFTER `child_task_id`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'child_execution_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'CREATE INDEX `idx_flow_task_candidate_child` ON `sys_flow_task_candidate` (`tenant_id`, `child_task_id`, `status`)',
            'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND index_name = 'idx_flow_task_candidate_child'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
