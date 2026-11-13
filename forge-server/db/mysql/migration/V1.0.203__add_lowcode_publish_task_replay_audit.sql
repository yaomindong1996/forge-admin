-- 为低代码发布 DEAD 任务增加授权人工重放审计。
-- 回滚：先关闭人工重放接口；审计列应优先保留，确需回滚时确认无审计要求后再删除。

SET @publish_replay_count_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_lowcode_publish_task'
    AND COLUMN_NAME = 'replay_count'
);
SET @add_publish_replay_count_sql = IF(
  @publish_replay_count_exists = 0,
  'ALTER TABLE `ai_lowcode_publish_task` ADD COLUMN `replay_count` int NOT NULL DEFAULT 0 COMMENT ''人工重放次数'' AFTER `completed_time`',
  'SELECT 1'
);
PREPARE add_publish_replay_count_stmt FROM @add_publish_replay_count_sql;
EXECUTE add_publish_replay_count_stmt;
DEALLOCATE PREPARE add_publish_replay_count_stmt;

SET @publish_replayed_by_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_lowcode_publish_task'
    AND COLUMN_NAME = 'replayed_by'
);
SET @add_publish_replayed_by_sql = IF(
  @publish_replayed_by_exists = 0,
  'ALTER TABLE `ai_lowcode_publish_task` ADD COLUMN `replayed_by` bigint NULL COMMENT ''最近人工重放操作人'' AFTER `replay_count`',
  'SELECT 1'
);
PREPARE add_publish_replayed_by_stmt FROM @add_publish_replayed_by_sql;
EXECUTE add_publish_replayed_by_stmt;
DEALLOCATE PREPARE add_publish_replayed_by_stmt;

SET @publish_replayed_time_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_lowcode_publish_task'
    AND COLUMN_NAME = 'replayed_time'
);
SET @add_publish_replayed_time_sql = IF(
  @publish_replayed_time_exists = 0,
  'ALTER TABLE `ai_lowcode_publish_task` ADD COLUMN `replayed_time` datetime NULL COMMENT ''最近人工重放时间'' AFTER `replayed_by`',
  'SELECT 1'
);
PREPARE add_publish_replayed_time_stmt FROM @add_publish_replayed_time_sql;
EXECUTE add_publish_replayed_time_stmt;
DEALLOCATE PREPARE add_publish_replayed_time_stmt;

SET @publish_replay_reason_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_lowcode_publish_task'
    AND COLUMN_NAME = 'replay_reason'
);
SET @add_publish_replay_reason_sql = IF(
  @publish_replay_reason_exists = 0,
  'ALTER TABLE `ai_lowcode_publish_task` ADD COLUMN `replay_reason` varchar(500) NULL COMMENT ''最近人工重放原因'' AFTER `replayed_time`',
  'SELECT 1'
);
PREPARE add_publish_replay_reason_stmt FROM @add_publish_replay_reason_sql;
EXECUTE add_publish_replay_reason_stmt;
DEALLOCATE PREPARE add_publish_replay_reason_stmt;

SET @lowcode_app_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/ai/lowcode-apps'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code, min_user_type
)
SELECT 1, '重放低代码发布死信', COALESCE(@lowcode_app_menu_id, 0), 3, 90,
       0, '_self', 0, 1, 1, 'ai:lowcode:publish-task:replay',
       0, 0, '仅租户管理员或平台管理员显式授权的发布死信恢复操作',
       1, NOW(), 1, NOW(), 1, 'pc', 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource resource_row
  WHERE resource_row.tenant_id = 1
    AND resource_row.resource_type = 3
    AND resource_row.perms = 'ai:lowcode:publish-task:replay'
    AND resource_row.del_flag = 0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type
)
SELECT 1, '重放低代码发布死信接口', COALESCE(@lowcode_app_menu_id, 0), 4, 91,
       0, '_self', 0, 1, 1, 'ai:lowcode:publish-task:replay',
       'POST', '/ai/lowcode/app/publish-tasks/*/replay', 0, 0,
       '仅租户管理员或平台管理员显式授权的发布死信恢复接口',
       1, NOW(), 1, NOW(), 1, 'pc', 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource resource_row
  WHERE resource_row.tenant_id = 1
    AND resource_row.resource_type = 4
    AND resource_row.api_method = 'POST'
    AND resource_row.api_url = '/ai/lowcode/app/publish-tasks/*/replay'
    AND resource_row.del_flag = 0
);
