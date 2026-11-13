-- 下线旧「三方登录配置」菜单
-- 三方登录能力已整合进「企业协同」（连接管理 + 应用管理 + LOGIN 能力绑定），
-- 旧菜单 /system/socialConfig 只剩只读兼容视图，经确认整体下线。
-- 处理方式：
-- 1. sys_resource 按逻辑删除规范置 del_flag = 1（0/1 语义，见 V1.0.8）；
-- 2. sys_role_resource 关系表按既有约定物理清理（资源删除入口同样先清关系，见 V1.0.8 注释）。
-- 幂等性：UPDATE / DELETE 天然可重复执行；匹配条件使用 path + resource_type，不依赖固定 id。

-- 1. 清理角色-资源授权关系
DELETE FROM sys_role_resource
WHERE resource_id IN (
  SELECT id FROM (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND resource_type = 2
      AND path = '/system/socialConfig' AND del_flag = 0
  ) tmp
);

-- 2. 逻辑删除菜单资源
UPDATE sys_resource
SET del_flag = 1, update_by = 1, update_time = NOW()
WHERE tenant_id = 1 AND resource_type = 2
  AND path = '/system/socialConfig' AND del_flag = 0;
