-- 租户套餐（开通模板）：与 MySQL V1.0.235 平行
-- sqlcmd 默认可能关闭 QUOTED_IDENTIFIER；sys_resource 等表索引要求必须 ON。
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

IF OBJECT_ID(N'sys_tenant_package', N'U') IS NULL
BEGIN
    CREATE TABLE sys_tenant_package (
        id              bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        package_code    nvarchar(64)  NOT NULL,
        package_name    nvarchar(128) NOT NULL,
        package_status  smallint      NOT NULL CONSTRAINT DF_tenant_package_status DEFAULT (1),
        is_default      smallint      NOT NULL CONSTRAINT DF_tenant_package_default DEFAULT (0),
        description     nvarchar(500) NULL,
        sort            int           NOT NULL CONSTRAINT DF_tenant_package_sort DEFAULT (0),
        remark          nvarchar(500) NULL,
        create_by       bigint NULL,
        create_time     datetime2 NULL,
        create_dept     bigint NULL,
        update_by       bigint NULL,
        update_time     datetime2 NULL,
        del_flag        bigint        NOT NULL CONSTRAINT DF_tenant_package_del DEFAULT (0)
    );
    CREATE UNIQUE INDEX uk_tenant_package_code_active ON sys_tenant_package (package_code, del_flag);
END
GO

IF OBJECT_ID(N'sys_tenant_package_item', N'U') IS NULL
BEGIN
    CREATE TABLE sys_tenant_package_item (
        id           bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        package_id   bigint       NOT NULL,
        item_type    nvarchar(32) NOT NULL,
        item_code    nvarchar(128) NOT NULL,
        item_value   nvarchar(max) NULL,
        sort         int          NOT NULL CONSTRAINT DF_tenant_package_item_sort DEFAULT (0),
        create_by    bigint NULL,
        create_time  datetime2 NULL,
        create_dept  bigint NULL,
        update_by    bigint NULL,
        update_time  datetime2 NULL,
        del_flag     bigint       NOT NULL CONSTRAINT DF_tenant_package_item_del DEFAULT (0)
    );
    CREATE INDEX idx_tenant_package_item_pkg ON sys_tenant_package_item (package_id, del_flag);
    CREATE UNIQUE INDEX uk_tenant_package_item ON sys_tenant_package_item (package_id, item_type, item_code, del_flag);
END
GO

IF OBJECT_ID(N'sys_tenant_package_grant', N'U') IS NULL
BEGIN
    CREATE TABLE sys_tenant_package_grant (
        id              bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        tenant_id       bigint       NOT NULL,
        package_id      bigint       NOT NULL,
        package_code    nvarchar(64) NOT NULL,
        snapshot_json   nvarchar(max) NOT NULL,
        grant_status    nvarchar(32) NOT NULL CONSTRAINT DF_tenant_package_grant_status DEFAULT (N'ACTIVE'),
        granted_at      datetime2    NOT NULL,
        create_by       bigint NULL,
        create_time     datetime2 NULL,
        create_dept     bigint NULL,
        update_by       bigint NULL,
        update_time     datetime2 NULL,
        del_flag        bigint       NOT NULL CONSTRAINT DF_tenant_package_grant_del DEFAULT (0)
    );
    CREATE INDEX idx_tenant_package_grant_tenant ON sys_tenant_package_grant (tenant_id, del_flag);
END
GO

IF COL_LENGTH('sys_tenant', 'package_id') IS NULL
BEGIN
    ALTER TABLE sys_tenant ADD package_id bigint NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys_tenant_package WHERE package_code = N'standard' AND del_flag = 0)
BEGIN
    INSERT INTO sys_tenant_package (
        package_code, package_name, package_status, is_default, description, sort,
        create_by, create_time, update_by, update_time, create_dept, del_flag
    ) VALUES (
        N'standard', N'标准版', 1, 1, N'含全部推荐初始化包，适合常规开户', 10,
        1, SYSDATETIME(), 1, SYSDATETIME(), 1, 0
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys_tenant_package WHERE package_code = N'minimal' AND del_flag = 0)
BEGIN
    INSERT INTO sys_tenant_package (
        package_code, package_name, package_status, is_default, description, sort,
        create_by, create_time, update_by, update_time, create_dept, del_flag
    ) VALUES (
        N'minimal', N'精简版', 1, 0, N'仅字典配置、根组织与租户管理员角色', 20,
        1, SYSDATETIME(), 1, SYSDATETIME(), 1, 0
    );
END
GO

INSERT INTO sys_tenant_package_item (package_id, item_type, item_code, item_value, sort, create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT p.id, v.item_type, v.item_code, v.item_value, v.sort, 1, SYSDATETIME(), 1, SYSDATETIME(), 1, 0
FROM sys_tenant_package p
CROSS JOIN (VALUES
    (N'BOOTSTRAP', N'dictAndConfig', N'true', 1),
    (N'BOOTSTRAP', N'rootOrg', N'true', 2),
    (N'BOOTSTRAP', N'tenantAdminRole', N'true', 3),
    (N'BOOTSTRAP', N'messageTemplates', N'true', 4),
    (N'BOOTSTRAP', N'flowAssist', N'true', 5),
    (N'BOOTSTRAP', N'dataScopePresets', N'true', 6),
    (N'QUOTA', N'userLimit', N'0', 10)
) v(item_type, item_code, item_value, sort)
WHERE p.package_code = N'standard' AND p.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_tenant_package_item i
      WHERE i.package_id = p.id AND i.item_type = v.item_type
        AND i.item_code = v.item_code AND i.del_flag = 0
  );
GO

INSERT INTO sys_tenant_package_item (package_id, item_type, item_code, item_value, sort, create_by, create_time, update_by, update_time, create_dept, del_flag)
SELECT p.id, v.item_type, v.item_code, v.item_value, v.sort, 1, SYSDATETIME(), 1, SYSDATETIME(), 1, 0
FROM sys_tenant_package p
CROSS JOIN (VALUES
    (N'BOOTSTRAP', N'dictAndConfig', N'true', 1),
    (N'BOOTSTRAP', N'rootOrg', N'true', 2),
    (N'BOOTSTRAP', N'tenantAdminRole', N'true', 3),
    (N'QUOTA', N'userLimit', N'0', 10)
) v(item_type, item_code, item_value, sort)
WHERE p.package_code = N'minimal' AND p.del_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_tenant_package_item i
      WHERE i.package_id = p.id AND i.item_type = v.item_type
        AND i.item_code = v.item_code AND i.del_flag = 0
  );
GO

IF NOT EXISTS (SELECT 1 FROM sys_resource WHERE tenant_id = 1 AND path = N'/system/tenant-package' AND del_flag = 0)
BEGIN
    DECLARE @tenant_menu_id bigint = (SELECT TOP 1 id FROM sys_resource WHERE tenant_id = 1 AND path = N'/system/tenant' AND del_flag = 0 ORDER BY id);
    DECLARE @system_menu_id bigint = (SELECT parent_id FROM sys_resource WHERE id = @tenant_menu_id);
    DECLARE @tenant_sort int = (SELECT sort FROM sys_resource WHERE id = @tenant_menu_id);
    IF @system_menu_id IS NOT NULL
    BEGIN
        INSERT INTO sys_resource (
            tenant_id, resource_name, parent_id, path, component, resource_type, sort,
            is_external, open_target, is_public, menu_status, visible, perms,
            keep_alive, always_show, remark,
            create_by, create_time, update_by, update_time, create_dept, client_code, del_flag
        ) VALUES (
            1, N'租户套餐', @system_menu_id, N'/system/tenant-package', N'system/tenant-package', 2,
            ISNULL(@tenant_sort, 50) + 1,
            0, N'_self', 0, 1, 1, N'system:tenantPackage:view',
            0, 0, N'租户开通模板管理',
            1, SYSDATETIME(), 1, SYSDATETIME(), 1, N'pc', 0
        );
    END
END
GO

DECLARE @pkg_menu_id bigint = (SELECT TOP 1 id FROM sys_resource WHERE tenant_id = 1 AND path = N'/system/tenant-package' AND del_flag = 0 ORDER BY id);
IF @pkg_menu_id IS NOT NULL
BEGIN
    INSERT INTO sys_resource (
        tenant_id, resource_name, parent_id, resource_type, sort,
        is_external, open_target, is_public, menu_status, visible, perms,
        api_method, api_url, keep_alive, always_show, remark,
        create_by, create_time, update_by, update_time, create_dept, client_code, del_flag
    )
    SELECT 1, v.resource_name, @pkg_menu_id, 4, v.sort,
           0, N'_self', 0, 1, 0, v.perms,
           v.api_method, v.api_url, 0, 0, v.remark,
           1, SYSDATETIME(), 1, SYSDATETIME(), 1, N'pc', 0
    FROM (VALUES
        (N'套餐分页', 1, N'system:tenantPackage:page', N'GET', N'/system/tenant-package/page', N'分页查询租户套餐'),
        (N'套餐详情', 2, N'system:tenantPackage:detail', N'GET', N'/system/tenant-package/:id', N'查询套餐详情'),
        (N'套餐选项', 3, N'system:tenantPackage:options', N'GET', N'/system/tenant-package/options', N'启用套餐下拉'),
        (N'新增套餐', 4, N'system:tenantPackage:add', N'POST', N'/system/tenant-package/add', N'新增租户套餐'),
        (N'修改套餐', 5, N'system:tenantPackage:edit', N'POST', N'/system/tenant-package/edit', N'修改租户套餐'),
        (N'删除套餐', 6, N'system:tenantPackage:remove', N'POST', N'/system/tenant-package/remove', N'删除租户套餐')
    ) v(resource_name, sort, perms, api_method, api_url, remark)
    WHERE NOT EXISTS (
        SELECT 1 FROM sys_resource r
        WHERE r.tenant_id = 1 AND r.resource_type = 4 AND r.api_url = v.api_url AND r.del_flag = 0
    );
END
GO

INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT role.tenant_id, role.id, resource.id, SYSDATETIME()
FROM sys_role role
JOIN sys_resource resource
  ON resource.del_flag = 0
 AND (
     resource.path = N'/system/tenant-package'
     OR resource.api_url LIKE N'/system/tenant-package%'
 )
WHERE role.del_flag = 0
  AND role.role_key = N'admin'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource existing
      WHERE existing.role_id = role.id AND existing.resource_id = resource.id
  );
GO
