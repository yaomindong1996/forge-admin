-- 修正应用中心菜单结构和可点击入口。
-- V1.0.27 将“应用中心”建成了带子菜单的菜单资源，父菜单点击行为不稳定；
-- 这里补充目录节点，并把首页、引擎、移动、集成入口挂到目录下。

SET @business_app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '应用中心', 0, 1, 6, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, NULL, 'ionicons5:GridOutline',
       NULL, NULL, 0, 1, NULL, '企业应用装配平台目录', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '应用中心'
);

SET @business_app_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '应用总览', @business_app_root_id, 2, 1, '/app-center', 'app-center/index', 0,
       0, NULL, '_self', 0, 1, 1, 'ai:businessApp:list', 'ionicons5:GridOutline',
       NULL, NULL, 1, 0, NULL, '企业应用装配平台主入口', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @business_app_root_id IS NOT NULL
  AND @business_app_overview_id IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:businessApp:list'
  );

SET @business_app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  LIMIT 1
);

UPDATE sys_resource
SET resource_name = '应用总览',
    parent_id = @business_app_root_id,
    sort = 1,
    path = '/app-center',
    component = 'app-center/index',
    menu_status = 1,
    visible = 1,
    icon = 'ionicons5:GridOutline',
    keep_alive = 1,
    always_show = 0,
    redirect = NULL,
    remark = '企业应用装配平台主入口',
    update_by = 1,
    update_time = NOW()
WHERE @business_app_root_id IS NOT NULL
  AND @business_app_overview_id IS NOT NULL
  AND id = @business_app_overview_id;

UPDATE sys_resource
SET parent_id = @business_app_root_id,
    menu_status = 1,
    visible = 1,
    path = CASE perms
             WHEN 'ai:businessEngine:list' THEN '/app-center/engines'
             WHEN 'ai:businessMobile:list' THEN '/app-center/mobile'
             WHEN 'ai:businessIntegration:list' THEN '/app-center/integration'
             ELSE path
           END,
    component = CASE perms
                  WHEN 'ai:businessEngine:list' THEN 'app-center/engines'
                  WHEN 'ai:businessMobile:list' THEN 'app-center/mobile'
                  WHEN 'ai:businessIntegration:list' THEN 'app-center/integration'
                  ELSE component
                END,
    update_by = 1,
    update_time = NOW()
WHERE @business_app_root_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND perms IN (
    'ai:businessEngine:list',
    'ai:businessMobile:list',
    'ai:businessIntegration:list'
  );

UPDATE sys_resource
SET parent_id = @business_app_overview_id,
    menu_status = 0,
    visible = 0,
    path = CASE perms
             WHEN 'ai:businessSuite:list' THEN '/app-center/suite/:suiteCode'
             WHEN 'ai:businessObject:list' THEN '/app-center/object/:objectCode'
             ELSE path
           END,
    component = CASE perms
                  WHEN 'ai:businessSuite:list' THEN 'app-center/suite/[suiteCode]'
                  WHEN 'ai:businessObject:list' THEN 'app-center/object/[objectCode]'
                  ELSE component
                END,
    update_by = 1,
    update_time = NOW()
WHERE @business_app_overview_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND perms IN (
    'ai:businessSuite:list',
    'ai:businessObject:list'
  );

UPDATE sys_resource
SET parent_id = @business_app_overview_id,
    update_by = 1,
    update_time = NOW()
WHERE @business_app_overview_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN (
    'ai:businessSuite:list',
    'ai:businessSuite:edit',
    'ai:businessObject:list',
    'ai:businessObject:add',
    'ai:businessObject:edit',
    'ai:businessObject:delete',
    'ai:businessObject:relation',
    'ai:businessBinding:config',
    'ai:businessApp:add',
    'ai:businessApp:edit',
    'ai:businessApp:delete',
    'ai:businessApp:status',
    'ai:businessApp:open'
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, @business_app_root_id, NOW()
FROM sys_role_resource rr
WHERE @business_app_root_id IS NOT NULL
  AND @business_app_overview_id IS NOT NULL
  AND rr.resource_id = @business_app_overview_id
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @business_app_root_id
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, @business_app_overview_id, NOW()
FROM sys_role_resource rr
WHERE @business_app_root_id IS NOT NULL
  AND @business_app_overview_id IS NOT NULL
  AND rr.resource_id = @business_app_root_id
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @business_app_overview_id
  );
