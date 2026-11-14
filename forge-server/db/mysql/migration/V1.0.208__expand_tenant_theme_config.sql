-- 完整主题 JSON 已超过旧 varchar(1000) 容量，只扩容、不截断或重写配置。
-- 回滚应用无需缩列；如确需恢复旧列，须先备份并确认所有配置满足旧容量。

-- 只处理当前库的窄文本列；重复执行或已有 TEXT/JSON/更大文本列时不降级。
SET @tenant_theme_expand_sql = (
  SELECT CONCAT(
    'ALTER TABLE `sys_tenant` MODIFY COLUMN `theme_config` TEXT CHARACTER SET ',
    CHARACTER_SET_NAME,
    ' COLLATE ', COLLATION_NAME,
    ' NULL COMMENT ''主题配置'''
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_tenant'
    AND COLUMN_NAME = 'theme_config'
    AND DATA_TYPE IN ('char', 'varchar', 'tinytext')
);
SET @tenant_theme_expand_sql = COALESCE(@tenant_theme_expand_sql, 'SELECT 1');
PREPARE tenant_theme_expand_stmt FROM @tenant_theme_expand_sql;
EXECUTE tenant_theme_expand_stmt;
DEALLOCATE PREPARE tenant_theme_expand_stmt;
