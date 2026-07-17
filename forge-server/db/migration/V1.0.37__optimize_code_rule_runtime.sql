-- 编码规则运行时优化：存量规则保留旧水位续接，新建规则使用严格容量；优化分段查询索引。

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_code_rule'
    AND COLUMN_NAME = 'legacy_compat_enabled'
);
SET @sql = IF(@column_exists = 0,
  'ALTER TABLE ai_code_rule ADD COLUMN legacy_compat_enabled tinyint NOT NULL DEFAULT 1 COMMENT ''是否兼容旧编码计数器水位'' AFTER version_no',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @segment_order_index_columns = (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_code_rule_segment'
    AND INDEX_NAME = 'idx_ai_code_rule_segment_order'
);
SET @sql = IF(
  @segment_order_index_columns IS NOT NULL
    AND @segment_order_index_columns != 'tenant_id,rule_id,del_flag,segment_order,id',
  'ALTER TABLE ai_code_rule_segment DROP INDEX idx_ai_code_rule_segment_order',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_code_rule_segment'
    AND INDEX_NAME = 'idx_ai_code_rule_segment_order'
);
SET @sql = IF(@index_exists = 0,
  'CREATE INDEX idx_ai_code_rule_segment_order ON ai_code_rule_segment (tenant_id, rule_id, del_flag, segment_order, id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
