-- 将打印中心从应用总览的历史隐藏路由中拆出，恢复两个模块的独立菜单边界。
-- 回滚：保留资源与角色授权，仅将本脚本新增的打印目录及菜单设为不可见；不删除打印配置数据。

SET @app_center_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '应用中心'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

SET @app_overview_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 2
      AND perms = 'ai:businessApp:list'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

-- V1.0.204 曾把应用总览下的 /print 隐藏路由直接设为可见；先确保应用总览自身保持原结构。
UPDATE sys_resource
SET resource_name = '应用总览',
    parent_id = @app_center_dir_id,
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
WHERE @app_center_dir_id IS NOT NULL
  AND @app_overview_id IS NOT NULL
  AND id = @app_overview_id;

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT 1, '打印中心', 0, 1, 7, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, NULL, 'ionicons5:PrintOutline',
       NULL, NULL, 0, 1, NULL, '平台级打印模板、业务数据源与场景绑定',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '打印中心'
      AND client_code = 'pc'
      AND del_flag = 0
);

SET @print_center_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '打印中心'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

-- 复用原 /print 资源，保留既有角色授权，但改成打印中心下的模板管理入口。
UPDATE sys_resource
SET resource_name = '打印模板',
    parent_id = @print_center_dir_id,
    sort = 2,
    path = '/print/templates',
    component = 'print/index',
    menu_status = 1,
    visible = 1,
    perms = 'print:template:view',
    icon = 'ionicons5:DocumentTextOutline',
    keep_alive = 1,
    always_show = 0,
    redirect = NULL,
    remark = '平台级打印模板管理',
    update_by = 1,
    update_time = NOW()
WHERE @print_center_dir_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND path IN ('/print', '/print/templates')
  AND client_code = 'pc'
  AND del_flag = 0;

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT seed.tenant_id, seed.resource_name, @print_center_dir_id, 2, seed.sort, seed.path, 'print/index', 0,
       0, NULL, '_self', 0, 1, 1, seed.perms, seed.icon,
       NULL, NULL, 1, 0, NULL, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
    SELECT 1 tenant_id, '业务数据源' resource_name, 1 sort, '/print/sources' path,
           'print:source:view' perms, 'ionicons5:ServerOutline' icon, '打印业务数据源接入与配置' remark
    UNION ALL
    SELECT 1, '场景绑定', 3, '/print/bindings',
           'print:template:manage', 'ionicons5:LinkOutline', '将已发布模板绑定到列表、详情或流程场景'
) seed
WHERE @print_center_dir_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource resource_row
      WHERE resource_row.tenant_id = seed.tenant_id
        AND resource_row.resource_type = 2
        AND resource_row.path = seed.path
        AND resource_row.client_code = 'pc'
        AND resource_row.del_flag = 0
  );

SET @print_source_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 2 AND path = '/print/sources'
      AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);
SET @print_template_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 2 AND path = '/print/templates'
      AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);
SET @print_binding_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 2 AND path = '/print/bindings'
      AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);

-- 权限节点归到对应页面，避免继续残留在应用总览或错误的模板页面下面。
UPDATE sys_resource
SET parent_id = @print_source_menu_id, update_by = 1, update_time = NOW()
WHERE @print_source_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN ('print:source:view', 'print:source:manage')
  AND client_code = 'pc'
  AND del_flag = 0;

UPDATE sys_resource
SET parent_id = @print_template_menu_id, update_by = 1, update_time = NOW()
WHERE @print_template_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms IN ('print:template:view', 'print:template:publish', 'print:execute')
  AND client_code = 'pc'
  AND del_flag = 0;

UPDATE sys_resource
SET parent_id = @print_binding_menu_id, update_by = 1, update_time = NOW()
WHERE @print_binding_menu_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 3
  AND perms = 'print:template:manage'
  AND client_code = 'pc'
  AND del_flag = 0;

-- 设计器和预览是业务跳转路由，不作为侧边栏菜单展示。
UPDATE sys_resource
SET parent_id = @print_center_dir_id,
    menu_status = 1,
    visible = 0,
    update_by = 1,
    update_time = NOW()
WHERE @print_center_dir_id IS NOT NULL
  AND tenant_id = 1
  AND resource_type = 2
  AND path IN ('/print/designer', '/print/preview')
  AND client_code = 'pc'
  AND del_flag = 0;

-- 只继承现有打印权限，不给原本无打印权限的角色扩权。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT role_permission.tenant_id, role_permission.role_id, target.resource_id, NOW()
FROM (
    SELECT role_resource.tenant_id, role_resource.role_id, resource_row.perms
    FROM sys_role_resource role_resource
    INNER JOIN sys_resource resource_row
        ON resource_row.id = role_resource.resource_id
       AND resource_row.tenant_id = role_resource.tenant_id
       AND resource_row.client_code = 'pc'
       AND resource_row.del_flag = 0
    WHERE resource_row.perms IN (
        'print:source:view', 'print:source:manage', 'print:template:view',
        'print:template:manage', 'print:template:publish', 'print:execute'
    )
) role_permission
INNER JOIN (
    SELECT @print_center_dir_id resource_id, NULL required_perm
    UNION ALL SELECT @print_source_menu_id, 'print:source:view'
    UNION ALL SELECT @print_source_menu_id, 'print:source:manage'
    UNION ALL SELECT @print_template_menu_id, 'print:template:view'
    UNION ALL SELECT @print_template_menu_id, 'print:template:manage'
    UNION ALL SELECT @print_template_menu_id, 'print:template:publish'
    UNION ALL SELECT @print_binding_menu_id, 'print:template:manage'
) target
    ON target.resource_id IS NOT NULL
   AND (target.required_perm IS NULL OR target.required_perm = role_permission.perms)
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = role_permission.tenant_id
      AND existing.role_id = role_permission.role_id
      AND existing.resource_id = target.resource_id
);

-- 原来已持有 /print 页面资源的角色继续拥有新的打印中心目录。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT existing.tenant_id, existing.role_id, @print_center_dir_id, NOW()
FROM sys_role_resource existing
WHERE @print_center_dir_id IS NOT NULL
  AND @print_template_menu_id IS NOT NULL
  AND existing.resource_id = @print_template_menu_id
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_resource assigned
      WHERE assigned.tenant_id = existing.tenant_id
        AND assigned.role_id = existing.role_id
        AND assigned.resource_id = @print_center_dir_id
  );
