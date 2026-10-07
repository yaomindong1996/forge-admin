-- 仅扩展资源元数据；存量 NULL 编码保持原菜单/权限行为，不批量改写授权。
-- 回滚：先回退依赖该字段的应用，再按需 ALTER TABLE sys_resource DROP COLUMN feature_code。
SET @resource_feature_code_sql = (
  SELECT IF(
    COUNT(*) = 0,
    CONCAT(
      'ALTER TABLE `sys_resource` ADD COLUMN `feature_code` VARCHAR(64) NULL ',
      'COMMENT ''功能授权编码，空表示不受授权控制'' AFTER `perms`'
    ),
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_resource'
    AND COLUMN_NAME = 'feature_code'
);
PREPARE resource_feature_code_stmt FROM @resource_feature_code_sql;
EXECUTE resource_feature_code_stmt;
DEALLOCATE PREPARE resource_feature_code_stmt;
