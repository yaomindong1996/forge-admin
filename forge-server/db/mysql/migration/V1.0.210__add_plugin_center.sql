-- 当前后端实例的只读插件中心，仅平台超级管理员；不授权普通角色、不调整已有菜单。
-- 回滚：隐藏 perms='system:plugin:view' 的菜单及其子资源，保留资源/字典历史。
INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component,
    is_external, open_target, is_public, menu_status, visible, perms, icon,
    keep_alive, always_show, remark, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, '插件中心', platform.id, 2, 90, '/system/plugin', 'system/plugin',
       0, '_self', 0, 1, 1, 'system:plugin:view', 'ionicons5:ExtensionPuzzleOutline',
       0, 0, '当前服务加载的内置及外部插件，源码安装需独立构建部署', 1, NOW(), 1, NOW(), 1, 'pc', 0, 0
FROM sys_resource platform
WHERE platform.tenant_id = 1 AND platform.resource_type = 1 AND platform.parent_id = 0
  AND platform.resource_name = '平台管理' AND platform.client_code = 'pc' AND platform.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND resource_type = 2 AND del_flag = 0
        AND (perms = 'system:plugin:view' OR path = '/system/plugin')
  )
ORDER BY platform.id LIMIT 1;

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, is_public, menu_status, visible,
    perms, api_method, api_url, create_by, create_time, update_by, update_time,
    create_dept, client_code, min_user_type, del_flag
)
SELECT 1, entry.name, menu.id, entry.type, entry.sort, 0, 1, 1, entry.perms,
       entry.method, entry.url, 1, NOW(), 1, NOW(), 1, 'pc', 0, 0
FROM sys_resource menu
CROSS JOIN (
    SELECT '查询插件清单' AS name, 4 AS type, 1 AS sort, 'system:plugin:list' AS perms,
           'GET' AS method, '/system/plugin/page' AS url
    UNION ALL SELECT '查询插件详情', 4, 2, 'system:plugin:detail', 'GET', '/system/plugin/*'
    UNION ALL SELECT '查看插件', 3, 3, 'system:plugin:detail', NULL, NULL
) entry
WHERE menu.tenant_id = 1 AND menu.resource_type = 2 AND menu.del_flag = 0
  AND menu.perms = 'system:plugin:view' AND menu.path = '/system/plugin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing
      WHERE existing.tenant_id = 1 AND existing.resource_type = entry.type AND existing.del_flag = 0
        AND (existing.perms = entry.perms
             OR (entry.url IS NOT NULL AND existing.api_method = entry.method AND existing.api_url = entry.url))
  );

INSERT INTO sys_dict_type (
    tenant_id, dict_name, dict_type, dict_status, create_by, create_time,
    update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.name, entry.code, 1, 1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT '插件来源' AS name, 'sys_plugin_origin' AS code
    UNION ALL SELECT '插件发行版', 'sys_plugin_edition'
    UNION ALL SELECT '插件加载状态', 'sys_plugin_load_state'
    UNION ALL SELECT '插件功能使用权', 'sys_plugin_feature_state'
) entry
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type existing
    WHERE existing.tenant_id = 1 AND existing.dict_type = entry.code AND existing.del_flag = 0
);

INSERT INTO sys_dict_data (
    tenant_id, dict_sort, dict_label, dict_value, dict_type, list_class, is_default, dict_status,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1, entry.sort, entry.label, entry.value, entry.type, entry.style, 'N', 1,
       1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 1 AS sort, '内置模块' AS label, 'builtin' AS value, 'sys_plugin_origin' AS type, 'default' AS style
    UNION ALL SELECT 2, '外部插件', 'external', 'sys_plugin_origin', 'info'
    UNION ALL SELECT 1, '社区版', 'community', 'sys_plugin_edition', 'default'
    UNION ALL SELECT 2, '商业版', 'ee', 'sys_plugin_edition', 'info'
    UNION ALL SELECT 1, '后端已加载', 'backend_loaded', 'sys_plugin_load_state', 'success'
    UNION ALL SELECT 2, '仅元数据', 'metadata_only', 'sys_plugin_load_state', 'warning'
    UNION ALL SELECT 1, '已开通', 'true', 'sys_plugin_feature_state', 'success'
    UNION ALL SELECT 2, '未开通', 'false', 'sys_plugin_feature_state', 'warning'
) entry
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data existing
    WHERE existing.tenant_id = 1 AND existing.dict_type = entry.type
      AND existing.dict_value = entry.value AND existing.del_flag = 0
);
