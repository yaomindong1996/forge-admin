-- 数据权限配置菜单SQL

-- 1. 添加"数据权限配置"菜单（父菜单，假设在"系统管理"下）
-- 注意：请根据实际情况调整parent_id，这里假设系统管理的ID为某个值
-- 可以先查询系统管理的ID：SELECT id FROM sys_resource WHERE resource_name = '系统管理' LIMIT 1;

-- 插入数据权限配置菜单
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `path`,
    `component`,
    `is_external`,
    `is_public`,
    `menu_status`,
    `visible`,
    `perms`,
    `icon`,
    `keep_alive`,
    `always_show`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置',
    0,  -- 请根据实际情况修改为"系统管理"的ID
    2,  -- 2表示菜单
    5,
    '/system/dataScopeConfig',
    '/views/system/dataScopeConfig',
    0,
    0,
    1,
    1,
    'system:dataScope:list',
    'lock',
    0,
    0,
    '数据权限配置管理',
    1,
    NOW(),
    1,
    NOW()
);

-- 2. 添加数据权限配置相关的API权限

-- 获取刚插入的菜单ID（用于作为父资源）
SET @menu_id = LAST_INSERT_ID();

-- 查询分页API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-分页查询',
    @menu_id,
    4,  -- 4表示API接口
    1,
    'system:dataScopeConfig:page',
    'GET',
    '/system/dataScopeConfig/page',
    1,
    '分页查询数据权限配置列表',
    1,
    NOW(),
    1,
    NOW()
);

-- 查询列表API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-列表查询',
    @menu_id,
    4,
    2,
    'system:dataScopeConfig:list',
    'GET',
    '/system/dataScopeConfig/list',
    1,
    '查询数据权限配置列表',
    1,
    NOW(),
    1,
    NOW()
);

-- 查询详情API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-查询详情',
    @menu_id,
    4,
    3,
    'system:dataScopeConfig:query',
    'POST',
    '/system/dataScopeConfig/getById',
    1,
    '根据ID查询数据权限配置详情',
    1,
    NOW(),
    1,
    NOW()
);

-- 新增API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-新增',
    @menu_id,
    4,
    4,
    'system:dataScopeConfig:add',
    'POST',
    '/system/dataScopeConfig/add',
    1,
    '新增数据权限配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 修改API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-修改',
    @menu_id,
    4,
    5,
    'system:dataScopeConfig:edit',
    'POST',
    '/system/dataScopeConfig/edit',
    1,
    '修改数据权限配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 删除API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-删除',
    @menu_id,
    4,
    6,
    'system:dataScopeConfig:remove',
    'POST',
    '/system/dataScopeConfig/remove',
    1,
    '删除数据权限配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 批量删除API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `perms`,
    `api_method`,
    `api_url`,
    `visible`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '数据权限配置-批量删除',
    @menu_id,
    4,
    7,
    'system:dataScopeConfig:remove',
    'POST',
    '/system/dataScopeConfig/removeBatch',
    1,
    '批量删除数据权限配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 使用说明：
-- 1. 执行此SQL前，请先修改第一段INSERT语句中的parent_id为"系统管理"菜单的实际ID
-- 2. 可以通过以下SQL查询系统管理菜单的ID：
--    SELECT id FROM sys_resource WHERE resource_name = '系统管理' LIMIT 1;
-- 3. 如果需要在角色中分配这些权限，需要在系统管理-角色管理中，为相应的角色分配"数据权限配置"菜单及其子权限
-- 4. 执行完成后，刷新页面即可看到"数据权限配置"菜单
