-- Gitee 社区体验租户：与默认演示租户数据隔离，仅承接 Star 后自动建号的用户。
-- 不写入 OAuth 密钥。Gitee 应用凭据仍在现有连接上配置。

INSERT INTO sys_tenant (
  id, tenant_name, tenant_status, user_limit, system_name, browser_title, tenant_desc,
  del_flag, create_by, create_time, update_by, update_time
)
SELECT 9001, 'Gitee社区体验', 1, 0, 'Forge 社区体验', 'Forge 社区体验',
       'Gitee Star 用户自动进入的隔离体验租户，与默认演示租户数据隔离',
       0, 1, NOW(), 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM sys_tenant WHERE id = 9001 AND del_flag = 0
)
AND NOT EXISTS (
  SELECT 1 FROM sys_tenant WHERE tenant_name = 'Gitee社区体验' AND del_flag = 0
);

SET @community_tenant_id := (
  SELECT id FROM sys_tenant
  WHERE del_flag = 0
    AND (id = 9001 OR tenant_name = 'Gitee社区体验')
  ORDER BY CASE WHEN id = 9001 THEN 0 ELSE 1 END
  LIMIT 1
);

INSERT INTO sys_org (
  tenant_id, org_name, parent_id, ancestors, sort, org_type, org_status,
  remark, del_flag, create_by, create_time, update_by, update_time
)
SELECT @community_tenant_id, '社区体验', 0, '0', 0, '1', 1,
       'Gitee 社区登录默认组织', 0, 1, NOW(), 1, NOW()
WHERE @community_tenant_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_org
    WHERE tenant_id = @community_tenant_id
      AND org_name = '社区体验'
      AND del_flag = 0
  );

INSERT INTO sys_role (
  tenant_id, role_name, role_key, role_type, org_scope_type, data_scope, sort,
  role_status, is_system, remark, del_flag, create_by, create_time, update_by, update_time
)
SELECT @community_tenant_id, 'Gitee社区体验', 'gitee_community', 2, 1, 2, 10,
       1, 1, 'Gitee Star 用户默认角色，复制演示环境功能菜单', 0, 1, NOW(), 1, NOW()
WHERE @community_tenant_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_role
    WHERE tenant_id = @community_tenant_id
      AND role_key = 'gitee_community'
      AND del_flag = 0
  );

SET @community_role_id := (
  SELECT id FROM sys_role
  WHERE tenant_id = @community_tenant_id
    AND role_key = 'gitee_community'
    AND del_flag = 0
  LIMIT 1
);

SET @source_role_id := (
  SELECT r.id
  FROM sys_role r
  WHERE r.tenant_id = 1
    AND r.del_flag = 0
    AND r.role_status = 1
  ORDER BY CASE
             WHEN r.role_key IN ('common', 'user', 'demo') THEN 0
             WHEN IFNULL(r.is_system, 0) = 0 THEN 1
             ELSE 2
           END,
           (SELECT COUNT(1) FROM sys_role_resource rr WHERE rr.role_id = r.id) DESC,
           r.id ASC
  LIMIT 1
);

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT @community_tenant_id, @community_role_id, rr.resource_id, NOW()
FROM sys_role_resource rr
INNER JOIN sys_resource res ON res.id = rr.resource_id
WHERE @community_tenant_id IS NOT NULL
  AND @community_role_id IS NOT NULL
  AND @source_role_id IS NOT NULL
  AND rr.role_id = @source_role_id
  AND IFNULL(res.path, '') NOT LIKE '/system/tenant%'
  AND IFNULL(res.path, '') NOT LIKE '/system/collaboration%'
  AND IFNULL(res.path, '') NOT LIKE '/system/client%'
  AND IFNULL(res.path, '') NOT LIKE '/job%'
  AND IFNULL(res.path, '') NOT LIKE '/system/online%'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = @community_tenant_id
      AND existing.role_id = @community_role_id
      AND existing.resource_id = rr.resource_id
  );
