-- ----------------------------
-- API配置管理菜单
-- ----------------------------
-- 菜单ID从1000开始，避免与其他模块冲突

-- 一级菜单：系统管理（如果已存在则跳过）
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1000, '系统管理', 0, 1, 'system', NULL, 1, 'M', '0', 1, '', 'setting', 'admin', NOW(), NULL, NULL, '系统管理目录');

-- 二级菜单：API配置管理
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1100, 'API配置管理', 1000, 5, 'apiConfig', 'system/apiConfig/index', 1, 'C', '0', 1, 'system:apiConfig:list', 'api', 'admin', NOW(), NULL, NULL, 'API配置管理菜单');

-- 三级菜单：API配置查询
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1101, 'API配置查询', 1100, 1, '', '', 1, 'F', '0', 1, 'system:apiConfig:query', '#', 'admin', NOW(), NULL, NULL, '');

-- 三级菜单：API配置新增
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1102, 'API配置新增', 1100, 2, '', '', 1, 'F', '0', 1, 'system:apiConfig:add', '#', 'admin', NOW(), NULL, NULL, '');

-- 三级菜单：API配置修改
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1103, 'API配置修改', 1100, 3, '', '', 1, 'F', '0', 1, 'system:apiConfig:edit', '#', 'admin', NOW(), NULL, NULL, '');

-- 三级菜单：API配置删除
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1104, 'API配置删除', 1100, 4, '', '', 1, 'F', '0', 1, 'system:apiConfig:remove', '#', 'admin', NOW(), NULL, NULL, '');

-- 三级菜单：API配置导出
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1105, 'API配置导出', 1100, 5, '', '', 1, 'F', '0', 1, 'system:apiConfig:export', '#', 'admin', NOW(), NULL, NULL, '');

-- 三级菜单：缓存管理
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (1106, '缓存管理', 1100, 6, '', '', 1, 'F', '0', 1, 'system:apiConfig:cache', '#', 'admin', NOW(), NULL, NULL, '');

-- 给超级管理员分配菜单权限（假设超级管理员角色ID为1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` >= 1100 AND `menu_id` < 1200;
