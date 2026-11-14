-- 插件中心提升为可直接进入的一级菜单；沿用原 ID、页面路由、排序及全部权限关联。
-- 仅移动默认租户 PC 的内置菜单，不修改资源类型、可见状态和平台管理员访问边界。
-- 上线前记录该菜单原 parent_id/icon；回滚恢复这两个字段即可，不删除资源或角色授权。
UPDATE sys_resource
SET parent_id = 0,
    icon = 'i-streamline-plump-color:module',
    update_by = 1,
    update_time = NOW()
WHERE tenant_id = 1
  AND client_code = 'pc'
  AND resource_type = 2
  AND min_user_type = 0
  AND del_flag = 0
  AND perms = 'system:plugin:view'
  AND path = '/system/plugin'
  AND component = 'system/plugin'
  AND (parent_id IS NULL OR parent_id <> 0
       OR icon IS NULL OR icon <> 'i-streamline-plump-color:module');
