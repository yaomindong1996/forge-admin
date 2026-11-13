-- 为登录会话增加数据库凭证版本。改密/重置时原子递增，所有实例在受保护请求上读取并比对。
-- 现有会话没有版本字段时按 0 处理，可与默认值 0 平滑兼容；首次改密后旧会话立即失效。
-- 回滚应用版本前可保留该列；直接删除会失去跨实例旧 Token 吊销兜底，不建议回滚数据库列。

SET @sys_user_password_version_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'password_version'
);
SET @add_sys_user_password_version_sql = IF(
  @sys_user_password_version_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `password_version` bigint NOT NULL DEFAULT 0 COMMENT ''密码凭证版本，每次改密原子递增'' AFTER `password`',
  'SELECT 1'
);
PREPARE add_sys_user_password_version_stmt FROM @add_sys_user_password_version_sql;
EXECUTE add_sys_user_password_version_stmt;
DEALLOCATE PREPARE add_sys_user_password_version_stmt;
