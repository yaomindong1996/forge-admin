-- Add explicit organization scope mode for roles.
-- 1 = tenant global, 2 = specified organizations.

SET @column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_role'
      AND COLUMN_NAME = 'org_scope_type'
);
SET @role_type_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_role'
      AND COLUMN_NAME = 'role_type'
);
SET @sql = IF(@column_exists = 0,
    IF(@role_type_exists > 0,
        'ALTER TABLE sys_role ADD COLUMN org_scope_type tinyint NOT NULL DEFAULT 1 COMMENT ''组织适用范围类型（1租户全局 2指定组织）'' AFTER role_type',
        'ALTER TABLE sys_role ADD COLUMN org_scope_type tinyint NOT NULL DEFAULT 1 COMMENT ''组织适用范围类型（1租户全局 2指定组织）'' AFTER role_key'),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_role r
LEFT JOIN (
    SELECT role_scope.tenant_id,
           role_scope.role_id,
           role_scope.role_org_count,
           COALESCE(org_scope.org_count, 0) AS org_count
    FROM (
        SELECT r0.tenant_id,
               r0.id AS role_id,
               COUNT(DISTINCT active_org.id) AS role_org_count
        FROM sys_role r0
        LEFT JOIN sys_role_org ro
            ON ro.tenant_id = r0.tenant_id
           AND ro.role_id = r0.id
        LEFT JOIN sys_org active_org
            ON active_org.tenant_id = ro.tenant_id
           AND active_org.id = ro.org_id
           AND active_org.del_flag = 0
        WHERE r0.del_flag = 0
        GROUP BY r0.tenant_id, r0.id
    ) role_scope
    LEFT JOIN (
        SELECT tenant_id, COUNT(1) AS org_count
        FROM sys_org
        WHERE del_flag = 0
        GROUP BY tenant_id
    ) org_scope
        ON org_scope.tenant_id = role_scope.tenant_id
) scope
    ON scope.tenant_id = r.tenant_id
   AND scope.role_id = r.id
SET r.org_scope_type = CASE
    WHEN scope.org_count > 0 AND scope.role_org_count >= scope.org_count THEN 1
    ELSE 2
END
WHERE r.del_flag = 0;

DELETE ro
FROM sys_role_org ro
INNER JOIN sys_role r
    ON r.tenant_id = ro.tenant_id
   AND r.id = ro.role_id
WHERE r.del_flag = 0
  AND r.org_scope_type = 1;
