-- 业务对象设计器入口、按钮权限和开发者菜单隔离。

SET @business_app_overview_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessApp:list'
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '业务对象设计器', @business_app_overview_id, 2, 6, '/app-center/object/:objectCode/designer',
       'app-center/object-designer.[objectCode]', 0,
       0, NULL, '_self', 0, 0, 0, 'ai:businessObject:design', 'ionicons5:BuildOutline',
       NULL, NULL, 0, 0, NULL, '业务对象设计器隐藏路由', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @business_app_overview_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:businessObject:design'
      AND path = '/app-center/object/:objectCode/designer'
  );

UPDATE sys_resource
SET parent_id = @business_app_overview_id,
    resource_name = '业务对象设计器',
    path = '/app-center/object/:objectCode/designer',
    component = 'app-center/object-designer.[objectCode]',
    menu_status = 0,
    visible = 0,
    icon = 'ionicons5:BuildOutline',
    remark = '业务对象设计器隐藏路由',
    update_by = 1,
    update_time = NOW()
WHERE @business_app_overview_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND perms = 'ai:businessObject:design';

SET @business_object_designer_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND perms = 'ai:businessObject:design'
    AND path = '/app-center/object/:objectCode/designer'
  ORDER BY id
  LIMIT 1
);

SET @business_object_permission_parent_id := COALESCE(@business_object_designer_id, @business_app_overview_id);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT seed.tenant_id, seed.resource_name, @business_object_permission_parent_id, 3, seed.sort, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, NULL,
       NULL, NULL, 0, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT 1 tenant_id, '设计业务对象' resource_name, 26 sort, 'ai:businessObject:design' perms, '进入和保存业务对象设计器' remark
  UNION ALL SELECT 1, '维护业务字段', 27, 'ai:businessObject:field', '新增、编辑、删除和排序业务字段'
  UNION ALL SELECT 1, '维护对象布局', 28, 'ai:businessObject:layout', '维护表单、列表和详情布局'
  UNION ALL SELECT 1, '配置对象操作', 29, 'ai:businessObject:action', '配置工具栏、行和详情自定义操作'
  UNION ALL SELECT 1, '发布业务对象', 30, 'ai:businessObject:publish', '执行发布检查并发布业务对象'
  UNION ALL SELECT 1, '高级对象配置', 31, 'ai:businessObject:advanced', '查看模型、表名、Schema、DDL和开发者入口'
) seed
WHERE @business_object_permission_parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = seed.tenant_id
      AND r.resource_type = 3
      AND r.perms = seed.perms
  );

UPDATE sys_resource
SET parent_id = @business_object_permission_parent_id,
    sort = CASE perms
             WHEN 'ai:businessObject:relation' THEN 25
             WHEN 'ai:businessObject:design' THEN 26
             WHEN 'ai:businessObject:field' THEN 27
             WHEN 'ai:businessObject:layout' THEN 28
             WHEN 'ai:businessObject:action' THEN 29
             WHEN 'ai:businessObject:publish' THEN 30
             WHEN 'ai:businessObject:advanced' THEN 31
             ELSE sort
           END,
    resource_name = CASE perms
                      WHEN 'ai:businessObject:relation' THEN '配置对象关系'
                      WHEN 'ai:businessObject:design' THEN '设计业务对象'
                      WHEN 'ai:businessObject:field' THEN '维护业务字段'
                      WHEN 'ai:businessObject:layout' THEN '维护对象布局'
                      WHEN 'ai:businessObject:action' THEN '配置对象操作'
                      WHEN 'ai:businessObject:publish' THEN '发布业务对象'
                      WHEN 'ai:businessObject:advanced' THEN '高级对象配置'
                      ELSE resource_name
                    END,
    menu_status = 1,
    visible = 1,
    update_by = 1,
    update_time = NOW()
WHERE @business_object_permission_parent_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN (
    'ai:businessObject:relation',
    'ai:businessObject:design',
    'ai:businessObject:field',
    'ai:businessObject:layout',
    'ai:businessObject:action',
    'ai:businessObject:publish',
    'ai:businessObject:advanced'
  );

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
                          sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
                          api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
                          update_by, update_time, create_dept, client_code)
SELECT 1, '开发者工具', 0, 1, 88, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, NULL, 'ionicons5:CodeSlashOutline',
       NULL, NULL, 0, 1, NULL, '低代码、模型设计、数据源和代码输出等开发者入口', 1, NOW(),
       1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '开发者工具'
);

SET @developer_root_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '开发者工具'
  ORDER BY id
  LIMIT 1
);

UPDATE sys_resource
SET parent_id = @developer_root_id,
    menu_status = 1,
    visible = 1,
    update_by = 1,
    update_time = NOW()
WHERE @developer_root_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND (
    perms IN ('ai:lowcode:domain:list', 'ai:lowcode:model:list', 'ai:datasource:list', 'gen:datasource:list')
    OR path IN ('/ai/lowcode-apps', '/ai/lowcode-models', '/generator/datasource')
    OR component IN ('ai/lowcode-apps', 'ai/lowcode-models', 'generator/datasource')
  );

UPDATE sys_resource
SET menu_status = 0,
    visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND (
    path IN ('/ai/crud-config', '/ai/crud-generator', '/ai/page-template', '/generator/table', '/generator/template')
    OR component IN ('ai/crud-config', 'ai/crud-generator', 'ai/page-template', 'generator/table', 'generator/template')
    OR perms IN ('ai:crud-config:list', 'ai:crud-gen:list', 'ai:crud-generator:use', 'ai:page-template:list')
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, target.id, NOW()
FROM sys_role_resource rr
JOIN sys_resource src ON src.id = rr.resource_id
JOIN sys_resource target ON target.tenant_id = 1
WHERE src.tenant_id = 1
  AND src.perms IN ('ai:businessObject:list', 'ai:businessObject:add', 'ai:businessObject:edit', 'ai:businessObject:relation')
  AND target.perms IN (
    'ai:businessObject:design',
    'ai:businessObject:field',
    'ai:businessObject:layout',
    'ai:businessObject:relation',
    'ai:businessObject:action',
    'ai:businessObject:publish'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = target.id
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, target.id, NOW()
FROM sys_role_resource rr
JOIN sys_resource src ON src.id = rr.resource_id
JOIN sys_resource target ON target.tenant_id = 1
WHERE src.tenant_id = 1
  AND (
    src.perms LIKE 'ai:lowcode:%'
    OR src.perms IN ('ai:crud-config:list', 'ai:crud-gen:list', 'ai:crud-generator:use', 'ai:page-template:list', 'ai:datasource:list', 'gen:datasource:list')
    OR src.path IN ('/ai/lowcode-apps', '/ai/lowcode-models', '/generator/datasource')
  )
  AND target.perms = 'ai:businessObject:advanced'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = target.id
  );
