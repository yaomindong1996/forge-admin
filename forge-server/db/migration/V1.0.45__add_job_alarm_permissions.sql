-- 定时任务最终失败告警、细粒度资源权限和管理审计基础资源。

SET @job_config_table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
);

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'alarm_enabled'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN alarm_enabled tinyint NOT NULL DEFAULT 0 COMMENT ''是否启用失败告警：0否 1是'' AFTER consecutive_failures',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'alarm_channels'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN alarm_channels varchar(64) NULL COMMENT ''失败告警渠道：WEB,EMAIL'' AFTER alarm_enabled',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_job_config'
    AND COLUMN_NAME = 'alarm_recipient_user_ids'
);
SET @sql = IF(@job_config_table_exists = 1 AND @column_exists = 0,
  'ALTER TABLE sys_job_config ADD COLUMN alarm_recipient_user_ids varchar(2000) NULL COMMENT ''站内信接收用户ID，逗号分隔'' AFTER alarm_channels',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT 1, '定时任务告警渠道', 'sys_job_alarm_channel', 1, '定时任务最终失败通知渠道',
       1, NOW(), 1, NOW(), 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type data
  WHERE data.tenant_id = 1
    AND data.dict_type = 'sys_job_alarm_channel'
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '站内信' dict_label, 'WEB' dict_value,
         'sys_job_alarm_channel' dict_type, 'info' list_class, 'Y' is_default, '发送给指定平台用户' remark
  UNION ALL SELECT 1, 2, '邮件', 'EMAIL', 'sys_job_alarm_channel', 'success', 'N', '发送给指定邮箱地址'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data data
  WHERE data.tenant_id = seed.tenant_id
    AND data.dict_type = seed.dict_type
    AND data.dict_value = seed.dict_value
);

SET @job_config_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/system/job-config'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

-- 只创建可分配的按钮权限，不自动绑定任何普通角色。
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @job_config_menu_id, 3, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '查看任务配置' resource_name, 1 sort, 'system:jobConfig:list' perms, '查询任务配置、执行目录和监控摘要' remark
  UNION ALL SELECT 1, '新增任务配置', 2, 'system:jobConfig:add', '新增定时任务配置'
  UNION ALL SELECT 1, '编辑任务配置', 3, 'system:jobConfig:edit', '编辑任务配置和执行计划'
  UNION ALL SELECT 1, '删除任务配置', 4, 'system:jobConfig:remove', '逻辑删除定时任务配置'
  UNION ALL SELECT 1, '启用任务', 5, 'system:jobConfig:start', '启用并同步定时任务'
  UNION ALL SELECT 1, '停用任务', 6, 'system:jobConfig:stop', '停用并同步定时任务'
  UNION ALL SELECT 1, '立即运行任务', 7, 'system:jobConfig:trigger', '立即触发一次任务执行'
  UNION ALL SELECT 1, '重新同步任务', 8, 'system:jobConfig:sync', '重试数据库与调度器同步'
  UNION ALL SELECT 1, '管理危险执行目标', 9, 'system:jobConfig:dangerous', '管理BEAN、RPC和受保护任务'
  UNION ALL SELECT 1, '查看任务日志', 20, 'system:jobLog:list', '查询任务运行日志'
  UNION ALL SELECT 1, '查看敏感日志详情', 21, 'system:jobLog:detail', '查看经过裁剪的结果和异常摘要'
  UNION ALL SELECT 1, '导出任务日志', 22, 'system:jobLog:export', '导出任务运行日志安全字段'
  UNION ALL SELECT 1, '清理任务日志', 23, 'system:jobLog:clean', '物理清理超期终态日志'
) seed
WHERE @job_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = seed.tenant_id
      AND resource.resource_type = 3
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );

-- API 权限使用实际通配符；PathMatcher 不解析 {id} 占位符。
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  api_method, api_url, keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @job_config_menu_id, 4, seed.sort,
       NULL, NULL, 0, 0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       seed.api_method, seed.api_url, 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '任务配置查询接口' resource_name, 101 sort, 'system:jobConfig:api:read' perms,
         'GET' api_method, '/job/config/**' api_url, '任务配置、目录、计划和概览查询接口' remark
  UNION ALL SELECT 1, '任务配置新增接口', 102, 'system:jobConfig:api:add', 'POST', '/job/config', '新增任务配置接口'
  UNION ALL SELECT 1, '任务配置编辑接口', 103, 'system:jobConfig:api:edit', 'PUT', '/job/config', '编辑任务配置接口'
  UNION ALL SELECT 1, '任务配置删除接口', 104, 'system:jobConfig:api:remove', 'DELETE', '/job/config/*', '删除任务配置接口'
  UNION ALL SELECT 1, '任务启用接口', 105, 'system:jobConfig:api:start', 'POST', '/job/config/*/start', '启用任务接口'
  UNION ALL SELECT 1, '任务停用接口', 106, 'system:jobConfig:api:stop', 'POST', '/job/config/*/stop', '停用任务接口'
  UNION ALL SELECT 1, '任务立即运行接口', 107, 'system:jobConfig:api:trigger', 'POST', '/job/config/*/trigger', '立即运行任务接口'
  UNION ALL SELECT 1, '任务同步接口', 108, 'system:jobConfig:api:sync', 'POST', '/job/config/*/sync', '重新同步任务接口'
  UNION ALL SELECT 1, '任务执行计划编辑接口', 109, 'system:jobConfig:api:cron', 'POST', '/job/config/*/cron', '编辑任务Cron接口'
  UNION ALL SELECT 1, '任务Cron预览接口', 110, 'system:jobConfig:api:preview', 'POST', '/job/config/cron/preview', 'Cron计划预览接口'
  UNION ALL SELECT 1, '任务监控摘要接口', 111, 'system:jobConfig:api:monitor', 'GET', '/job/monitor/summary', '任务监控摘要接口'
  UNION ALL SELECT 1, '任务日志查询接口', 120, 'system:jobLog:api:list', 'GET', '/job/log/page', '任务日志分页接口'
  UNION ALL SELECT 1, '任务日志详情接口', 121, 'system:jobLog:api:detail', 'GET', '/job/log/*', '任务日志敏感详情接口'
  UNION ALL SELECT 1, '任务日志导出接口', 122, 'system:jobLog:api:export', 'POST', '/job/log/export', '任务日志安全导出接口'
  UNION ALL SELECT 1, '任务日志清理接口', 123, 'system:jobLog:api:clean', 'DELETE', '/job/log/clean', '任务日志留存清理接口'
) seed
WHERE @job_config_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource resource
    WHERE resource.tenant_id = seed.tenant_id
      AND resource.resource_type = 4
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );
