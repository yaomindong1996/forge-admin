-- 在线用户管理菜单配置
-- 请根据实际的sys_menu表结构和parent_id调整SQL语句

-- 1. 插入在线用户管理菜单(假设系统管理的parent_id为100)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('在线用户', 100, 5, 'online', 'system/online/index', 1, 'C', '0', '0', 'system:online:list', 'user', 'admin', NOW(), '', NULL, '在线用户管理菜单');

-- 2. 获取刚插入的菜单ID(用于设置按钮权限的parent_id)
-- 注意:实际使用时需要查询获取真实的menu_id,这里假设为201

-- 3. 插入查询按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('在线用户查询', 201, 1, '#', '', 1, 'F', '0', '0', 'system:online:query', '#', 'admin', NOW(), '', NULL, '');

-- 4. 插入强制下线按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('强制下线', 201, 2, '#', '', 1, 'F', '0', '0', 'system:online:kickout', '#', 'admin', NOW(), '', NULL, '');

-- 5. 插入批量下线按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('批量下线', 201, 3, '#', '', 1, 'F', '0', '0', 'system:online:batchKickout', '#', 'admin', NOW(), '', NULL, '');

-- 6. 插入封禁用户按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('封禁用户', 201, 4, '#', '', 1, 'F', '0', '0', 'system:online:ban', '#', 'admin', NOW(), '', NULL, '');

-- 7. 插入解封用户按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('解封用户', 201, 5, '#', '', 1, 'F', '0', '0', 'system:online:unban', '#', 'admin', NOW(), '', NULL, '');
