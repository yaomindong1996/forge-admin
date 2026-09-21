-- 打印预览/准备接口需要 print:execute；V1.0.172 只登记资源、未赋权，导致能设计模板却打不开预览。
-- 将使用打印权限授予：已有打印模板查看/管理权限，或已有应用运行入口权限的角色。
-- 回滚：删除本脚本写入的 sys_role_resource 行（按 resource perms=print:execute 与来源角色条件核对后删除）。

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, print_resource.id, NOW()
FROM (
    SELECT rr.tenant_id, rr.role_id
    FROM sys_role_resource rr
    INNER JOIN sys_resource r
      ON r.id = rr.resource_id
     AND r.tenant_id = rr.tenant_id
     AND r.del_flag = 0
     AND (
       r.perms IN ('print:template:view', 'print:template:manage', 'print:template:publish', 'ai:businessApplication:runtime')
       OR r.path = '/app-center'
     )
) seed
INNER JOIN sys_resource print_resource
  ON print_resource.tenant_id = seed.tenant_id
 AND print_resource.del_flag = 0
 AND print_resource.perms = 'print:execute'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.role_id = seed.role_id
      AND existing.resource_id = print_resource.id
);

-- 同步把隐藏打印页面挂到同一批角色，避免菜单/路由侧缺入口。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, print_page.id, NOW()
FROM (
    SELECT rr.tenant_id, rr.role_id
    FROM sys_role_resource rr
    INNER JOIN sys_resource r
      ON r.id = rr.resource_id
     AND r.tenant_id = rr.tenant_id
     AND r.del_flag = 0
     AND (
       r.perms IN ('print:template:view', 'print:template:manage', 'print:execute', 'ai:businessApplication:runtime')
       OR r.path = '/app-center'
     )
) seed
INNER JOIN sys_resource print_page
  ON print_page.tenant_id = seed.tenant_id
 AND print_page.del_flag = 0
 AND print_page.path IN ('/print', '/print/designer', '/print/preview')
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.role_id = seed.role_id
      AND existing.resource_id = print_page.id
);
