-- 客户端前端入口地址：供 admin 单点跳转子系统（如 forge_report / AI大屏）时读取，可在「系统管理 → 客户端」维护。
SET @sys_client_exists = (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_client'
);

SET @home_url_exists = (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_client'
    AND column_name = 'home_url'
);

SET @ddl = IF(
  @sys_client_exists = 1 AND @home_url_exists = 0,
  'ALTER TABLE sys_client
     ADD COLUMN home_url varchar(512) DEFAULT NULL COMMENT ''前端入口地址（SSO 跳转 baseUrl，如 http://host/forge-report）'' AFTER description',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
