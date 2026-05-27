-- 规范应用中心菜单目录和可见子菜单，避免目录节点残留页面路径后抢占选中状态。
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

SET @business_app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  ORDER BY CASE WHEN parent_id = @business_app_root_id THEN 0 ELSE 1 END, id
  LIMIT 1
);

UPDATE sys_resource
SET path = NULL,
    component = NULL,
    perms = NULL,
    menu_status = 1,
    visible = 1,
    always_show = 1,
    update_by = 1,
    update_time = NOW()
WHERE @business_app_root_id IS NOT NULL
  AND id = @business_app_root_id
  AND tenant_id = 1
  AND resource_type = 1;

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
  AND id = @business_app_overview_id
  AND tenant_id = 1;

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @business_app_root_id, 2, seed.sort, seed.path, seed.component, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, seed.icon,
       NULL, NULL, 1, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '引擎中心' resource_name, 3 sort, '/app-center/engines' path, 'app-center/engines' component, 'ai:businessEngine:list' perms, 'ionicons5:HardwareChipOutline' icon, '流程、审批、报表、权限和消息引擎统一入口' remark
  UNION ALL SELECT 1, '移动端中心', 4, '/app-center/mobile', 'app-center/mobile', 'ai:businessMobile:list', 'ionicons5:PhonePortraitOutline', '移动H5和移动业务入口'
  UNION ALL SELECT 1, '集成中心', 5, '/app-center/integration', 'app-center/integration', 'ai:businessIntegration:list', 'ionicons5:GitNetworkOutline', '开放接口、Webhook和第三方平台连接入口'
) seed
WHERE @business_app_root_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = 2
      AND r.perms = seed.perms
  );

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
    update_by = 1,
    update_time = NOW()
WHERE @business_app_overview_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND perms IN (
    'ai:businessSuite:list',
    'ai:businessObject:list'
  );
