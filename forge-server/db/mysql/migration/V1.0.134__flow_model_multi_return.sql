-- 流程模型多级指定节点退回开关。
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_model'
      AND COLUMN_NAME = 'allow_multi_return'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE sys_flow_model ADD COLUMN allow_multi_return TINYINT NOT NULL DEFAULT 0 COMMENT ''是否允许多级指定节点退回'' AFTER todo_detail_url_template',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
