-- 客户端管理菜单
INSERT INTO `sys_resource` (`tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `path`, `component`, `is_external`, `is_public`, `menu_status`, `visible`, `perms`, `icon`, `create_by`) 
VALUES 
(0, '客户端管理', 2, 1, 8, '/system/client', 'system/client', 0, 0, 1, 1, NULL, 'mdi:desktop-mac', 1);

-- 获取客户端管理菜单的ID
SET @client_menu_id = LAST_INSERT_ID();

-- 客户端管理权限按钮
INSERT INTO `sys_resource` (`tenant_id`, `resource_name`, `parent_id`, `resource_type`, `sort`, `perms`, `menu_status`, `visible`, `create_by`) 
VALUES 
(0, '客户端查询', @client_menu_id, 2, 1, 'system:client:list', 1, 1, 1),
(0, '客户端新增', @client_menu_id, 2, 2, 'system:client:create', 1, 1, 1),
(0, '客户端修改', @client_menu_id, 2, 3, 'system:client:update', 1, 1, 1),
(0, '客户端删除', @client_menu_id, 2, 4, 'system:client:delete', 1, 1, 1),
(0, '在线用户查询', @client_menu_id, 2, 5, 'system:client:online', 1, 1, 1),
(0, '踢出用户', @client_menu_id, 2, 6, 'system:client:kickout', 1, 1, 1),
(0, '刷新缓存', @client_menu_id, 2, 7, 'system:client:reload-cache', 1, 1, 1);