-- 幂等运行回执，无业务表单内容，不参与逻辑删除；与主库建单保持原子事务。
CREATE TABLE IF NOT EXISTS ai_capability_form_receipt (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL DEFAULT 1,
  client_id BIGINT NOT NULL,
  capability_id BIGINT NOT NULL,
  key_hash CHAR(64) NOT NULL,
  request_digest CHAR(64) NOT NULL,
  record_id VARCHAR(128) NOT NULL DEFAULT '',
  create_by BIGINT NOT NULL,
  create_dept BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by BIGINT NOT NULL,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cap_form_receipt (tenant_id, client_id, capability_id, key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台表单填报幂等回执';

-- 只增加可授权资源，不向客户端或普通角色自动授权。
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target,
  is_public, menu_status, visible, perms, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name,
       COALESCE((SELECT id FROM (SELECT id FROM sys_resource WHERE component = 'ai/capability/catalog' AND del_flag = 0 LIMIT 1) parent), 0),
       3, seed.sort, 0, '_self', 0, 1, 1, seed.perms, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '调用已授权系统能力' resource_name, 40 sort, 'ai:capability:system-service:invoke' perms,
         '系统REST及低代码表单能力调用门槛，仍须具体业务权限和客户端授权' remark
  UNION ALL
  SELECT '外部读取字典选项', 41, 'ai:capability:dictionary:read', '仅开放当前租户的字典选项读取，不包含字典维护权限'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_resource r WHERE r.perms = seed.perms AND r.del_flag = 0);

-- 只调整内置菜单展示名；保留自定义名称、路由、授权和资源 ID。
UPDATE sys_resource
SET resource_name = '接入系统', update_by = 1, update_time = NOW()
WHERE component = 'ai/capability/client' AND resource_name = '机器客户端' AND del_flag = 0;
