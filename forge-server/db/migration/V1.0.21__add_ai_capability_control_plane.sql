-- Forge AI 中枢阶段 1 控制面：能力、版本、机器客户端、授权和安全调用日志。

CREATE TABLE IF NOT EXISTS ai_capability (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  capability_code varchar(128) NOT NULL,
  protocol_tool_name varchar(128) NOT NULL,
  capability_name varchar(128) NOT NULL,
  description varchar(1000) NOT NULL,
  source_type varchar(32) NOT NULL,
  source_key varchar(128) NOT NULL,
  source_version varchar(64) NOT NULL,
  current_version varchar(32) NOT NULL,
  schema_checksum char(64) NOT NULL,
  behavior varchar(24) NOT NULL,
  risk_level varchar(16) NOT NULL,
  visibility varchar(16) NOT NULL DEFAULT 'PRIVATE',
  publish_status varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  enabled tinyint NOT NULL DEFAULT 1,
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_code_active (tenant_id, capability_code, logic_delete_active),
  UNIQUE KEY uk_ai_capability_tool_active (tenant_id, protocol_tool_name, logic_delete_active),
  KEY idx_ai_capability_source (tenant_id, source_type, source_key, del_flag),
  KEY idx_ai_capability_publish (tenant_id, publish_status, enabled, del_flag, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢能力目录';

CREATE TABLE IF NOT EXISTS ai_capability_version (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  capability_id bigint NOT NULL,
  version varchar(32) NOT NULL,
  input_schema json NOT NULL,
  output_schema json NOT NULL,
  source_type varchar(32) NOT NULL,
  source_key varchar(128) NOT NULL,
  source_version varchar(64) NOT NULL,
  behavior varchar(24) NOT NULL,
  risk_level varchar(16) NOT NULL,
  visibility varchar(16) NOT NULL,
  policy_snapshot json DEFAULT NULL,
  schema_checksum char(64) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_version_active (tenant_id, capability_id, version, logic_delete_active),
  KEY idx_ai_capability_version_checksum (tenant_id, capability_id, schema_checksum, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢能力不可变版本';

CREATE TABLE IF NOT EXISTS ai_capability_client (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  client_code varchar(64) NOT NULL,
  client_name varchar(128) NOT NULL,
  key_id varchar(22) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  key_prefix varchar(32) NOT NULL,
  key_hash char(64) NOT NULL,
  credential_version int NOT NULL DEFAULT 1,
  service_user_id bigint NOT NULL,
  active_org_id bigint NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'ENABLED',
  expires_at datetime DEFAULT NULL,
  last_used_at datetime DEFAULT NULL,
  remark varchar(500) DEFAULT NULL,
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_client_code_active (tenant_id, client_code, logic_delete_active),
  UNIQUE KEY uk_ai_capability_client_key_active (key_id, logic_delete_active),
  KEY idx_ai_capability_client_status (tenant_id, status, expires_at, del_flag),
  KEY idx_ai_capability_client_user_org (tenant_id, service_user_id, active_org_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢机器客户端';

CREATE TABLE IF NOT EXISTS ai_capability_grant (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  client_id bigint NOT NULL,
  capability_id bigint NOT NULL,
  version_strategy varchar(16) NOT NULL DEFAULT 'PINNED',
  fixed_version varchar(32) DEFAULT NULL,
  field_policy json DEFAULT NULL,
  status varchar(16) NOT NULL DEFAULT 'ENABLED',
  expires_at datetime DEFAULT NULL,
  del_flag tinyint NOT NULL DEFAULT 0,
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = 0 THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_grant_active (tenant_id, client_id, capability_id, logic_delete_active),
  KEY idx_ai_capability_grant_client (tenant_id, client_id, status, expires_at, del_flag),
  KEY idx_ai_capability_grant_capability (tenant_id, capability_id, status, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢客户端能力授权';

CREATE TABLE IF NOT EXISTS ai_capability_invocation_log (
  id bigint NOT NULL,
  tenant_id bigint NOT NULL DEFAULT 1,
  request_id varchar(64) NOT NULL,
  client_id bigint DEFAULT NULL,
  client_code varchar(64) DEFAULT NULL,
  capability_id bigint DEFAULT NULL,
  capability_code varchar(128) NOT NULL,
  capability_version varchar(32) DEFAULT NULL,
  actor_type varchar(16) DEFAULT NULL,
  actor_user_id bigint DEFAULT NULL,
  service_user_id bigint DEFAULT NULL,
  active_org_id bigint DEFAULT NULL,
  result_status varchar(24) NOT NULL,
  result_code varchar(64) NOT NULL,
  error_code varchar(64) DEFAULT NULL,
  schema_path varchar(256) DEFAULT NULL,
  trace_id varchar(64) DEFAULT NULL,
  duration_ms bigint NOT NULL DEFAULT 0,
  del_flag tinyint NOT NULL DEFAULT 0,
  create_by bigint DEFAULT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_dept bigint DEFAULT NULL,
  update_by bigint DEFAULT NULL,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_capability_invocation_request (tenant_id, request_id),
  KEY idx_ai_capability_invocation_client (tenant_id, client_id, create_time, del_flag),
  KEY idx_ai_capability_invocation_actor (tenant_id, actor_user_id, create_time, del_flag),
  KEY idx_ai_capability_invocation_capability (tenant_id, capability_id, create_time, del_flag),
  KEY idx_ai_capability_invocation_result (tenant_id, result_code, create_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI中枢能力安全调用日志';

INSERT INTO sys_dict_type (tenant_id, dict_name, dict_type, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 'AI能力可见性' dict_name, 'ai_capability_visibility' dict_type, '能力目录可见性' remark
  UNION ALL SELECT 1, 'AI能力发布状态', 'ai_capability_publish_status', '能力发布状态'
  UNION ALL SELECT 1, 'AI能力客户端状态', 'ai_capability_client_status', '机器客户端状态'
  UNION ALL SELECT 1, 'AI能力授权状态', 'ai_capability_grant_status', '客户端授权状态'
  UNION ALL SELECT 1, 'AI能力版本策略', 'ai_capability_version_strategy', '授权版本选择策略'
  UNION ALL SELECT 1, 'AI能力风险等级', 'ai_capability_risk_level', '能力风险等级'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_by, create_time, update_by, update_time, create_dept)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type, NULL, seed.list_class, seed.is_default, 1, seed.remark, 1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '私有' dict_label, 'PRIVATE' dict_value, 'ai_capability_visibility' dict_type, 'default' list_class, 'Y' is_default, '仅管理员可见' remark
  UNION ALL SELECT 1, 2, '可发现', 'DISCOVERABLE', 'ai_capability_visibility', 'info', 'N', '同租户管理员可申请授权'
  UNION ALL SELECT 1, 1, '已发布', 'PUBLISHED', 'ai_capability_publish_status', 'success', 'Y', '当前发布版本可用'
  UNION ALL SELECT 1, 2, '已停用', 'DISABLED', 'ai_capability_publish_status', 'error', 'N', '能力停止发现和调用'
  UNION ALL SELECT 1, 1, '启用', 'ENABLED', 'ai_capability_client_status', 'success', 'Y', '客户端可认证'
  UNION ALL SELECT 1, 2, '已吊销', 'REVOKED', 'ai_capability_client_status', 'error', 'N', '客户端凭据已吊销'
  UNION ALL SELECT 1, 1, '启用', 'ENABLED', 'ai_capability_grant_status', 'success', 'Y', '授权有效'
  UNION ALL SELECT 1, 2, '已撤销', 'REVOKED', 'ai_capability_grant_status', 'error', 'N', '授权已撤销'
  UNION ALL SELECT 1, 1, '固定版本', 'PINNED', 'ai_capability_version_strategy', 'info', 'Y', '只允许指定版本'
  UNION ALL SELECT 1, 2, '跟随主版本', 'FOLLOW_MAJOR', 'ai_capability_version_strategy', 'success', 'N', '跟随同主版本最新版本'
  UNION ALL SELECT 1, 1, '低风险', 'LOW', 'ai_capability_risk_level', 'success', 'Y', '元数据和安全只读能力'
  UNION ALL SELECT 1, 2, '中风险', 'MEDIUM', 'ai_capability_risk_level', 'warning', 'N', '需要数据权限的只读能力'
  UNION ALL SELECT 1, 3, '高风险', 'HIGH', 'ai_capability_risk_level', 'error', 'N', '当前阶段禁止授权'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

SET @ai_parent_id := COALESCE(
  (SELECT parent_id FROM (
    SELECT parent_id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/ai/provider-model' AND del_flag = 0
    LIMIT 1
  ) x),
  0
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, seed.resource_name, @ai_parent_id, 3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '查询能力目录' resource_name, 20 sort, 'ai:capability:query' perms, '查询AI中枢能力目录' remark
  UNION ALL SELECT '发布停用能力', 21, 'ai:capability:publish', '发布或停用AI中枢能力'
  UNION ALL SELECT '查询机器客户端', 22, 'ai:capability:client:query', '查询AI中枢机器客户端'
  UNION ALL SELECT '创建机器客户端', 23, 'ai:capability:client:add', '创建机器客户端并一次性展示密钥'
  UNION ALL SELECT '轮换客户端密钥', 24, 'ai:capability:client:rotate', '轮换机器客户端密钥'
  UNION ALL SELECT '吊销机器客户端', 25, 'ai:capability:client:revoke', '吊销机器客户端'
  UNION ALL SELECT '查询能力授权', 26, 'ai:capability:grant:query', '查询客户端能力授权'
  UNION ALL SELECT '新增能力授权', 27, 'ai:capability:grant:add', '新增客户端能力授权'
  UNION ALL SELECT '撤销能力授权', 28, 'ai:capability:grant:revoke', '撤销客户端能力授权'
  UNION ALL SELECT '查询能力调用日志', 29, 'ai:capability:invocation:query', '查询安全能力调用日志'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.perms = seed.perms AND r.del_flag = 0
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' AND del_flag = 0 ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms LIKE 'ai:capability:%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );
