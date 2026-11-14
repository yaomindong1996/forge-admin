-- 企业协同连接升级后，sys_social_config 只表示连接根，应用凭据落在 sys_social_app_config。
-- 新增连接不再填写 client_id/client_secret；存量 NOT NULL 会在严格模式下导致插入失败。

SET @table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_social_config'
);

SET @client_id_not_nullable = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_social_config'
    AND COLUMN_NAME = 'client_id'
    AND IS_NULLABLE = 'NO'
);
SET @sql = IF(@table_exists = 1 AND @client_id_not_nullable = 1,
  'ALTER TABLE sys_social_config MODIFY COLUMN client_id varchar(255) DEFAULT NULL COMMENT ''应用ID/Key（旧登录配置字段，兼容期保留；凭据改存应用表后可空）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @client_secret_not_nullable = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_social_config'
    AND COLUMN_NAME = 'client_secret'
    AND IS_NULLABLE = 'NO'
);
SET @sql = IF(@table_exists = 1 AND @client_secret_not_nullable = 1,
  'ALTER TABLE sys_social_config MODIFY COLUMN client_secret varchar(255) DEFAULT NULL COMMENT ''应用Secret（旧登录配置明文字段，兼容期保留；新凭据统一存应用表密文，本列可空）''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
