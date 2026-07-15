-- 应用级完整代码包权限；仅继承已有应用编辑权限，避免向只读角色暴露源码。

SET @application_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center'
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
SELECT 1, seed.resource_name, @application_menu_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '配置应用代码包' resource_name, 81 sort, 'ai:businessApplication:code' perms,
         '查看和保存应用代码包设置' remark
  UNION ALL SELECT '预览应用代码', 82, 'ai:businessApplication:codePreview', '预览应用完整前后端代码'
  UNION ALL SELECT '下载应用代码', 83, 'ai:businessApplication:codeDownload', '批量下载应用完整代码包'
) seed
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource resource
    WHERE resource.tenant_id = 1
      AND resource.perms = seed.perms
      AND resource.del_flag = 0
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, new_resource.id, NOW()
FROM (
  SELECT 'ai:businessApplication:edit' old_perms, 'ai:businessApplication:code' new_perms
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessApplication:codePreview'
  UNION ALL SELECT 'ai:businessApplication:edit', 'ai:businessApplication:codeDownload'
) permission_mapping
INNER JOIN sys_resource old_resource
  ON old_resource.tenant_id = 1
 AND old_resource.perms = permission_mapping.old_perms
 AND old_resource.del_flag = 0
INNER JOIN sys_role_resource old_role_resource
  ON old_role_resource.tenant_id = 1
 AND old_role_resource.resource_id = old_resource.id
INNER JOIN sys_resource new_resource
  ON new_resource.tenant_id = 1
 AND new_resource.perms = permission_mapping.new_perms
 AND new_resource.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = old_role_resource.role_id
    AND existing.resource_id = new_resource.id
);
