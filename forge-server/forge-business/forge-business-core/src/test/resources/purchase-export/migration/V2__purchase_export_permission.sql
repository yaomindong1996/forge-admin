-- Test-only incremental grant. Keep the existing list resources and only grant the target role.
INSERT INTO sys_resource (tenant_id, parent_id, resource_name, resource_type, perms, api_method, api_url, client_code)
SELECT page.tenant_id, page.id, action.resource_name, action.resource_type, action.perms,
       action.api_method, action.api_url, page.client_code
FROM sys_resource page
CROSS JOIN (
    SELECT '导出采购单' AS resource_name, 3 AS resource_type,
           'business:purchaseOrderTest:export' AS perms,
           CAST(NULL AS VARCHAR(10)) AS api_method, CAST(NULL AS VARCHAR(160)) AS api_url
    UNION ALL
    SELECT '导出采购单接口', 4, 'business:purchaseOrderTest:exportApi', 'POST', '/business/sample-purchase-order/export'
) action
WHERE page.tenant_id = 1 AND page.resource_type = 2 AND page.del_flag = 0
  AND page.menu_status = 1 AND page.client_code = 'pc' AND page.path = '/business/purchase-order-test'
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource existing
      WHERE existing.tenant_id = page.tenant_id AND existing.resource_type = action.resource_type
        AND existing.perms = action.perms AND existing.client_code = page.client_code AND existing.del_flag = 0
  );

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id)
SELECT role.tenant_id, role.id, resource.id
FROM sys_role role
JOIN sys_resource resource ON resource.tenant_id = role.tenant_id
WHERE role.tenant_id = 1 AND role.role_key = 'purchasing' AND role.del_flag = 0 AND role.role_status = 1
  AND resource.client_code = 'pc' AND resource.del_flag = 0
  AND ((resource.resource_type = 3 AND resource.perms = 'business:purchaseOrderTest:export')
    OR (resource.resource_type = 4 AND resource.perms = 'business:purchaseOrderTest:exportApi'))
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.tenant_id = role.tenant_id AND existing.role_id = role.id AND existing.resource_id = resource.id
  );
