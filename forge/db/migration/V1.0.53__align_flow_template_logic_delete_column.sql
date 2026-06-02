-- Phase 8 verification fix: align sys_flow_template with FlowTemplate @TableLogic.
-- Some existing databases were initialized from an older flow SQL that did not contain del_flag.

SET @add_flow_template_del_flag := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE sys_flow_template ADD COLUMN del_flag tinyint NOT NULL DEFAULT 0 COMMENT ''删除标志：0正常 1删除'' AFTER update_time',
    'SELECT 1'
  )
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_flow_template'
    AND column_name = 'del_flag'
);
PREPARE stmt FROM @add_flow_template_del_flag;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
