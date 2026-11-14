-- SQL 预览必须经过独立 API 权限；已保存数据集还会在 Controller 内校验 MANAGE ACL。
-- 回滚：删除 perms=data:dataset:preview-sql 且 api_url=/data/dataset/preview-sql 的资源及其角色关联。

SET @dataset_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND path = '/data/dataset'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (tenant_id, resource_name, parent_id, resource_type, sort, is_external, open_target,
                          is_public, menu_status, visible, perms, api_method, api_url, keep_alive, always_show,
                          remark, create_by, create_time, update_by, update_time, create_dept, client_code, min_user_type)
SELECT 1, 'SQL数据集预览API', @dataset_menu_id, 4, 12, 0, '_self', 0, 1, 1,
       'data:dataset:preview-sql', 'POST', '/data/dataset/preview-sql', 0, 0,
       '仅数据集或维度管理角色可执行受控只读SQL预览', 1, NOW(), 1, NOW(), 1, 'pc', 2
WHERE @dataset_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource
      WHERE tenant_id = 1
        AND perms = 'data:dataset:preview-sql'
        AND api_method = 'POST'
        AND api_url = '/data/dataset/preview-sql'
        AND del_flag = 0
  );

-- 兼容现有角色：仅继承已有的数据集/维度新增或编辑权限，不向只读角色放开任意 SQL 预览。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT existing_grant.tenant_id, existing_grant.role_id, preview_resource.id, NOW()
FROM sys_role_resource existing_grant
INNER JOIN sys_resource management_resource
        ON management_resource.id = existing_grant.resource_id
       AND management_resource.tenant_id = existing_grant.tenant_id
       AND management_resource.del_flag = 0
       AND management_resource.perms IN (
           'data:dataset:add', 'data:dataset:edit',
           'data:dimension:add', 'data:dimension:edit'
       )
INNER JOIN sys_resource preview_resource
        ON preview_resource.tenant_id = existing_grant.tenant_id
       AND preview_resource.del_flag = 0
       AND preview_resource.perms = 'data:dataset:preview-sql'
       AND preview_resource.api_method = 'POST'
       AND preview_resource.api_url = '/data/dataset/preview-sql'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource duplicate_grant
    WHERE duplicate_grant.tenant_id = existing_grant.tenant_id
      AND duplicate_grant.role_id = existing_grant.role_id
      AND duplicate_grant.resource_id = preview_resource.id
);
