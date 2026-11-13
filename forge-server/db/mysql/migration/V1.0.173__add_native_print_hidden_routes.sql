-- M3b 隐藏打印页面；不自动授权角色，不执行运行数据迁移。
-- 回滚：解除角色关联后将本脚本三个 path 对应的资源按 id 逻辑删除；保留四项打印 API 权限。
SET @print_application_menu_id = (SELECT id FROM sys_resource WHERE tenant_id = 1 AND path = '/app-center' AND resource_type = 2 AND del_flag = 0 ORDER BY id LIMIT 1);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '打印模板', @print_application_menu_id, 2, 95, '/print', 'print/index', 0, '_self', 0, 1, 0, NULL, 0, 0, '打印隐藏页面；授权仍以打印权限、应用及来源权限为准', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND path = '/print' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '打印模板设计', @print_application_menu_id, 2, 96, '/print/designer', 'print/designer', 0, '_self', 0, 1, 0, NULL, 0, 0, '打印隐藏页面；授权仍以打印权限、应用及来源权限为准', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND path = '/print/designer' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, open_target, is_public, menu_status, visible, perms, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code)
SELECT 1, '单据打印', @print_application_menu_id, 2, 97, '/print/preview', 'print/preview', 0, '_self', 0, 1, 0, NULL, 0, 0, '打印隐藏页面；授权仍以打印权限、应用及来源权限为准', 1, NOW(), 1, NOW(), 1, 'pc'
WHERE @print_application_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND path = '/print/preview' AND del_flag = 0);
