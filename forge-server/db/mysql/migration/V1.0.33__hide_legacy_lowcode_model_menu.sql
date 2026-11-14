-- 简易数据表导入已统一收进应用/业务对象创建流程。
-- 旧模型资产路由继续保留在授权树中，避免存量收藏和旧应用失效，但不再显示为主导航入口。

UPDATE sys_resource
SET visible = 0,
    remark = '高级模型资产兼容入口；简单表导入请从应用中心的新建应用或数据对象进入',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND resource_type = 2
  AND path = '/ai/lowcode-models'
  AND del_flag = 0;
