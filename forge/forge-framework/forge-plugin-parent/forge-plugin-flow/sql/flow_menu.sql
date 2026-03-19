-- =============================================
-- Forge Flow 流程管理模块菜单数据
-- 根据 sys_resource 表结构生成
-- =============================================

-- 流程管理一级目录
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '流程管理', 0, 1, 50, '/flow', NULL, 0, 0, 1, 1, 'mdi:flow', 0, 0, '流程管理目录', NOW(), NOW());

-- 获取刚插入的目录ID（假设为 LAST_INSERT_ID()）
SET @flow_root_id = LAST_INSERT_ID();

-- 我的待办菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '我的待办', @flow_root_id, 2, 1, '/flow/todo', '/flow/todo', 0, 0, 1, 1, 'mdi:clipboard-list', 0, 0, '我的待办任务', NOW(), NOW());

-- 我的已办菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '我的已办', @flow_root_id, 2, 2, '/flow/done', '/flow/done', 0, 0, 1, 1, 'mdi:clipboard-check', 0, 0, '我的已办任务', NOW(), NOW());

-- 我发起的菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '我发起的', @flow_root_id, 2, 3, '/flow/started', '/flow/started', 0, 0, 1, 1, 'mdi:send', 0, 0, '我发起的流程', NOW(), NOW());

-- 我的抄送菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '我的抄送', @flow_root_id, 2, 4, '/flow/cc', '/flow/cc', 0, 0, 1, 1, 'mdi:content-copy', 0, 0, '我的抄送', NOW(), NOW());

-- =============================================
-- 流程管理后台菜单（管理员使用）
-- =============================================

-- 流程管理二级目录
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '流程配置', @flow_root_id, 1, 10, '/flow/admin', NULL, 0, 0, 1, 1, 'mdi:cog', 0, 0, '流程管理配置', NOW(), NOW());

SET @flow_admin_id = LAST_INSERT_ID();

-- 流程模型管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '流程模型', @flow_admin_id, 2, 1, '/flow/admin/model', '/flow/model', 0, 0, 1, 1, 'mdi:file-document', 0, 0, '流程模型管理', NOW(), NOW());

SET @flow_model_id = LAST_INSERT_ID();

-- 流程分类管理菜单
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '流程分类', @flow_admin_id, 2, 2, '/flow/admin/category', '/flow/category', 0, 0, 1, 1, 'mdi:folder', 0, 0, '流程分类管理', NOW(), NOW());

SET @flow_category_id = LAST_INSERT_ID();

-- 流程设计页面（隐藏菜单，通过流程模型页面进入）
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external, is_public, menu_status, visible, icon, keep_alive, always_show, remark, create_time, update_time)
VALUES (0, '流程设计', @flow_admin_id, 2, 3, '/flow/design', '/flow/design', 0, 0, 1, 0, 'mdi:pencil-ruler', 0, 0, '流程设计器', NOW(), NOW());

-- =============================================
-- 按钮权限
-- =============================================

-- 流程模型按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES 
(0, '创建模型', @flow_model_id, 3, 1, 'flow:model:create', 0, 0, 1, 1, '创建流程模型', NOW(), NOW()),
(0, '编辑模型', @flow_model_id, 3, 2, 'flow:model:update', 0, 0, 1, 1, '编辑流程模型', NOW(), NOW()),
(0, '删除模型', @flow_model_id, 3, 3, 'flow:model:delete', 0, 0, 1, 1, '删除流程模型', NOW(), NOW()),
(0, '部署模型', @flow_model_id, 3, 4, 'flow:model:deploy', 0, 0, 1, 1, '部署流程模型', NOW(), NOW());

-- 流程分类按钮权限
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES 
(0, '创建分类', @flow_category_id, 3, 1, 'flow:category:create', 0, 0, 1, 1, '创建流程分类', NOW(), NOW()),
(0, '编辑分类', @flow_category_id, 3, 2, 'flow:category:update', 0, 0, 1, 1, '编辑流程分类', NOW(), NOW()),
(0, '删除分类', @flow_category_id, 3, 3, 'flow:category:delete', 0, 0, 1, 1, '删除流程分类', NOW(), NOW());

-- =============================================
-- API接口权限
-- =============================================

-- 流程任务API
INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, perms, api_method, api_url, is_external, is_public, menu_status, visible, remark, create_time, update_time)
VALUES 
(0, '待办任务查询', @flow_model_id, 4, 10, 'flow:task:todo', 'GET', '/api/flow/task/todo', 0, 0, 1, 1, '查询待办任务', NOW(), NOW()),
(0, '已办任务查询', @flow_model_id, 4, 11, 'flow:task:done', 'GET', '/api/flow/task/done', 0, 0, 1, 1, '查询已办任务', NOW(), NOW()),
(0, '发起的流程查询', @flow_model_id, 4, 12, 'flow:task:started', 'GET', '/api/flow/task/started', 0, 0, 1, 1, '查询发起的流程', NOW(), NOW()),
(0, '任务签收', @flow_model_id, 4, 13, 'flow:task:claim', 'POST', '/api/flow/task/claim', 0, 0, 1, 1, '签收任务', NOW(), NOW()),
(0, '任务审批', @flow_model_id, 4, 14, 'flow:task:approve', 'POST', '/api/flow/task/approve', 0, 0, 1, 1, '审批任务', NOW(), NOW()),
(0, '任务驳回', @flow_model_id, 4, 15, 'flow:task:reject', 'POST', '/api/flow/task/reject', 0, 0, 1, 1, '驳回任务', NOW(), NOW()),
(0, '任务催办', @flow_model_id, 4, 16, 'flow:task:remind', 'POST', '/api/flow/task/remind', 0, 0, 1, 1, '催办任务', NOW(), NOW());

-- =============================================
-- 给超级管理员角色分配流程管理菜单权限
-- 假设超级管理员角色ID为 1
-- =============================================

INSERT INTO sys_role_resource (role_id, resource_id)
SELECT 1, id FROM sys_resource WHERE tenant_id = 0 AND id >= @flow_root_id;