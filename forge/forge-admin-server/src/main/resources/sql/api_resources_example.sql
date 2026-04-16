-- ========================================
-- API资源权限配置示例
-- ========================================
-- 说明：
-- 1. resourceType=4 表示API接口类型
-- 2. apiUrl字段配置接口路径，支持通配符：
--    - /system/** 匹配系统管理下所有接口
--    - /system/user/* 匹配用户管理的单层接口
--    - /system/user/add 精确匹配单个接口
-- 3. visible=1 表示资源可用
-- ========================================

-- 系统管理模块API资源
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, visible, api_url, remark, create_time, update_time)
VALUES
-- 通配符示例：系统管理所有接口
(1, '系统管理-所有接口', 0, 4, 1, 1, '/system/**', '系统管理模块所有接口', NOW(), NOW()),

-- 用户管理API
(1, '用户列表查询', 0, 4, 2, 1, '/system/user/page', '分页查询用户列表', NOW(), NOW()),
(1, '用户详情查询', 0, 4, 3, 1, '/system/user/getById', '根据ID查询用户详情', NOW(), NOW()),
(1, '用户新增', 0, 4, 4, 1, '/system/user/add', '新增用户', NOW(), NOW()),
(1, '用户编辑', 0, 4, 5, 1, '/system/user/edit', '编辑用户信息', NOW(), NOW()),
(1, '用户删除', 0, 4, 6, 1, '/system/user/remove', '删除用户', NOW(), NOW()),

-- 角色管理API
(1, '角色列表查询', 0, 4, 7, 1, '/system/role/page', '分页查询角色列表', NOW(), NOW()),
(1, '角色新增', 0, 4, 8, 1, '/system/role/add', '新增角色', NOW(), NOW()),
(1, '角色编辑', 0, 4, 9, 1, '/system/role/edit', '编辑角色信息', NOW(), NOW()),
(1, '角色删除', 0, 4, 10, 1, '/system/role/remove', '删除角色', NOW(), NOW()),

-- 资源管理API
(1, '资源列表查询', 0, 4, 11, 1, '/system/resource/page', '分页查询资源列表', NOW(), NOW()),
(1, '资源树查询', 0, 4, 12, 1, '/system/resource/tree', '查询资源树', NOW(), NOW()),
(1, '当前用户资源树', 0, 4, 13, 1, '/system/resource/current/tree', '查询当前用户资源树', NOW(), NOW()),
(1, '当前用户菜单树', 0, 4, 14, 1, '/system/resource/current/menu', '查询当前用户菜单树', NOW(), NOW()),
(1, '当前用户权限列表', 0, 4, 15, 1, '/system/resource/current/permissions', '查询当前用户权限列表', NOW(), NOW()),

-- 组织管理API（通配符示例）
(1, '组织管理-所有接口', 0, 4, 16, 1, '/system/org/*', '组织管理模块所有单层接口', NOW(), NOW()),

-- 租户管理API
(1, '租户管理-所有接口', 0, 4, 17, 1, '/system/tenant/**', '租户管理模块所有接口', NOW(), NOW());

-- ========================================
-- 角色资源关联示例
-- ========================================
-- 假设roleId=1是超级管理员角色，roleId=2是普通管理员角色

-- 普通管理员角色（roleId=2）的API权限
-- 注意：需要根据实际的资源ID进行关联
-- 示例：授予用户管理查询权限
-- INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
-- SELECT 1, 2, id, NOW() FROM sys_resource WHERE resource_name IN ('用户列表查询', '用户详情查询');

-- ========================================
-- 使用说明
-- ========================================
-- 1. 在sys_resource表中配置API资源（resourceType=4）
-- 2. 在apiUrl字段填写接口路径，支持通配符
-- 3. 将API资源分配给角色（sys_role_resource表）
-- 4. 系统会自动校验用户访问接口时是否有权限
-- 5. 超级管理员（userType=0）自动拥有所有接口权限
-- ========================================
