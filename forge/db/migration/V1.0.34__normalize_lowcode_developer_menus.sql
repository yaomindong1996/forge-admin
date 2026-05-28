-- 将低代码开发、模型、数据源和代码生成入口收敛到“开发者工具”目录。
-- 业务用户默认从“应用中心”进入，历史路由保留，纯 JSON / 旧代码生成入口继续隐藏。

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
    sort = CASE perms
             WHEN 'ai:lowcode:domain:list' THEN 1
             WHEN 'ai:lowcode:model:list' THEN 2
             WHEN 'ai:datasource:list' THEN 3
             WHEN 'gen:datasource:list' THEN 3
             ELSE sort
           END,
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
    remark = CONCAT(COALESCE(remark, ''), '；已收敛到开发者工具或隐藏普通菜单入口'),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND (
    path IN ('/ai/crud-config', '/ai/crud-generator', '/ai/page-template', '/generator/table', '/generator/template')
    OR component IN ('ai/crud-config', 'ai/crud-generator', 'ai/page-template', 'generator/table', 'generator/template')
    OR perms IN ('ai:crud-config:list', 'ai:crud-gen:list', 'ai:crud-generator:use', 'ai:page-template:list')
  );

UPDATE sys_resource
SET parent_id = COALESCE(
      CASE
        WHEN perms IN (
          'ai:lowcode:domain:add',
          'ai:lowcode:domain:edit',
          'ai:lowcode:domain:status',
          'ai:lowcode:domain:move',
          'ai:lowcode:publish',
          'ai:lowcode:rollback',
          'ai:lowcode:deploy-ddl',
          'ai:lowcode:ai-generate',
          'ai:lowcode:code-preview',
          'ai:lowcode:code-download'
        ) THEN (
          SELECT id
          FROM (
            SELECT id
            FROM sys_resource
            WHERE tenant_id = 1
              AND resource_type = 2
              AND perms = 'ai:lowcode:domain:list'
            LIMIT 1
          ) app_menu
        )
        WHEN perms IN (
          'ai:lowcode:model:add',
          'ai:lowcode:model:edit',
          'ai:lowcode:model:status',
          'ai:lowcode:model:import-table',
          'ai:lowcode:model:ai-generate'
        ) THEN (
          SELECT id
          FROM (
            SELECT id
            FROM sys_resource
            WHERE tenant_id = 1
              AND resource_type = 2
              AND perms = 'ai:lowcode:model:list'
            LIMIT 1
          ) model_menu
        )
        ELSE parent_id
      END,
      parent_id
    ),
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 3
  AND perms LIKE 'ai:lowcode:%';

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT rr.tenant_id, rr.role_id, @developer_root_id, NOW()
FROM sys_role_resource rr
JOIN sys_resource r ON r.id = rr.resource_id
WHERE @developer_root_id IS NOT NULL
  AND r.tenant_id = 1
  AND (
    r.perms LIKE 'ai:lowcode:%'
    OR r.perms IN ('ai:datasource:list', 'gen:datasource:list')
    OR r.path IN ('/ai/lowcode-apps', '/ai/lowcode-models', '/generator/datasource')
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_resource exists_rr
    WHERE exists_rr.tenant_id = rr.tenant_id
      AND exists_rr.role_id = rr.role_id
      AND exists_rr.resource_id = @developer_root_id
  );
