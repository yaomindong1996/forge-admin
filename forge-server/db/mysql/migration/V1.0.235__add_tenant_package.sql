-- 租户套餐（开通模板）：平台级定义 + 组成项 + 租户绑定/开通快照
-- tip 之后首个平行增量；PG / SQL Server 同号脚本必须同步提交。

CREATE TABLE IF NOT EXISTS sys_tenant_package (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    package_code    varchar(64)  NOT NULL COMMENT '套餐编码（平台唯一）',
    package_name    varchar(128) NOT NULL COMMENT '套餐名称',
    package_status  tinyint      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    is_default      tinyint      NOT NULL DEFAULT 0 COMMENT '是否默认套餐：0否 1是',
    description     varchar(500) DEFAULT NULL COMMENT '说明',
    sort            int          NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    remark          varchar(500) DEFAULT NULL COMMENT '备注',
    create_by       bigint       DEFAULT NULL,
    create_time     datetime     DEFAULT NULL,
    create_dept     bigint       DEFAULT NULL,
    update_by       bigint       DEFAULT NULL,
    update_time     datetime     DEFAULT NULL,
    del_flag        bigint       NOT NULL DEFAULT 0 COMMENT '删除标志：0正常，删除后写主键',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_package_code_active (package_code, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐定义（平台级）';

CREATE TABLE IF NOT EXISTS sys_tenant_package_item (
    id           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    package_id   bigint       NOT NULL COMMENT '套餐ID',
    item_type    varchar(32)  NOT NULL COMMENT 'BOOTSTRAP / RESOURCE / QUOTA',
    item_code    varchar(128) NOT NULL COMMENT '项编码（如 dictAndConfig、userLimit）',
    item_value   text         DEFAULT NULL COMMENT '项值（JSON 或标量文本）',
    sort         int          NOT NULL DEFAULT 0,
    create_by    bigint       DEFAULT NULL,
    create_time  datetime     DEFAULT NULL,
    create_dept  bigint       DEFAULT NULL,
    update_by    bigint       DEFAULT NULL,
    update_time  datetime     DEFAULT NULL,
    del_flag     bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_tenant_package_item_pkg (package_id, del_flag),
    UNIQUE KEY uk_tenant_package_item (package_id, item_type, item_code, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐组成项';

CREATE TABLE IF NOT EXISTS sys_tenant_package_grant (
    id              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id       bigint       NOT NULL COMMENT '租户ID',
    package_id      bigint       NOT NULL COMMENT '开通时套餐ID',
    package_code    varchar(64)  NOT NULL COMMENT '开通时套餐编码快照',
    snapshot_json   mediumtext   NOT NULL COMMENT '开通快照 JSON（bootstrap/资源/配额）',
    grant_status    varchar(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE；二期扩展 CHANGED/EXPIRED',
    granted_at      datetime     NOT NULL COMMENT '开通时间',
    create_by       bigint       DEFAULT NULL,
    create_time     datetime     DEFAULT NULL,
    create_dept     bigint       DEFAULT NULL,
    update_by       bigint       DEFAULT NULL,
    update_time     datetime     DEFAULT NULL,
    del_flag        bigint       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_tenant_package_grant_tenant (tenant_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户套餐开通快照';

-- sys_tenant.package_id
SET @pkg_col_exists = (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_tenant' AND column_name = 'package_id'
);
SET @ddl = IF(
  @pkg_col_exists = 0,
  'ALTER TABLE sys_tenant ADD COLUMN package_id bigint DEFAULT NULL COMMENT ''当前绑定套餐ID'' AFTER default_business_datasource_code',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 种子：standard / minimal
INSERT INTO sys_tenant_package (
    package_code, package_name, package_status, is_default, description, sort,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 'standard', '标准版', 1, 1, '含全部推荐初始化包，适合常规开户', 10,
       1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_tenant_package WHERE package_code = 'standard' AND del_flag = 0
);

INSERT INTO sys_tenant_package (
    package_code, package_name, package_status, is_default, description, sort,
    create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 'minimal', '精简版', 1, 0, '仅字典配置、根组织与租户管理员角色', 20,
       1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_tenant_package WHERE package_code = 'minimal' AND del_flag = 0
);

SET @standard_id = (SELECT id FROM sys_tenant_package WHERE package_code = 'standard' AND del_flag = 0 LIMIT 1);
SET @minimal_id = (SELECT id FROM sys_tenant_package WHERE package_code = 'minimal' AND del_flag = 0 LIMIT 1);

INSERT INTO sys_tenant_package_item (package_id, item_type, item_code, item_value, sort, create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT @standard_id, seed.item_type, seed.item_code, seed.item_value, seed.sort, 1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 'BOOTSTRAP' item_type, 'dictAndConfig' item_code, 'true' item_value, 1 sort
    UNION ALL SELECT 'BOOTSTRAP', 'rootOrg', 'true', 2
    UNION ALL SELECT 'BOOTSTRAP', 'tenantAdminRole', 'true', 3
    UNION ALL SELECT 'BOOTSTRAP', 'messageTemplates', 'true', 4
    UNION ALL SELECT 'BOOTSTRAP', 'flowAssist', 'true', 5
    UNION ALL SELECT 'BOOTSTRAP', 'dataScopePresets', 'true', 6
    UNION ALL SELECT 'QUOTA', 'userLimit', '0', 10
) seed
WHERE @standard_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_tenant_package_item i
      WHERE i.package_id = @standard_id AND i.item_type = seed.item_type
        AND i.item_code = seed.item_code AND i.del_flag = 0
  );

INSERT INTO sys_tenant_package_item (package_id, item_type, item_code, item_value, sort, create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT @minimal_id, seed.item_type, seed.item_code, seed.item_value, seed.sort, 1, NOW(), 1, NOW(), 1, 0
FROM (
    SELECT 'BOOTSTRAP' item_type, 'dictAndConfig' item_code, 'true' item_value, 1 sort
    UNION ALL SELECT 'BOOTSTRAP', 'rootOrg', 'true', 2
    UNION ALL SELECT 'BOOTSTRAP', 'tenantAdminRole', 'true', 3
    UNION ALL SELECT 'QUOTA', 'userLimit', '0', 10
) seed
WHERE @minimal_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_tenant_package_item i
      WHERE i.package_id = @minimal_id AND i.item_type = seed.item_type
        AND i.item_code = seed.item_code AND i.del_flag = 0
  );

-- 菜单：租户套餐（挂在系统管理下，紧邻租户管理）
SET @system_menu_id = (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/system' AND del_flag = 0
    ORDER BY id LIMIT 1
);
SET @tenant_menu_id = (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/system/tenant' AND del_flag = 0
    ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, path, component, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms,
    keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code, del_flag
)
SELECT 1, '租户套餐', @system_menu_id, '/system/tenant-package', 'system/tenant-package', 2,
       COALESCE((SELECT sort FROM sys_resource WHERE id = @tenant_menu_id), 50) + 1,
       0, '_self', 0, 1, 1, 'system:tenantPackage:view',
       0, 0, '租户开通模板管理',
       1, NOW(), 1, NOW(), 1, 'pc', 0
WHERE @system_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource
      WHERE tenant_id = 1 AND path = '/system/tenant-package' AND del_flag = 0
  );

SET @pkg_menu_id = (
    SELECT id FROM sys_resource
    WHERE tenant_id = 1 AND path = '/system/tenant-package' AND del_flag = 0
    ORDER BY id LIMIT 1
);

INSERT INTO sys_resource (
    tenant_id, resource_name, parent_id, resource_type, sort,
    is_external, open_target, is_public, menu_status, visible, perms,
    api_method, api_url, keep_alive, always_show, remark,
    create_by, create_time, update_by, update_time, create_dept, client_code, del_flag
)
SELECT 1, seed.resource_name, @pkg_menu_id, 4, seed.sort,
       0, '_self', 0, 1, 0, seed.perms,
       seed.api_method, seed.api_url, 0, 0, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc', 0
FROM (
    SELECT '套餐分页' resource_name, 1 sort, 'system:tenantPackage:view' perms, 'GET' api_method,
           '/system/tenant-package/page' api_url, '分页查询租户套餐' remark
    UNION ALL
    SELECT '套餐详情', 2, 'system:tenantPackage:view', 'GET', '/system/tenant-package/:id', '查询套餐详情'
    UNION ALL
    SELECT '套餐选项', 3, 'system:tenantPackage:view', 'GET', '/system/tenant-package/options', '启用套餐下拉'
    UNION ALL
    SELECT '新增套餐', 4, 'system:tenantPackage:manage', 'POST', '/system/tenant-package/add', '新增租户套餐'
    UNION ALL
    SELECT '修改套餐', 5, 'system:tenantPackage:manage', 'POST', '/system/tenant-package/edit', '修改租户套餐'
    UNION ALL
    SELECT '删除套餐', 6, 'system:tenantPackage:manage', 'POST', '/system/tenant-package/remove', '删除租户套餐'
) seed
WHERE @pkg_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_resource r
      WHERE r.tenant_id = 1 AND r.resource_type = 4 AND r.api_url = seed.api_url AND r.del_flag = 0
  );

-- 超管角色同步菜单权限
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT role.tenant_id, role.id, resource.id, NOW()
FROM sys_role role
JOIN sys_resource resource
  ON resource.del_flag = 0
 AND (
     resource.path = '/system/tenant-package'
     OR resource.api_url LIKE '/system/tenant-package%'
 )
WHERE role.del_flag = 0
  AND role.role_key = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.role_id = role.id AND existing.resource_id = resource.id
  );
