-- 测试数据初始化脚本
-- 用于测试认证授权功能
-- 密码使用BCrypt加密，明文密码为：123456
-- BCrypt加密后的123456: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi

-- 1. 插入测试租户
INSERT INTO sys_tenant (id, tenant_name, contact_person, contact_phone, user_limit, tenant_status, expire_time, tenant_desc, create_time, update_time) 
VALUES 
(1, '默认租户', '系统管理员', '13800138000', 0, 1, '2099-12-31 23:59:59', '系统默认租户', NOW(), NOW());

-- 2. 插入测试用户
INSERT INTO sys_user (id, tenant_id, username, real_name, user_type, email, phone, gender, password, user_status, login_count, create_time, update_time)
VALUES 
(1, 1, 'admin', '超级管理员', 0, 'admin@example.com', '13800138000', 1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 0, NOW(), NOW()),
(2, 1, 'test', '测试用户', 2, 'test@example.com', '13800138001', 1, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 0, NOW(), NOW());

-- 3. 插入测试组织
INSERT INTO sys_org (id, tenant_id, org_name, parent_id, ancestors, sort, org_type, org_status, leader_id, leader_name, create_time, update_time)
VALUES 
(1, 1, '默认集团', 0, '0', 1, 1, 1, 1, '超级管理员', NOW(), NOW()),
(2, 1, '研发部', 1, '0,1', 1, 2, 1, 2, '测试用户', NOW(), NOW()),
(3, 1, '测试组', 2, '0,1,2', 1, 3, 1, 2, '测试用户', NOW(), NOW());

-- 4. 插入测试岗位
INSERT INTO sys_post (id, tenant_id, post_code, org_id, post_name, post_status, post_type, sort, remark, create_time, update_time)
VALUES 
(1, 1, 'CEO', 1, '首席执行官', 1, 1, 1, '公司最高管理者', NOW(), NOW()),
(2, 1, 'CTO', 1, '技术总监', 1, 1, 2, '技术部门负责人', NOW(), NOW()),
(3, 1, 'DEV', 2, '开发工程师', 1, 2, 1, '软件开发工程师', NOW(), NOW()),
(4, 1, 'QA', 2, '测试工程师', 1, 2, 2, '软件测试工程师', NOW(), NOW());

-- 5. 插入测试角色
INSERT INTO sys_role (id, tenant_id, role_name, role_key, data_scope, sort, role_status, is_system, remark, create_time, update_time)
VALUES 
(1, 1, '超级管理员', 'admin', 1, 1, 1, 1, '超级管理员角色，拥有所有权限', NOW(), NOW()),
(2, 1, '普通用户', 'user', 5, 2, 1, 0, '普通用户角色，只有查询权限', NOW(), NOW()),
(3, 1, '部门管理员', 'dept_admin', 3, 3, 1, 0, '部门管理员，管理本部门数据', NOW(), NOW());

-- 6. 插入测试资源（菜单和权限）
INSERT INTO sys_resource (id, tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, perms, icon, api_method, api_url, keep_alive, always_show, remark, create_time, update_time)
VALUES 
-- 一级菜单：系统管理
(1, 1, '系统管理', 0, 1, 1, '/system', NULL, 0, 0, 1, 1, NULL, 'system', NULL, NULL, 0, 1, '系统管理目录', NOW(), NOW()),

-- 二级菜单：用户管理
(2, 1, '用户管理', 1, 2, 1, '/system/user', 'system/user/index', 0, 0, 1, 1, 'system:user:list', 'user', NULL, NULL, 1, 0, '用户管理菜单', NOW(), NOW()),
-- 用户管理按钮
(3, 1, '用户查询', 2, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:query', NULL, NULL, NULL, 0, 0, '用户查询按钮', NOW(), NOW()),
(4, 1, '用户新增', 2, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, NULL, NULL, 0, 0, '用户新增按钮', NOW(), NOW()),
(5, 1, '用户修改', 2, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:user:edit', NULL, NULL, NULL, 0, 0, '用户修改按钮', NOW(), NOW()),
(6, 1, '用户删除', 2, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:user:remove', NULL, NULL, NULL, 0, 0, '用户删除按钮', NOW(), NOW()),
-- 用户管理API
(7, 1, '用户分页查询API', 2, 4, 1, NULL, NULL, 0, 0, 1, 1, 'system:user:page', NULL, 'GET', '/system/user/page', 0, 0, '用户分页查询接口', NOW(), NOW()),
(8, 1, '用户新增API', 2, 4, 2, NULL, NULL, 0, 0, 1, 1, 'system:user:add', NULL, 'POST', '/system/user/add', 0, 0, '用户新增接口', NOW(), NOW()),

-- 二级菜单：角色管理
(9, 1, '角色管理', 1, 2, 2, '/system/role', 'system/role/index', 0, 0, 1, 1, 'system:role:list', 'role', NULL, NULL, 1, 0, '角色管理菜单', NOW(), NOW()),
-- 角色管理按钮
(10, 1, '角色查询', 9, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:role:query', NULL, NULL, NULL, 0, 0, '角色查询按钮', NOW(), NOW()),
(11, 1, '角色新增', 9, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:role:add', NULL, NULL, NULL, 0, 0, '角色新增按钮', NOW(), NOW()),
(12, 1, '角色修改', 9, 3, 3, NULL, NULL, 0, 0, 1, 1, 'system:role:edit', NULL, NULL, NULL, 0, 0, '角色修改按钮', NOW(), NOW()),
(13, 1, '角色删除', 9, 3, 4, NULL, NULL, 0, 0, 1, 1, 'system:role:remove', NULL, NULL, NULL, 0, 0, '角色删除按钮', NOW(), NOW()),

-- 二级菜单：组织管理
(14, 1, '组织管理', 1, 2, 3, '/system/org', 'system/org/index', 0, 0, 1, 1, 'system:org:list', 'org', NULL, NULL, 1, 0, '组织管理菜单', NOW(), NOW()),
(15, 1, '组织查询', 14, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:org:query', NULL, NULL, NULL, 0, 0, '组织查询按钮', NOW(), NOW()),
(16, 1, '组织新增', 14, 3, 2, NULL, NULL, 0, 0, 1, 1, 'system:org:add', NULL, NULL, NULL, 0, 0, '组织新增按钮', NOW(), NOW()),

-- 二级菜单：岗位管理
(17, 1, '岗位管理', 1, 2, 4, '/system/post', 'system/post/index', 0, 0, 1, 1, 'system:post:list', 'post', NULL, NULL, 1, 0, '岗位管理菜单', NOW(), NOW()),
(18, 1, '岗位查询', 17, 3, 1, NULL, NULL, 0, 0, 1, 1, 'system:post:query', NULL, NULL, NULL, 0, 0, '岗位查询按钮', NOW(), NOW());

-- 7. 绑定用户角色关系
INSERT INTO sys_user_role (tenant_id, user_id, role_id, create_time)
VALUES 
(1, 1, 1, NOW()),  -- admin用户绑定超级管理员角色
(1, 2, 2, NOW());  -- test用户绑定普通用户角色

-- 8. 绑定用户岗位关系
INSERT INTO sys_user_post (tenant_id, user_id, post_id, is_main, create_time)
VALUES 
(1, 1, 1, 1, NOW()),  -- admin用户任CEO（主岗）
(1, 2, 3, 1, NOW()),  -- test用户任开发工程师（主岗）
(1, 2, 4, 0, NOW());  -- test用户兼任测试工程师（兼岗）

-- 9. 绑定用户组织关系
INSERT INTO sys_user_org (tenant_id, user_id, org_id, is_main, create_time)
VALUES 
(1, 1, 1, 1, NOW()),  -- admin用户在默认集团（主组织）
(1, 2, 2, 1, NOW()),  -- test用户在研发部（主组织）
(1, 2, 3, 0, NOW());  -- test用户在测试组（兼职组织）

-- 10. 绑定角色资源关系
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
VALUES 
-- 超级管理员拥有所有权限
(1, 1, 1, NOW()), (1, 1, 2, NOW()), (1, 1, 3, NOW()), (1, 1, 4, NOW()), (1, 1, 5, NOW()),
(1, 1, 6, NOW()), (1, 1, 7, NOW()), (1, 1, 8, NOW()), (1, 1, 9, NOW()), (1, 1, 10, NOW()),
(1, 1, 11, NOW()), (1, 1, 12, NOW()), (1, 1, 13, NOW()), (1, 1, 14, NOW()), (1, 1, 15, NOW()),
(1, 1, 16, NOW()), (1, 1, 17, NOW()), (1, 1, 18, NOW()),

-- 普通用户只有查询权限
(1, 2, 1, NOW()),  -- 系统管理目录
(1, 2, 2, NOW()),  -- 用户管理菜单
(1, 2, 3, NOW()),  -- 用户查询按钮
(1, 2, 7, NOW()),  -- 用户查询API
(1, 2, 9, NOW()),  -- 角色管理菜单
(1, 2, 10, NOW()), -- 角色查询按钮
(1, 2, 14, NOW()), -- 组织管理菜单
(1, 2, 15, NOW()), -- 组织查询按钮
(1, 2, 17, NOW()), -- 岗位管理菜单
(1, 2, 18, NOW()), -- 岗位查询按钮

-- 部门管理员拥有部门管理权限
(1, 3, 1, NOW()),  -- 系统管理目录
(1, 3, 2, NOW()),  -- 用户管理菜单
(1, 3, 3, NOW()),  -- 用户查询
(1, 3, 4, NOW()),  -- 用户新增
(1, 3, 5, NOW()),  -- 用户修改
(1, 3, 14, NOW()), -- 组织管理菜单
(1, 3, 15, NOW()), -- 组织查询
(1, 3, 16, NOW()); -- 组织新增

-- 11. 插入系统配置
INSERT INTO sys_config (config_id, tenant_id, config_name, config_key, config_value, config_type, config_desc, sort, create_time, update_time)
VALUES 
(1, 1, '用户初始密码', 'sys.user.initPassword', '123456', 'Y', '用户注册时的默认密码', 1, NOW(), NOW()),
(2, 1, '账号自助注册', 'sys.account.registerUser', 'false', 'Y', '是否开启用户自助注册功能', 2, NOW(), NOW());

-- 12. 插入字典类型
INSERT INTO sys_dict_type (dict_id, tenant_id, dict_name, dict_type, dict_status, remark, create_time, update_time)
VALUES 
(1, 1, '用户性别', 'sys_user_sex', 1, '用户性别列表', NOW(), NOW()),
(2, 1, '用户状态', 'sys_user_status', 1, '用户状态列表', NOW(), NOW()),
(3, 1, '组织类型', 'sys_org_type', 1, '组织类型列表', NOW(), NOW());

-- 13. 插入字典数据
INSERT INTO sys_dict_data (dict_code, tenant_id, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, dict_status, remark, create_time, update_time)
VALUES 
-- 用户性别
(1, 1, 1, '未知', '0', 'sys_user_sex', '', '', 'Y', 1, '性别未知', NOW(), NOW()),
(2, 1, 2, '男', '1', 'sys_user_sex', '', '', 'N', 1, '性别男', NOW(), NOW()),
(3, 1, 3, '女', '2', 'sys_user_sex', '', '', 'N', 1, '性别女', NOW(), NOW()),
-- 用户状态
(4, 1, 1, '禁用', '0', 'sys_user_status', '', 'danger', 'N', 1, '用户禁用状态', NOW(), NOW()),
(5, 1, 2, '正常', '1', 'sys_user_status', '', 'success', 'Y', 1, '用户正常状态', NOW(), NOW()),
(6, 1, 3, '锁定', '2', 'sys_user_status', '', 'warning', 'N', 1, '用户锁定状态', NOW(), NOW()),
-- 组织类型
(7, 1, 1, '公司', '1', 'sys_org_type', '', 'primary', 'Y', 1, '公司组织', NOW(), NOW()),
(8, 1, 2, '部门', '2', 'sys_org_type', '', 'success', 'N', 1, '部门组织', NOW(), NOW()),
(9, 1, 3, '小组', '3', 'sys_org_type', '', 'info', 'N', 1, '小组组织', NOW(), NOW());

-- ==============================================
-- 测试账号说明：
-- ==============================================
-- 账号1: admin / 123456 
--   角色: 超级管理员
--   权限: 拥有所有权限 (*:*:*)
--   岗位: CEO
--   组织: 默认集团
--
-- 账号2: test / 123456 
--   角色: 普通用户
--   权限: 只有查询权限
--   岗位: 开发工程师(主)、测试工程师(兼)
--   组织: 研发部(主)、测试组(兼)
-- ==============================================
