-- 低代码应用门户产品化：应用门户访问标识、门户配置和 AI 助理配置。
-- 迁移必须兼容 V1.0.27 已存在的应用表，以及重复执行和部分历史修复场景。

SET @application_table_exists = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_application'
);

SET @sql = IF(
  @application_table_exists > 0
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND COLUMN_NAME = 'portal_slug'
  ),
  'ALTER TABLE ai_business_application ADD COLUMN portal_slug varchar(50) DEFAULT NULL COMMENT ''门户访问 slug'' AFTER application_code',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  @application_table_exists > 0
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND COLUMN_NAME = 'portal_config'
  ),
  'ALTER TABLE ai_business_application ADD COLUMN portal_config json DEFAULT NULL COMMENT ''门户主题、水印和导航配置'' AFTER options',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  @application_table_exists > 0
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND COLUMN_NAME = 'ai_assistant_config'
  ),
  'ALTER TABLE ai_business_application ADD COLUMN ai_assistant_config json DEFAULT NULL COMMENT ''应用 AI 助理绑定配置'' AFTER portal_config',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 先完成存量回填，再创建唯一索引。超过 50 字符的历史编码保留稳定摘要，避免截断碰撞。
UPDATE ai_business_application
SET portal_slug = CASE
  WHEN CHAR_LENGTH(application_code) <= 50 THEN application_code
  ELSE CONCAT(LEFT(application_code, 41), '_', LEFT(MD5(application_code), 8))
END
WHERE @application_table_exists > 0
  AND (portal_slug IS NULL OR portal_slug = '')
  AND application_code IS NOT NULL;

UPDATE ai_business_application target_row
INNER JOIN (
  SELECT tenant_id, portal_slug, del_flag, MIN(id) AS keep_id
  FROM ai_business_application
  WHERE portal_slug IS NOT NULL
    AND portal_slug <> ''
  GROUP BY tenant_id, portal_slug, del_flag
  HAVING COUNT(1) > 1
) duplicate_row
  ON duplicate_row.tenant_id = target_row.tenant_id
 AND duplicate_row.portal_slug = target_row.portal_slug
 AND duplicate_row.del_flag = target_row.del_flag
SET target_row.portal_slug = CONCAT(
  LEFT(target_row.portal_slug, 37), '_', RIGHT(MD5(CAST(target_row.id AS CHAR)), 12)
)
WHERE target_row.id <> duplicate_row.keep_id;

UPDATE ai_business_application
SET portal_config = JSON_OBJECT()
WHERE portal_config IS NULL;

SET @sql = IF(
  @application_table_exists > 0
  AND NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND INDEX_NAME = 'uk_ai_business_application_portal_slug_active'
  ),
  'ALTER TABLE ai_business_application ADD UNIQUE KEY uk_ai_business_application_portal_slug_active (tenant_id, portal_slug, del_flag)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 手写动态门户路由必须进入权限路由树，但不显示在控制台菜单。
SET @application_menu_id = (
  SELECT id FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type IN (1, 2)
    AND path = '/app-center'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  path, component, is_external, sso_enabled, sso_target_client,
  open_target, is_public, menu_status, visible, perms, icon,
  keep_alive, always_show, redirect, remark,
  create_by, create_time, update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, COALESCE(@application_menu_id, 0), 2, seed.sort,
       seed.path, seed.component, 0, 0, NULL, '_self', 0, 1, 0, seed.perms,
       'ionicons5:AppsOutline', 0, 0, NULL, seed.remark,
       1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '应用门户' resource_name, 93 sort,
         '/app/:applicationCodeOrSlug' path,
         'app-center/application-portal' component,
         'ai:businessApplication:portal' perms,
         '已发布业务应用独立门户隐藏路由' remark
  UNION ALL
  SELECT '应用设置', 94, '/app-center/application/:applicationCode/settings',
         'app-center/application-settings.[applicationCode]',
         'ai:businessApplication:settings', '业务应用门户设置隐藏路由'
  UNION ALL
  SELECT '应用发布', 95, '/app-center/application/:applicationCode/publish',
         'app-center/application-publish.[applicationCode]',
         'ai:businessApplication:publishPage', '业务应用发布管理隐藏路由'
) seed
WHERE @application_table_exists > 0
  AND @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.resource_type = 2
      AND r.path = seed.path
      AND r.del_flag = 0
  );

-- 只给已有业务应用查看权限的角色继承门户、设置和发布路由，不扩大应用数据访问范围。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, role_resource.role_id, portal_resource.id, NOW()
FROM sys_role_resource role_resource
INNER JOIN sys_resource list_permission
  ON list_permission.tenant_id = 1
 AND list_permission.id = role_resource.resource_id
 AND list_permission.perms = 'ai:businessApplication:list'
 AND list_permission.del_flag = 0
INNER JOIN sys_resource portal_resource
  ON portal_resource.tenant_id = 1
 AND portal_resource.resource_type = 2
 AND portal_resource.perms IN (
   'ai:businessApplication:portal',
   'ai:businessApplication:settings',
   'ai:businessApplication:publishPage'
 )
 AND portal_resource.del_flag = 0
WHERE role_resource.tenant_id = 1
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_resource existing
    WHERE existing.tenant_id = 1
      AND existing.role_id = role_resource.role_id
      AND existing.resource_id = portal_resource.id
  );
