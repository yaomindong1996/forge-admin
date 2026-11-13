-- 编码规则流水号段支持分别排除 I、O、Z；旧总开关数据继续等价为全选。

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_code_rule_segment'
    AND COLUMN_NAME = 'excluded_characters'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE ai_code_rule_segment ADD COLUMN excluded_characters varchar(16) DEFAULT NULL COMMENT ''具体排除的易混淆字符，逗号分隔：I/O/Z'' AFTER exclude_ambiguous',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ai_code_rule_segment
SET excluded_characters = 'I,O,Z',
    update_time = NOW()
WHERE exclude_ambiguous = 1
  AND (excluded_characters IS NULL OR excluded_characters = '');
