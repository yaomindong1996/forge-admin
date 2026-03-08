-- ========================================
-- 缓存管理菜单和权限配置
-- ========================================
-- 说明：
-- 1. 执行此SQL前，请先查询"系统管理"菜单的实际ID，并替换下方的parent_id值
-- 2. 查询SQL: SELECT id FROM sys_resource WHERE resource_name = '系统管理' AND resource_type = 1 LIMIT 1;
-- 3. tenant_id默认为000000，请根据实际情况修改
-- ========================================

-- 1. 插入缓存管理菜单（二级菜单）
-- 注意：parent_id需要替换为"系统管理"菜单的实际ID
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
    000000,                    -- 租户ID
    '缓存管理',                -- 菜单名称
    1,                         -- 父菜单ID（需要替换为"系统管理"的实际ID）
    2,                         -- 资源类型：2-菜单
    100,                       -- 排序（值越大越靠后）
    '/system/cache',           -- 菜单路径
    'system/cache',            -- 组件路径
    0,                         -- 是否外链：0-否
    0,                         -- 是否公开：0-否
    1,                         -- 菜单状态：1-显示
    1,                         -- 是否可见：1-是
    'system:cache:list',       -- 权限标识
    'i-mdi:database',          -- 菜单图标
    1,                         -- 是否缓存：1-是
    0,                         -- 是否总是显示：0-否
    '缓存管理菜单',            -- 备注
    1,                         -- 创建者
    NOW(),                     -- 创建时间
    1,                         -- 更新者
    NOW()                      -- 更新时间
);

-- 获取刚插入的菜单ID
SET @cache_menu_id = LAST_INSERT_ID();

-- 2. 插入缓存管理相关的按钮权限

-- 查询缓存按钮
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存查询',
    @cache_menu_id,
    3,                         -- 资源类型：3-按钮
    1,
    1,
    'system:cache:query',
    '查询缓存按钮',
    1,
    NOW(),
    1,
    NOW()
);

-- 删除缓存按钮
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存删除',
    @cache_menu_id,
    3,
    2,
    1,
    'system:cache:remove',
    '删除缓存按钮',
    1,
    NOW(),
    1,
    NOW()
);

-- 清空缓存按钮
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '清空缓存',
    @cache_menu_id,
    3,
    3,
    1,
    'system:cache:clear',
    '清空缓存按钮',
    1,
    NOW(),
    1,
    NOW()
);

-- 3. 插入缓存管理相关的API权限

-- 分页查询API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `api_method`,
    `api_url`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存-分页查询',
    @cache_menu_id,
    4,                         -- 资源类型：4-API接口
    1,
    1,
    'system:cache:page',
    'GET',
    '/system/cache/page',
    '分页查询缓存列表',
    1,
    NOW(),
    1,
    NOW()
);

-- 获取缓存详情API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `api_method`,
    `api_url`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存-详情查询',
    @cache_menu_id,
    4,
    2,
    1,
    'system:cache:getInfo',
    'POST',
    '/system/cache/getInfo',
    '查询缓存详细信息',
    1,
    NOW(),
    1,
    NOW()
);

-- 删除缓存API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `api_method`,
    `api_url`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存-删除',
    @cache_menu_id,
    4,
    3,
    1,
    'system:cache:remove',
    'POST',
    '/system/cache/remove',
    '删除单个缓存',
    1,
    NOW(),
    1,
    NOW()
);

-- 批量删除缓存API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `api_method`,
    `api_url`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存-批量删除',
    @cache_menu_id,
    4,
    4,
    1,
    'system:cache:removeBatch',
    'POST',
    '/system/cache/removeBatch',
    '批量删除缓存',
    1,
    NOW(),
    1,
    NOW()
);

-- 清空所有缓存API
INSERT INTO `sys_resource` (
    `tenant_id`,
    `resource_name`,
    `parent_id`,
    `resource_type`,
    `sort`,
    `visible`,
    `perms`,
    `api_method`,
    `api_url`,
    `remark`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`
) VALUES (
    000000,
    '缓存-清空',
    @cache_menu_id,
    4,
    5,
    1,
    'system:cache:clear',
    'POST',
    '/system/cache/clear',
    '清空所有缓存',
    1,
    NOW(),
    1,
    NOW()
);

-- ========================================
-- 使用说明：
-- ========================================
-- 1. 执行SQL前的准备工作：
--    a) 查询"系统管理"菜单的ID：
--       SELECT id FROM sys_resource WHERE resource_name = '系统管理' AND resource_type = 1 LIMIT 1;
--    b) 将查询结果替换第25行的parent_id值（当前为1）
--    c) 根据实际情况修改tenant_id（当前为000000）
--
-- 2. 执行SQL：
--    直接执行本SQL文件即可
--
-- 3. 分配权限：
--    在系统管理-角色管理中，为超级管理员角色分配"缓存管理"菜单及其所有子权限
--    执行以下SQL为超级管理员（role_id=1）分配权限：
--
--    INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
--    SELECT 000000, 1, id, NOW() 
--    FROM sys_resource 
--    WHERE resource_name LIKE '缓存%' OR parent_id = @cache_menu_id;
--
-- 4. 刷新页面：
--    执行完成后，退出重新登录或刷新页面即可看到"缓存管理"菜单
-- ========================================
