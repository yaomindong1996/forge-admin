-- 外部 API 显式声明低代码只读查询资格及输入/输出契约；默认关闭，不开放任何存量接口。

SET @external_api_lowcode_enabled_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND COLUMN_NAME = 'lowcode_query_enabled'
);
SET @add_external_api_lowcode_enabled_sql = IF(
  @external_api_lowcode_enabled_exists = 0,
  'ALTER TABLE `sys_external_api` ADD COLUMN `lowcode_query_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否允许作为低代码只读查询源'' AFTER `required_permission`',
  'SELECT 1'
);
PREPARE add_external_api_lowcode_enabled_stmt FROM @add_external_api_lowcode_enabled_sql;
EXECUTE add_external_api_lowcode_enabled_stmt;
DEALLOCATE PREPARE add_external_api_lowcode_enabled_stmt;

SET @external_api_input_schema_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND COLUMN_NAME = 'input_schema_json'
);
SET @add_external_api_input_schema_sql = IF(
  @external_api_input_schema_exists = 0,
  'ALTER TABLE `sys_external_api` ADD COLUMN `input_schema_json` text NULL COMMENT ''低代码查询输入Schema(JSON数组)'' AFTER `lowcode_query_enabled`',
  'SELECT 1'
);
PREPARE add_external_api_input_schema_stmt FROM @add_external_api_input_schema_sql;
EXECUTE add_external_api_input_schema_stmt;
DEALLOCATE PREPARE add_external_api_input_schema_stmt;

SET @external_api_output_schema_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND COLUMN_NAME = 'output_schema_json'
);
SET @add_external_api_output_schema_sql = IF(
  @external_api_output_schema_exists = 0,
  'ALTER TABLE `sys_external_api` ADD COLUMN `output_schema_json` text NULL COMMENT ''低代码查询输出Schema(JSON数组)'' AFTER `input_schema_json`',
  'SELECT 1'
);
PREPARE add_external_api_output_schema_stmt FROM @add_external_api_output_schema_sql;
EXECUTE add_external_api_output_schema_stmt;
DEALLOCATE PREPARE add_external_api_output_schema_stmt;

SET @external_api_lowcode_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_external_api'
    AND INDEX_NAME = 'idx_external_api_lowcode_query'
);
SET @add_external_api_lowcode_index_sql = IF(
  @external_api_lowcode_index_exists = 0,
  'CREATE INDEX `idx_external_api_lowcode_query` ON `sys_external_api` (`tenant_id`, `lowcode_query_enabled`, `api_status`, `system_id`)',
  'SELECT 1'
);
PREPARE add_external_api_lowcode_index_stmt FROM @add_external_api_lowcode_index_sql;
EXECUTE add_external_api_lowcode_index_stmt;
DEALLOCATE PREPARE add_external_api_lowcode_index_stmt;
