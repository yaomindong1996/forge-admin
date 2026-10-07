-- 使用 hello 独立迁移历史，不登记进主库 migration；不自动授予任何角色权限。
INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component,
    is_external, open_target, is_public, menu_status, visible, perms, feature_code, icon,
    keep_alive, always_show, remark, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, '示例插件', 0, 2, 900, '/plugins/hello', 'plugins/hello/index',
       0, '_self', 0, 1, 1, 'plugin:hello:view', NULL, 'i-lucide:puzzle',
       0, 0, '社区源码插件示例，普通用户需显式授权菜单和接口', 1, NOW(), 1, NOW(), 1, 'pc', 2, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 2 AND del_flag = 0
      AND (perms = 'plugin:hello:view' OR path = '/plugins/hello')
);

-- 只挂在自身有效菜单下；遇到同路径的客户资源时不覆盖、不向其追加接口。
INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms, feature_code,
    api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type, del_flag
)
SELECT 1, '读取示例插件信息', menu.id, 4, 1,
       0, '_self', 0, 1, 1, 'plugin:hello:info', NULL, 'GET', '/plugin/hello/info', 0, 0,
       '需要 plugin:hello:info 权限；不自动分配给现有角色',
       1, NOW(), 1, NOW(), 1, 'pc', 2, 0
FROM sys_resource menu
WHERE menu.tenant_id = 1 AND menu.resource_type = 2 AND menu.del_flag = 0
  AND menu.perms = 'plugin:hello:view' AND menu.path = '/plugins/hello'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND resource_type = 4 AND del_flag = 0
        AND (perms = 'plugin:hello:info' OR (api_method = 'GET' AND api_url = '/plugin/hello/info'))
  );
