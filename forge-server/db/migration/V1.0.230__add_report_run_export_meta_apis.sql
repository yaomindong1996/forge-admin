-- 报表运行元数据与 Excel 导出：权限已由 V1.0.229 的通配资源覆盖
-- （report:run → /report/run/**，report:definition:manage → /report/**）。
-- 本脚本仅补齐角色授权，避免同一 perms 再次插入 type=4 触发 uk_tenant_resource_active。

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, NOW()
FROM (
    SELECT rr.tenant_id, rr.role_id
    FROM sys_role_resource rr
    INNER JOIN sys_resource r
      ON r.id = rr.resource_id
     AND r.tenant_id = rr.tenant_id
     AND r.del_flag = 0
     AND r.perms IN (
       'report:run', 'report:definition:view', 'report:definition:manage',
       'data:dataset:view', 'data:dataset:query'
     )
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND target.client_code = 'pc'
 AND target.resource_type = 4
 AND target.perms IN (
   'report:run', 'report:definition:view', 'report:definition:manage', 'report:definition:publish'
 )
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.role_id = seed.role_id
      AND existing.resource_id = target.id
);
