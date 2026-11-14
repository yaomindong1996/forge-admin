-- Register the permission for standalone business process publishing.
-- Standalone publish creates an immutable process version and switches the runtime projection
-- without creating an application publish run.

SET @process_designer_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/business-process/:processId'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, '发布业务流程', @process_designer_menu_id, 3, 16,
       0, '_self', 0, 1, 1, 'ai:businessProcess:publish',
       0, 0, '独立发布业务流程版本并切换运行投影，不创建应用发布运行单',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @process_designer_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource_row
    WHERE resource_row.tenant_id = 1
      AND resource_row.perms = 'ai:businessProcess:publish'
      AND resource_row.del_flag = 0
  );

-- Grant the publish permission to roles that already manage process drafts.
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, publish_resource.id, NOW()
FROM sys_resource old_resource
INNER JOIN sys_role_resource old_role_resource
  ON old_role_resource.tenant_id = 1
 AND old_role_resource.resource_id = old_resource.id
INNER JOIN sys_resource publish_resource
  ON publish_resource.tenant_id = 1
 AND publish_resource.perms = 'ai:businessProcess:publish'
 AND publish_resource.del_flag = 0
WHERE old_resource.tenant_id = 1
  AND old_resource.perms = 'ai:businessProcess:edit'
  AND old_resource.del_flag = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing_row
    WHERE existing_row.tenant_id = 1
      AND existing_row.role_id = old_role_resource.role_id
      AND existing_row.resource_id = publish_resource.id
  );
