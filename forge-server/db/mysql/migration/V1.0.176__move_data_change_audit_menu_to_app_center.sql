-- 数据变更审计是低代码应用能力，菜单从系统监控改挂到应用中心。

SET @app_center_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND del_flag = 0
    AND path = '/app-center'
    AND resource_type IN (1, 2)
  ORDER BY resource_type DESC, id
  LIMIT 1
);

UPDATE sys_resource
SET parent_id = @app_center_menu_id,
    path = '/app-center/data-change-audit',
    component = 'app-center/data-change-audit',
    resource_name = '数据变更审计',
    remark = '低代码应用字段级数据变更审计',
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND del_flag = 0
  AND path IN ('/system/data-change-audit', '/app-center/data-change-audit')
  AND @app_center_menu_id IS NOT NULL;

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '数据变更审计', @app_center_menu_id, 2, 80, '/app-center/data-change-audit', 'app-center/data-change-audit', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:dataAudit:list', 'ionicons5:DocumentsOutline',
       NULL, NULL, 1, 0, NULL, '低代码应用字段级数据变更审计', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @app_center_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND path = '/app-center/data-change-audit'
      AND del_flag = 0
  );

SET @data_audit_menu_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center/data-change-audit'
    AND del_flag = 0
  LIMIT 1
);

UPDATE sys_resource
SET parent_id = @data_audit_menu_id,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type IN (3, 4)
  AND del_flag = 0
  AND perms LIKE 'ai:dataAudit:%'
  AND @data_audit_menu_id IS NOT NULL;
