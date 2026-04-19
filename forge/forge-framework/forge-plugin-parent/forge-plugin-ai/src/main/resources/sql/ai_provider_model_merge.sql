-- =============================================
-- 供应商+模型整合：合并为供应商管理页面
-- =============================================

-- 更新供应商管理菜单的 component 和名称
UPDATE sys_resource
SET resource_name = '供应商管理',
    component = '/ai/provider-model',
    icon = 'mdi:server'
WHERE tenant_id = 0
  AND component = '/ai/provider'
  AND resource_type = 2;

-- 隐藏模型管理菜单（改为不可见）
UPDATE sys_resource
SET visible = 0
WHERE tenant_id = 0
  AND component = '/ai/model'
  AND resource_type = 2;
