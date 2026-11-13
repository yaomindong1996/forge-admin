-- sys_resource 的菜单权限码需要按客户端隔离。
-- 页面发布会为同一页面分别投影 pc 和 h5 资源；旧索引未包含 client_code，
-- 导致第二个客户端资源无法插入并报 uk_tenant_resource_active 冲突。

-- 历史资源未填写客户端时按现有默认值归一为管理端，避免迁移后重复生成一条 pc 资源。
UPDATE sys_resource
SET client_code = 'pc'
WHERE client_code IS NULL OR TRIM(client_code) = '';

SET @resource_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND INDEX_NAME = 'uk_tenant_resource_active'
);
SET @resource_index_has_client = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND INDEX_NAME = 'uk_tenant_resource_active'
      AND COLUMN_NAME = 'client_code'
);

-- 只有旧索引存在且未包含 client_code 时才删除，重复执行不会反复重建索引。
SET @sql = IF(
    @resource_index_exists > 0 AND @resource_index_has_client = 0,
    'ALTER TABLE sys_resource DROP INDEX uk_tenant_resource_active',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @resource_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND INDEX_NAME = 'uk_tenant_resource_active'
);
SET @sql = IF(
    @resource_index_exists = 0,
    'ALTER TABLE sys_resource ADD UNIQUE INDEX uk_tenant_resource_active (tenant_id, resource_type, perms, client_code, del_flag)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
