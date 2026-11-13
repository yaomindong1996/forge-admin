-- 动态加签关系审计字段。兼容既有候选关系数据，旧 FLOWABLE 行保持 NULL。
SET @schema_name = DATABASE();

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `parent_task_id` varchar(64) DEFAULT NULL COMMENT ''动态加签父任务ID'' AFTER `task_id`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'parent_task_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `sign_mode` varchar(16) DEFAULT NULL COMMENT ''加签模式'' AFTER `source`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'sign_mode'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `operator_id` varchar(64) DEFAULT NULL COMMENT ''关系操作人'' AFTER `sign_mode`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'operator_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'ALTER TABLE `sys_flow_task_candidate` ADD COLUMN `reason` varchar(500) DEFAULT NULL COMMENT ''加签/减签原因'' AFTER `operator_id`',
            'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND column_name = 'reason'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(COUNT(*) = 0,
            'CREATE INDEX `idx_flow_task_candidate_parent` ON `sys_flow_task_candidate` (`tenant_id`, `parent_task_id`, `candidate_type`, `source`)',
            'SELECT 1')
  FROM information_schema.statistics
  WHERE table_schema = @schema_name
    AND table_name = 'sys_flow_task_candidate'
    AND index_name = 'idx_flow_task_candidate_parent'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
