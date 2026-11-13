-- 数据变更审计与「应用总览」平级，挂在「应用中心」目录下，不要作为总览的子菜单。

SET @app_center_dir_id := (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND del_flag = 0
    AND resource_type = 1
    AND parent_id = 0
    AND resource_name = '应用中心'
  ORDER BY id
  LIMIT 1
);

UPDATE sys_resource
SET parent_id = @app_center_dir_id,
    path = '/app-center/data-change-audit',
    component = 'app-center/data-change-audit',
    resource_name = '数据变更审计',
    sort = 80,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND del_flag = 0
  AND path IN ('/system/data-change-audit', '/app-center/data-change-audit')
  AND @app_center_dir_id IS NOT NULL;
