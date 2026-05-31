SET @column_exists := (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object_design_version'
      AND COLUMN_NAME = 'designer_options_snapshot'
);

SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `ai_business_object_design_version` ADD COLUMN `designer_options_snapshot` json DEFAULT NULL COMMENT ''表单优先设计器扩展快照'' AFTER `relation_snapshot`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
