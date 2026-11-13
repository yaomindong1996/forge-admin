-- 统一能力开放平台：受控系统服务发布权限。
-- 系统服务只允许从代码注册表选择，不提供任意 URL、Bean 或 SQL 配置入口。

SET @capability_catalog_id := COALESCE(
  (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND path = '/open-platform/capability-catalog'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
  ),
  0
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '发布受控系统服务', @capability_catalog_id, 3, 34,
       0, '_self', 0, 1, 1, 'ai:capability:system-service:publish',
       0, 0, '从代码注册表选择系统服务并发布不可变能力版本',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource resource
  WHERE resource.tenant_id = 1
    AND resource.perms = 'ai:capability:system-service:publish'
    AND resource.del_flag = 0
);

INSERT INTO sys_role_resource (
  tenant_id, role_id, resource_id, create_time
)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (
  SELECT id
  FROM sys_role
  WHERE tenant_id = 1
    AND role_key = 'admin'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
) admin_role
JOIN sys_resource resource
  ON resource.tenant_id = 1
 AND resource.perms = 'ai:capability:system-service:publish'
 AND resource.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = admin_role.id
    AND existing.resource_id = resource.id
);
