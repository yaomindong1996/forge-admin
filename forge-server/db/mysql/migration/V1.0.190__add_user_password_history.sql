-- Password age and reuse enforcement.
-- The history table stores one-way hashes only. Rows outside the configured reuse
-- window are physically purged by the application to minimize credential material.
-- Rollback: keep the additive column/table while rolling back application code;
-- dropping either loses password-age/reuse evidence and is intentionally not automatic.

SET @password_changed_time_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'password_changed_time'
);
SET @add_password_changed_time_sql = IF(
  @password_changed_time_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `password_changed_time` datetime NULL COMMENT ''最近密码变更时间'' AFTER `password_version`',
  'SELECT 1'
);
PREPARE add_password_changed_time_stmt FROM @add_password_changed_time_sql;
EXECUTE add_password_changed_time_stmt;
DEALLOCATE PREPARE add_password_changed_time_stmt;

UPDATE sys_user
SET password_changed_time = COALESCE(password_changed_time, update_time, create_time, NOW())
WHERE password_changed_time IS NULL;

SET @password_changed_time_nullable = (
  SELECT IS_NULLABLE
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'password_changed_time'
  LIMIT 1
);
SET @make_password_changed_time_required_sql = IF(
  @password_changed_time_nullable = 'YES',
  'ALTER TABLE `sys_user` MODIFY COLUMN `password_changed_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''最近密码变更时间''',
  'SELECT 1'
);
PREPARE make_password_changed_time_required_stmt FROM @make_password_changed_time_required_sql;
EXECUTE make_password_changed_time_required_stmt;
DEALLOCATE PREPARE make_password_changed_time_required_stmt;

CREATE TABLE IF NOT EXISTS `sys_user_password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL COMMENT '用户归属租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `password_hash` varchar(100) NOT NULL COMMENT '历史密码单向哈希',
  `changed_time` datetime NOT NULL COMMENT '该密码被替换的时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_password_history_user_time` (`tenant_id`, `user_id`, `changed_time`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户密码历史（安全保留窗口）';
