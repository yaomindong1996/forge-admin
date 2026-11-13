-- 租户级「已安装内容资产」库：市场打印/表单等模板安装一次，各设计器选用，避免每次对着画布塞文件。
-- 回滚：停用接口后保留表数据；禁止自动 DROP。

CREATE TABLE IF NOT EXISTS sys_content_installed_asset (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    kind VARCHAR(32) NOT NULL COMMENT '资产类型：print/form/print-component',
    asset_key VARCHAR(128) NOT NULL COMMENT '租户内同类唯一键（市场 key 或派生）',
    asset_name VARCHAR(200) NOT NULL COMMENT '展示名称',
    asset_version VARCHAR(64) NULL COMMENT '包版本',
    package_json MEDIUMTEXT NOT NULL COMMENT '原始内容包 JSON（含 kind/name/payload）',
    source_file_name VARCHAR(255) NULL COMMENT '安装时文件名',
    remark VARCHAR(500) NULL COMMENT '备注',
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '删除墓碑：有效为 0，删除写主键',
    create_by BIGINT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_dept BIGINT NULL COMMENT '创建部门',
    update_by BIGINT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_asset_key (tenant_id, kind, asset_key, del_flag),
    KEY idx_content_asset_kind (tenant_id, kind, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户已安装内容资产（打印/表单等模板库）';

SET @system_menu_id = (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND path = '/system'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms,
    api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @system_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 0, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
    SELECT '内容资产-分页' resource_name, 2260 sort,
           'system:contentAsset:view' perms, 'GET' api_method,
           '/system/content-assets/page' api_url, '分页查询已安装模板资产' remark
    UNION ALL
    SELECT '内容资产-详情', 2261,
           'system:contentAsset:view', 'GET',
           '/system/content-assets/:id', '查询已安装模板资产详情'
    UNION ALL
    SELECT '内容资产-安装', 2262,
           'system:contentAsset:manage', 'POST',
           '/system/content-assets', '安装或更新模板资产'
    UNION ALL
    SELECT '内容资产-删除', 2263,
           'system:contentAsset:manage', 'DELETE',
           '/system/content-assets/:id', '卸载已安装模板资产'
) seed
WHERE @system_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource resource
      WHERE resource.tenant_id = 1
        AND resource.resource_type = 4
        AND resource.api_method = seed.api_method
        AND resource.api_url = seed.api_url
        AND resource.del_flag = 0
  );

-- 已有打印管理或应用中心权限的角色同步获得内容资产读写
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, asset_resource.id, NOW()
FROM (
    SELECT rr.tenant_id, rr.role_id
    FROM sys_role_resource rr
    INNER JOIN sys_resource r
      ON r.id = rr.resource_id
     AND r.tenant_id = rr.tenant_id
     AND r.del_flag = 0
     AND (
       r.perms IN (
         'print:template:view',
         'print:template:manage',
         'ai:businessApplication:runtime',
         'ai:businessObject:edit',
         'ai:businessObject:design'
       )
       OR r.path IN ('/app-center', '/print')
     )
) seed
INNER JOIN sys_resource asset_resource
  ON asset_resource.tenant_id = seed.tenant_id
 AND asset_resource.del_flag = 0
 AND asset_resource.perms IN ('system:contentAsset:view', 'system:contentAsset:manage')
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_role_resource existing
    WHERE existing.tenant_id = seed.tenant_id
      AND existing.role_id = seed.role_id
      AND existing.resource_id = asset_resource.id
);
