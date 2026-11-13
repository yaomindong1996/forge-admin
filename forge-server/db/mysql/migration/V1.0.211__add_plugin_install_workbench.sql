-- 插件任务只存审计/私有包，不执行构建、部署或授权；权限范围仅平台超级管理员。
-- 回滚：隐藏本轮按钮/API；确认无待构建任务后归档表，不能用源码回滚删除数据库对象。
CREATE TABLE IF NOT EXISTS sys_plugin_task (
    id varchar(36) NOT NULL COMMENT '随机任务ID',
    tenant_id bigint NOT NULL,
    request_id varchar(36) NOT NULL COMMENT '上传幂等请求ID',
    plugin_id varchar(32) NOT NULL,
    plugin_name varchar(128) NOT NULL,
    plugin_version varchar(256) NOT NULL,
    operation_type varchar(16) NOT NULL,
    task_status varchar(32) NOT NULL,
    active_plugin_id varchar(32) DEFAULT NULL COMMENT '待确认/待构建的占用键，终态置空',
    revision int NOT NULL DEFAULT 0,
    archive_sha256 char(64) NOT NULL,
    file_name varchar(128) NOT NULL,
    archive_bytes int NOT NULL,
    archive_data mediumblob NOT NULL COMMENT '私有ZIP，普通查询不读取/不提供下载',
    runtime_snapshot char(64) NOT NULL,
    preview_json mediumtext NOT NULL,
    confirmed_by bigint DEFAULT NULL,
    confirmed_time datetime DEFAULT NULL,
    cancelled_by bigint DEFAULT NULL,
    cancelled_time datetime DEFAULT NULL,
    del_flag int NOT NULL DEFAULT 0,
    create_by bigint NOT NULL,
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept bigint DEFAULT NULL,
    update_by bigint NOT NULL,
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_plugin_task_request (tenant_id, create_by, request_id),
    UNIQUE KEY uk_sys_plugin_task_active (tenant_id, active_plugin_id),
    KEY idx_sys_plugin_task_page (tenant_id, del_flag, create_time),
    CONSTRAINT ck_sys_plugin_task_bytes CHECK (archive_bytes BETWEEN 1 AND 8388608)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件安装预检与确认审计';

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_public, menu_status, visible,
    perms, api_method, api_url, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, entry.name, menu.id, entry.type, entry.sort, 0, 1, 1, entry.perms,
       entry.method, entry.url, 1, NOW(), 1, NOW(), 1, 'pc', 0, 0
FROM sys_resource menu
CROSS JOIN (
    SELECT '上传插件预检' AS name, 4 AS type, 10 AS sort, 'system:plugin:upload' AS perms,
           'POST' AS method, '/system/plugin-task/upload' AS url
    UNION ALL SELECT '插件任务查询', 4, 11, 'system:plugin:task:list', 'GET', '/system/plugin-task/page'
    UNION ALL SELECT '插件任务详情', 4, 12, 'system:plugin:task:detail', 'GET', '/system/plugin-task/*'
    UNION ALL SELECT '确认待构建', 4, 13, 'system:plugin:confirm', 'POST', '/system/plugin-task/*/confirm'
    UNION ALL SELECT '取消插件任务', 4, 14, 'system:plugin:cancel', 'POST', '/system/plugin-task/*/cancel'
    UNION ALL SELECT '当前后端构建快照', 4, 19, 'system:plugin:snapshot', 'GET', '/system/plugin/snapshot'
    UNION ALL SELECT '上传预检', 3, 15, 'system:plugin:upload', NULL, NULL
    UNION ALL SELECT '查看任务', 3, 16, 'system:plugin:task:detail', NULL, NULL
    UNION ALL SELECT '确认待构建', 3, 17, 'system:plugin:confirm', NULL, NULL
    UNION ALL SELECT '取消任务', 3, 18, 'system:plugin:cancel', NULL, NULL
) entry
WHERE menu.tenant_id = 1 AND menu.client_code = 'pc'
  AND menu.resource_type = 2 AND menu.del_flag = 0 AND menu.min_user_type = 0
  AND menu.perms = 'system:plugin:view' AND menu.path = '/system/plugin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing WHERE existing.tenant_id = 1 AND existing.del_flag = 0
      AND existing.client_code = 'pc' AND existing.resource_type = entry.type
      AND ((entry.type = 3 AND existing.perms = entry.perms)
          OR (entry.type = 4 AND (existing.perms = entry.perms
              OR (existing.api_method = entry.method AND existing.api_url = entry.url))))
  );

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.name, entry.code, 1, 1, NOW(), 1, NOW(), 1, 0 FROM (
    SELECT '插件任务状态' AS name, 'sys_plugin_task_status' AS code
    UNION ALL SELECT '插件任务操作', 'sys_plugin_task_operation'
) entry WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type existing WHERE existing.tenant_id = 1
    AND existing.dict_type = entry.code AND existing.del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.sort, entry.label, entry.value, entry.type, entry.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0 FROM (
    SELECT 1 AS sort, '待确认' AS label, 'await_confirmation' AS value,
           'sys_plugin_task_status' AS type, 'warning' AS style
    UNION ALL SELECT 2, '预检阻断', 'blocked', 'sys_plugin_task_status', 'error'
    UNION ALL SELECT 3, '待构建', 'queued', 'sys_plugin_task_status', 'info'
    UNION ALL SELECT 4, '已取消', 'cancelled', 'sys_plugin_task_status', 'default'
    UNION ALL SELECT 1, '新增（待源码核验）', 'install', 'sys_plugin_task_operation', 'default'
    UNION ALL SELECT 2, '整包替换（待源码核验）', 'replace', 'sys_plugin_task_operation', 'warning'
) entry WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing WHERE existing.tenant_id = 1
    AND existing.dict_type = entry.type AND existing.dict_value = entry.value AND existing.del_flag = 0
);
