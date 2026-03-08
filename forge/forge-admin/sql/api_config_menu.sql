-- API配置管理菜单SQL

-- 1. 添加"API配置管理"菜单（父菜单，假设在"系统管理"下）
-- 注意：请根据实际情况调整parent_id，这里假设系统管理的ID为某个值
-- 可以先查询系统管理的ID：SELECT id FROM sys_resource WHERE resource_name = '系统管理' LIMIT 1;

-- 插入API配置管理菜单
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
    'API配置管理',
    0,  -- 请根据实际情况修改为"系统管理"的ID
    2,  -- 2表示菜单
    6,
    '/system/apiConfig',
    '/views/system/apiConfig',
    0,
    0,
    1,
    1,
    'system:apiConfig:list',
    'api',
    0,
    0,
    'API接口配置管理',
    1,
    NOW(),
    1,
    NOW()
);

-- 2. 添加API配置管理相关的API权限

-- 获取刚插入的菜单ID（用于作为父资源）
SET @menu_id = LAST_INSERT_ID();

-- 分页查询API
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
    'API配置管理-分页查询',
    @menu_id,
    4,  -- 4表示API接口
    1,
    'system:apiConfig:page',
    'GET',
    '/system/apiConfig/page',
    1,
    '分页查询API配置列表',
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
    'API配置管理-列表查询',
    @menu_id,
    4,
    2,
    'system:apiConfig:list',
    'GET',
    '/system/apiConfig/list',
    1,
    '查询API配置列表',
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
    'API配置管理-查询详情',
    @menu_id,
    4,
    3,
    'system:apiConfig:query',
    'GET',
    '/system/apiConfig/getById',
    1,
    '根据ID查询API配置详情',
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
    'API配置管理-新增',
    @menu_id,
    4,
    4,
    'system:apiConfig:add',
    'POST',
    '/system/apiConfig/add',
    1,
    '新增API配置',
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
    'API配置管理-修改',
    @menu_id,
    4,
    5,
    'system:apiConfig:edit',
    'POST',
    '/system/apiConfig/edit',
    1,
    '修改API配置',
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
    'API配置管理-删除',
    @menu_id,
    4,
    6,
    'system:apiConfig:remove',
    'POST',
    '/system/apiConfig/remove',
    1,
    '删除API配置',
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
    'API配置管理-批量删除',
    @menu_id,
    4,
    7,
    'system:apiConfig:remove',
    'POST',
    '/system/apiConfig/removeBatch',
    1,
    '批量删除API配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 刷新缓存API
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
    'API配置管理-刷新缓存',
    @menu_id,
    4,
    8,
    'system:apiConfig:refresh',
    'POST',
    '/apiConfig/refresh',
    1,
    '刷新API配置缓存',
    1,
    NOW(),
    1,
    NOW()
);

-- 自动注册API配置
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
    'API配置管理-自动注册',
    @menu_id,
    4,
    9,
    'system:apiConfig:register',
    'POST',
    '/apiConfig/registerApiConfigs',
    1,
    '自动注册API配置',
    1,
    NOW(),
    1,
    NOW()
);

-- 使用说明：
-- 1. 执行此SQL前，请先修改第一段INSERT语句中的parent_id为"系统管理"菜单的实际ID
-- 2. 可以通过以下SQL查询系统管理菜单的ID：
--    SELECT id FROM sys_resource WHERE resource_name = '系统管理' LIMIT 1;
-- 3. 如果需要在角色中分配这些权限，需要在系统管理-角色管理中，为相应的角色分配"API配置管理"菜单及其子权限
-- 4. 执行完成后，刷新页面即可看到"API配置管理"菜单
