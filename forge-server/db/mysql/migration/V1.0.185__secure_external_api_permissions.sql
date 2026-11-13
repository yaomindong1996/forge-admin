-- 外部连接器管理/调用接口补齐显式权限资源。
-- 已有 permission_check_enabled=0 的接口不会被自动放开；调用守卫会拒绝，管理员需配置权限码后显式启用。
-- 回滚：移除本脚本新增的 external:* 权限资源及关联；列默认值可改回 0，但不应恢复调用侧 fail-open。

SET @external_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND path = '/external/system'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target,
                          is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show,
                          remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '查询外部系统', @external_menu_id, 4, 1, 0, '_self', 0, 1, 1,
       'external:system:query', 'GET', '/external/system/**', 0, 0, '外部连接器查询接口', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:system:query' AND api_method = 'GET' AND api_url = '/external/system/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '新增外部系统', @external_menu_id, 4, 2, 0, '_self', 0, 1, 1, 'external:system:add', 'POST', '/external/system', 0, 0, '外部连接器新增接口', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:system:add' AND api_method = 'POST' AND api_url = '/external/system' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '修改外部系统', @external_menu_id, 4, 3, 0, '_self', 0, 1, 1, 'external:system:edit', 'PUT', '/external/system', 0, 0, '外部连接器修改接口', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:system:edit' AND api_method = 'PUT' AND api_url = '/external/system' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '删除外部系统', @external_menu_id, 4, 4, 0, '_self', 0, 1, 1, 'external:system:remove', 'DELETE', '/external/system/**', 0, 0, '外部连接器删除接口', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:system:remove' AND api_method = 'DELETE' AND api_url = '/external/system/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '查询外部接口', @external_menu_id, 4, 5, 0, '_self', 0, 1, 1, 'external:api:query', 'GET', '/external/api/**', 0, 0, '外部接口配置查询', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:api:query' AND api_method = 'GET' AND api_url = '/external/api/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '新增外部接口', @external_menu_id, 4, 6, 0, '_self', 0, 1, 1, 'external:api:add', 'POST', '/external/api', 0, 0, '外部接口配置新增', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:api:add' AND api_method = 'POST' AND api_url = '/external/api' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '修改外部接口', @external_menu_id, 4, 7, 0, '_self', 0, 1, 1, 'external:api:edit', 'PUT', '/external/api', 0, 0, '外部接口配置修改', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:api:edit' AND api_method = 'PUT' AND api_url = '/external/api' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '删除外部接口', @external_menu_id, 4, 8, 0, '_self', 0, 1, 1, 'external:api:remove', 'DELETE', '/external/api/**', 0, 0, '外部接口配置删除', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:api:remove' AND api_method = 'DELETE' AND api_url = '/external/api/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '调用外部接口', @external_menu_id, 4, 9, 0, '_self', 0, 1, 1, 'external:proxy:invoke', NULL, '/external/proxy/*', 0, 0, '受控调用外部接口（GET/POST）', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource
      WHERE tenant_id = 1
        AND resource_type = 4
        AND perms = 'external:proxy:invoke'
        AND client_code = 'pc'
        AND del_flag = 0
  );

-- MySQL/Flyway 可能保留失败迁移在上一条 GET 插入后的半成品记录。
-- api_method 为空表示同一路径覆盖全部 HTTP 方法，与权限唯一键的一权限一资源约束保持一致。
UPDATE sys_resource
SET resource_name = '调用外部接口',
    sort = 9,
    api_method = NULL,
    api_url = '/external/proxy/*',
    remark = '受控调用外部接口（GET/POST）',
    min_user_type = 2,
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 4
  AND perms = 'external:proxy:invoke'
  AND client_code = 'pc'
  AND del_flag = 0;

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '调试外部接口', @external_menu_id, 4, 11, 0, '_self', 0, 1, 1, 'external:proxy:debug', 'POST', '/external/proxy/debug/**', 0, 0, '仅平台管理员显式授权', 1, NOW(), 1, NOW(), 1, 'pc', 1
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:proxy:debug' AND api_method = 'POST' AND api_url = '/external/proxy/debug/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '查询外部调用日志', @external_menu_id, 4, 12, 0, '_self', 0, 1, 1, 'external:log:query', 'GET', '/external/api/log/**', 0, 0, '外部调用日志查询', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:log:query' AND api_method = 'GET' AND api_url = '/external/api/log/**' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '删除外部调用日志', @external_menu_id, 4, 13, 0, '_self', 0, 1, 1, 'external:log:remove', 'DELETE', '/external/api/log/*', 0, 0, '删除单条外部调用日志', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:log:remove' AND api_method = 'DELETE' AND api_url = '/external/api/log/*' AND del_flag = 0);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target, is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show, remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, '清理外部调用日志', @external_menu_id, 4, 14, 0, '_self', 0, 1, 1, 'external:log:clear', 'DELETE', '/external/api/log/clear', 0, 0, '仅平台管理员显式授权', 1, NOW(), 1, NOW(), 1, 'pc', 1
WHERE @external_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND perms = 'external:log:clear' AND api_method = 'DELETE' AND api_url = '/external/api/log/clear' AND del_flag = 0);

-- 兼容已有“服务接口代理”菜单角色：授予常规管理和调用权限，不自动授予调试与日志清理高风险权限。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT menu_grant.tenant_id, menu_grant.role_id, permission_resource.id, NOW()
FROM sys_role_resource menu_grant
INNER JOIN sys_resource menu_resource
        ON menu_resource.id = menu_grant.resource_id
       AND menu_resource.tenant_id = menu_grant.tenant_id
       AND menu_resource.path = '/external/system'
       AND menu_resource.del_flag = 0
INNER JOIN sys_resource permission_resource
        ON permission_resource.tenant_id = menu_grant.tenant_id
       AND permission_resource.del_flag = 0
       AND permission_resource.perms IN (
           'external:system:query', 'external:system:add', 'external:system:edit', 'external:system:remove',
           'external:api:query', 'external:api:add', 'external:api:edit', 'external:api:remove',
           'external:proxy:invoke', 'external:log:query', 'external:log:remove'
       )
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = menu_grant.tenant_id
      AND existing.role_id = menu_grant.role_id
      AND existing.resource_id = permission_resource.id
);

-- 新建外部 API 默认必须开启调用权限校验；历史关闭项保持原值并由调用守卫失败关闭。
SET @external_permission_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_external_api'
      AND COLUMN_NAME = 'permission_check_enabled'
);
SET @external_permission_default = (
    SELECT COLUMN_DEFAULT
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_external_api'
      AND COLUMN_NAME = 'permission_check_enabled'
    LIMIT 1
);
SET @alter_external_permission_default_sql = IF(
    @external_permission_column_exists = 1 AND COALESCE(CAST(@external_permission_default AS CHAR), '') <> '1',
    'ALTER TABLE `sys_external_api` ALTER COLUMN `permission_check_enabled` SET DEFAULT 1',
    'SELECT 1'
);
PREPARE alter_external_permission_default_stmt FROM @alter_external_permission_default_sql;
EXECUTE alter_external_permission_default_stmt;
DEALLOCATE PREPARE alter_external_permission_default_stmt;
