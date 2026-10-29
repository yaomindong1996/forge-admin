-- 表格报表中心：定义/版本表 + 菜单权限。
-- 回滚：停用菜单 visible=0 并保留定义数据；禁止自动 DROP。

CREATE TABLE IF NOT EXISTS sys_report_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    report_code VARCHAR(80) NOT NULL COMMENT '租户内报表编码',
    report_name VARCHAR(100) NOT NULL COMMENT '报表名称',
    dataset_id BIGINT NOT NULL COMMENT '绑定数据集 ID',
    draft_doc MEDIUMTEXT NOT NULL COMMENT '已校验的设计草稿 JSON',
    draft_revision BIGINT NOT NULL DEFAULT 1 COMMENT '并发修订号',
    design_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CHANGED/PUBLISHED',
    publish_status TINYINT NOT NULL DEFAULT 0 COMMENT '0未发布 1已发布',
    published_version_id BIGINT NULL COMMENT '当前发布版本',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '启停 1启用 0停用',
    remark VARCHAR(500) NULL,
    del_flag BIGINT NOT NULL DEFAULT 0 COMMENT '删除墓碑：有效为 0，删除写主键',
    create_by BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_def_code (tenant_id, report_code, del_flag),
    KEY idx_report_def_dataset (tenant_id, dataset_id, del_flag),
    KEY idx_report_def_update (tenant_id, update_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表格报表定义';

CREATE TABLE IF NOT EXISTS sys_report_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL COMMENT '报表定义 ID',
    version_no INT NOT NULL COMMENT '跨历史永久唯一版本号',
    doc_json MEDIUMTEXT NOT NULL COMMENT '不可变规范化文档',
    doc_hash CHAR(64) NOT NULL COMMENT '规范化文档 SHA-256',
    dataset_id BIGINT NOT NULL COMMENT '发布时数据集快照',
    publish_time DATETIME NOT NULL COMMENT '发布时间',
    del_flag TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除，不释放版本号',
    create_by BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_dept BIGINT NULL,
    update_by BIGINT NOT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_version_no (tenant_id, definition_id, version_no),
    KEY idx_report_version_hash (tenant_id, definition_id, doc_hash, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表格报表发布版本';

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT 1, '报表中心', 0, 1, 8, NULL, NULL, 0,
       0, NULL, '_self', 0, 1, 1, NULL, 'ionicons5:GridOutline',
       NULL, NULL, 0, 1, NULL, '平台级表格报表定义与运行',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '报表中心'
      AND client_code = 'pc'
      AND del_flag = 0
);

SET @report_center_dir_id := (
    SELECT id
    FROM sys_resource
    WHERE tenant_id = 1
      AND resource_type = 1
      AND parent_id = 0
      AND resource_name = '报表中心'
      AND client_code = 'pc'
      AND del_flag = 0
    ORDER BY id
    LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT 1, '报表管理', @report_center_dir_id, 2, 1, '/report-center', 'report-center/index', 0,
       0, NULL, '_self', 0, 1, 1, 'report:definition:view', 'ionicons5:DocumentTextOutline',
       NULL, NULL, 1, 0, NULL, '表格报表定义列表',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @report_center_dir_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource
      WHERE tenant_id = 1
        AND resource_type = 2
        AND path = '/report-center'
        AND client_code = 'pc'
        AND del_flag = 0
  );

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT 1, '报表设计器', @report_center_dir_id, 2, 2, '/report-center/designer', 'report-center/designer', 0,
       0, NULL, '_self', 0, 1, 0, 'report:definition:manage', 'ionicons5:CreateOutline',
       NULL, NULL, 0, 0, NULL, '表格报表设计器（隐藏路由）',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @report_center_dir_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource
      WHERE tenant_id = 1
        AND resource_type = 2
        AND path = '/report-center/designer'
        AND client_code = 'pc'
        AND del_flag = 0
  );

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort, path, component, is_external,
    sso_enabled, sso_target_client, open_target, is_public, menu_status, visible, perms, icon,
    api_method, api_url, keep_alive, always_show, redirect, remark, create_by, create_time,
    update_by, update_time, create_dept, client_code
)
SELECT 1, '报表运行', @report_center_dir_id, 2, 3, '/report-center/run', 'report-center/run', 0,
       0, NULL, '_self', 0, 1, 0, 'report:run', 'ionicons5:PlayOutline',
       NULL, NULL, 0, 0, NULL, '表格报表运行页（隐藏路由）',
       1, NOW(), 1, NOW(), 1, 'pc'
WHERE @report_center_dir_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource
      WHERE tenant_id = 1
        AND resource_type = 2
        AND path = '/report-center/run'
        AND client_code = 'pc'
        AND del_flag = 0
  );

SET @report_list_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/report-center' AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);
SET @report_designer_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/report-center/designer' AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);
SET @report_run_menu_id := (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/report-center/run' AND client_code = 'pc' AND del_flag = 0
    ORDER BY id LIMIT 1
);

-- uk_tenant_resource_active = (tenant_id, resource_type, perms, client_code, del_flag)
-- 同一 perms 在 type=4 下只能一条；用通配 api_url 覆盖同权接口（对齐 V1.0.218）
INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms,
    api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @report_list_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
    SELECT '报表定义查询API' resource_name, 2290 sort, 'report:definition:view' perms,
           NULL api_method, '/report/definition/**' api_url, '定义分页/详情/字段目录等只读接口' remark
    UNION ALL SELECT '报表定义管理API', 2291, 'report:definition:manage',
           NULL, '/report/run/id/**', '草稿预览/导出 API；定义写接口靠 @SaCheckPermission'
    UNION ALL SELECT '报表发布API', 2292, 'report:definition:publish',
           'POST', '/report/definition/:id/publish', '发布报表版本'
    UNION ALL SELECT '报表运行API', 2293, 'report:run',
           NULL, '/report/run/**', '已发布报表元数据/预览/导出'
) seed
WHERE @report_list_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_resource resource
      WHERE resource.tenant_id = 1
        AND resource.resource_type = 4
        AND resource.perms = seed.perms
        AND resource.client_code = 'pc'
        AND resource.del_flag = 0
  );

-- 已有数据集管理权限的角色同步获得报表中心读写与运行
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT seed.tenant_id, seed.role_id, target.id, NOW()
FROM (
    SELECT rr.tenant_id, rr.role_id
    FROM sys_role_resource rr
    INNER JOIN sys_resource r
      ON r.id = rr.resource_id
     AND r.tenant_id = rr.tenant_id
     AND r.del_flag = 0
     AND (
       r.perms LIKE 'data:dataset:%'
       OR r.perms IN ('print:template:view', 'print:template:manage')
     )
) seed
INNER JOIN sys_resource target
  ON target.tenant_id = seed.tenant_id
 AND target.del_flag = 0
 AND target.client_code = 'pc'
 AND (
   target.id IN (@report_center_dir_id, @report_list_menu_id, @report_designer_menu_id, @report_run_menu_id)
   OR target.perms IN (
     'report:definition:view', 'report:definition:manage',
     'report:definition:publish', 'report:run'
   )
 )
WHERE @report_center_dir_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_resource existing
      WHERE existing.tenant_id = seed.tenant_id
        AND existing.role_id = seed.role_id
        AND existing.resource_id = target.id
  );
