-- 只创建交付控制面；真实迁移/云发布/部署由操作者授权执行。回退停用入口，审计与历史保留。
CREATE TABLE IF NOT EXISTS sys_plugin_delivery (
 id varchar(36) NOT NULL, tenant_id bigint NOT NULL, target_id varchar(64) NOT NULL,
 task_id varchar(36) NOT NULL, release_id varchar(68) NOT NULL, previous_release_id varchar(68) DEFAULT NULL,
 unverified_release_id varchar(68) DEFAULT NULL,
 action varchar(16) NOT NULL, status varchar(16) NOT NULL,
 request_id varchar(36) NOT NULL, command_sha256 char(64) NOT NULL,
 worker_id varchar(64) DEFAULT NULL, lease_hash char(64) DEFAULT NULL,
 lease_expires_time datetime DEFAULT NULL, deadline_time datetime DEFAULT NULL,
 artifact_manifest_sha256 char(64) NOT NULL, cos_verified boolean NOT NULL DEFAULT FALSE,
 runtime_verified boolean NOT NULL DEFAULT FALSE, failure_code varchar(96) DEFAULT NULL,
 backup_reference varchar(128) DEFAULT NULL, migrations_reviewed boolean NOT NULL,
 backward_compatible boolean NOT NULL, note varchar(1000) NOT NULL, del_flag int NOT NULL DEFAULT 0,
 reconcile_note varchar(1000) DEFAULT NULL, reconciled_by bigint DEFAULT NULL, reconciled_time datetime DEFAULT NULL,
 create_by bigint NOT NULL, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, create_dept bigint DEFAULT NULL,
 update_by bigint NOT NULL, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id), UNIQUE KEY uk_plugin_delivery_request (tenant_id, create_by, request_id),
 KEY idx_plugin_delivery_target (tenant_id, target_id, status), KEY idx_plugin_delivery_expiry (status, lease_expires_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件交付执行审计，超时保留占用';

CREATE TABLE IF NOT EXISTS sys_plugin_delivery_target (
 id varchar(36) NOT NULL, tenant_id bigint NOT NULL, target_id varchar(64) NOT NULL,
 active_task_id varchar(36) DEFAULT NULL, current_release_id varchar(68) DEFAULT NULL,
 previous_release_id varchar(68) DEFAULT NULL, unverified_release_id varchar(68) DEFAULT NULL,
 del_flag int NOT NULL DEFAULT 0,
 create_by bigint DEFAULT NULL, create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, create_dept bigint DEFAULT NULL,
 update_by bigint DEFAULT NULL, update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY (id), UNIQUE KEY uk_plugin_delivery_target (tenant_id, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交付目标串行占用及核验成功版本，不保存凭证';

INSERT INTO sys_dict_type
 (tenant_id, dict_name, dict_type, dict_status, create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT 1, seed.name, seed.type, 1, 1, NOW(), 1, NOW(), 1, 0
FROM (SELECT '交付动作' name, 'sys_plugin_delivery_action' type
 UNION ALL SELECT '交付状态', 'sys_plugin_delivery_status') seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type d WHERE d.tenant_id=1 AND d.dict_type=seed.type AND d.del_flag=0);

INSERT INTO sys_dict_data
 (tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
  create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT 1, seed.sort, seed.label, seed.value, seed.type, seed.style, 'N', 1, 1, NOW(), 1, NOW(), 1, 0
FROM (
 SELECT 1 sort, '发布到COS' label, 'publish' value, 'sys_plugin_delivery_action' type, 'info' style
 UNION ALL SELECT 2, '部署', 'deploy', 'sys_plugin_delivery_action', 'primary'
 UNION ALL SELECT 3, '恢复上一版', 'restore', 'sys_plugin_delivery_action', 'warning'
 UNION ALL SELECT 1, '待领取', 'queued', 'sys_plugin_delivery_status', 'info'
 UNION ALL SELECT 2, '执行中', 'running', 'sys_plugin_delivery_status', 'primary'
 UNION ALL SELECT 3, '核验成功', 'succeeded', 'sys_plugin_delivery_status', 'success'
 UNION ALL SELECT 4, '执行失败', 'failed', 'sys_plugin_delivery_status', 'error'
 UNION ALL SELECT 5, '结果待核查', 'uncertain', 'sys_plugin_delivery_status', 'warning'
 UNION ALL SELECT 6, '人工关闭', 'reconciled', 'sys_plugin_delivery_status', 'info'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data d
 WHERE d.tenant_id=1 AND d.dict_type=seed.type AND d.dict_value=seed.value AND d.del_flag=0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, api_method, api_url, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '查询交付目标与记录', m.id, 4, 24, 0, 1, 1, 'system:plugin:delivery:list', 'GET', '/system/plugin-delivery/*',
 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=4 AND (r.perms='system:plugin:delivery:list'
 OR (r.api_method='GET' AND r.api_url='/system/plugin-delivery/*')));

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, api_method, api_url, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '确认交付任务', m.id, 4, 24, 0, 1, 1, 'system:plugin:delivery:execute', 'POST', '/system/plugin-delivery/add',
 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=4 AND (r.perms='system:plugin:delivery:execute'
 OR (r.api_method='POST' AND r.api_url='/system/plugin-delivery/add')));

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, api_method, api_url, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '人工关闭交付', m.id, 4, 24, 0, 1, 1, 'system:plugin:delivery:reconcile', 'POST', '/system/plugin-delivery/*/reconcile',
 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=4 AND (r.perms='system:plugin:delivery:reconcile'
 OR (r.api_method='POST' AND r.api_url='/system/plugin-delivery/*/reconcile')));

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '交付查询', m.id, 3, 25, 0, 1, 1,
 'system:plugin:delivery:list', 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=3 AND r.perms='system:plugin:delivery:list');

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '交付执行', m.id, 3, 25, 0, 1, 1,
 'system:plugin:delivery:execute', 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=3 AND r.perms='system:plugin:delivery:execute');

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_public,
 menu_status, visible, perms, create_by, create_time, update_by, update_time,
 create_dept, client_code, min_user_type, del_flag)
SELECT 1, '交付核查', m.id, 3, 25, 0, 1, 1,
 'system:plugin:delivery:reconcile', 1, NOW(), 1, NOW(), 1, 'pc', 0, 0 FROM sys_resource m
WHERE m.tenant_id=1 AND m.client_code='pc' AND m.resource_type=2 AND m.del_flag=0
 AND m.perms='system:plugin:view' AND m.path='/system/plugin'
 AND NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.tenant_id=1 AND r.del_flag=0
 AND r.client_code='pc' AND r.resource_type=3 AND r.perms='system:plugin:delivery:reconcile');
