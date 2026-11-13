-- 企业协同同步批次删除能力资源
-- 同步批次列表（sys_social_sync_log 运行时表）支持删除已收敛批次，补按钮与API资源并授予默认租户超管。

-- 1. 按钮资源：挂在「同步批次」菜单下
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '删除批次', menu.id, 3, 2, 0, '_self', 0, 1, 1,
       'system:collaboration:sync:remove', 0, 0, '删除已收敛的同步批次',
       1, NOW(), 1, NOW(), 1, 'pc'
FROM sys_resource menu
WHERE menu.tenant_id = 1 AND menu.resource_type = 2
  AND menu.perms = 'system:collaboration:sync:view' AND menu.del_flag = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1 AND r.resource_type = 3
      AND r.perms = 'system:collaboration:sync:remove' AND r.del_flag = 0
  )
LIMIT 1;

-- 2. API 资源
INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible,
  perms, api_method, api_url, keep_alive, always_show, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, '同步批次删除接口', COALESCE(dir.id, 0), 4, 18, 0, '_self', 0, 1, 1,
       'system:collaboration:api:synclog:remove', 'DELETE', '/system/collaboration/sync-logs/*',
       0, 0, '删除已收敛的同步批次',
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (SELECT id FROM sys_resource WHERE tenant_id = 1 AND resource_type = 1
      AND path = '/system/collaboration' AND del_flag = 0 ORDER BY id LIMIT 1) dir
WHERE NOT EXISTS (
  SELECT 1 FROM sys_resource r
  WHERE r.tenant_id = 1 AND r.resource_type = 4
    AND r.perms = 'system:collaboration:api:synclog:remove' AND r.del_flag = 0
);

-- 3. 授予默认租户超级管理员
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT 1, admin_role.id, resource.id, NOW()
FROM (SELECT id FROM sys_role WHERE tenant_id = 1 AND role_key = 'admin' ORDER BY id LIMIT 1) admin_role
JOIN sys_resource resource ON resource.tenant_id = 1 AND resource.del_flag = 0
WHERE resource.perms IN ('system:collaboration:sync:remove', 'system:collaboration:api:synclog:remove')
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = admin_role.id
      AND existing.resource_id = resource.id
  );

-- 4. 收敛历史孤儿批次：租户回填前创建的批次收敛 CAS 因 tenant_id 为 NULL 匹配失败，
--    遗留的 RUNNING 批次统一判定失败（活动批次由连接级分布式锁保证，不会误伤）
UPDATE sys_social_sync_log
SET status = 'FAILED',
    error_code = 'ORPHAN_BACKFILL',
    error_summary = '历史孤儿批次，由迁移脚本统一收敛为失败',
    end_time = NOW()
WHERE status = 'RUNNING'
  AND start_time < DATE_SUB(NOW(), INTERVAL 1 HOUR);
