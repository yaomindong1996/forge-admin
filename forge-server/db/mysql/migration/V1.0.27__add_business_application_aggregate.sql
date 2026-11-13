-- 应用优先低代码工作台 Phase 1：新增真实业务应用聚合，并兼容现有访问入口。

CREATE TABLE IF NOT EXISTS ai_business_application (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_code varchar(128) NOT NULL COMMENT '业务应用编码',
  application_name varchar(128) NOT NULL COMMENT '业务应用名称',
  suite_code varchar(128) NOT NULL COMMENT '所属业务域编码',
  icon varchar(255) DEFAULT NULL COMMENT '应用图标',
  description varchar(500) DEFAULT NULL COMMENT '应用说明',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  design_status varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '设计状态：DRAFT/READY/PUBLISHED/CHANGED',
  last_publish_version int DEFAULT NULL COMMENT '最近发布版本号',
  last_publish_time datetime DEFAULT NULL COMMENT '最近发布时间',
  options json DEFAULT NULL COMMENT '扩展配置，不保存敏感密钥',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_application_code_active (tenant_id, application_code, logic_delete_active),
  KEY idx_ai_business_application_suite (tenant_id, suite_code, status, design_status, del_flag),
  KEY idx_ai_business_application_update (tenant_id, update_time, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码业务应用聚合';

CREATE TABLE IF NOT EXISTS ai_business_application_object (
  id bigint NOT NULL COMMENT '主键ID',
  tenant_id bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  application_id bigint NOT NULL COMMENT '业务应用ID',
  object_id bigint NOT NULL COMMENT '业务对象ID',
  object_role varchar(32) NOT NULL COMMENT '对象角色：PRIMARY/DETAIL/REFERENCE/SHARED',
  sort_order int NOT NULL DEFAULT 0 COMMENT '应用内排序',
  options json DEFAULT NULL COMMENT '应用内对象配置',
  del_flag char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
  logic_delete_active tinyint GENERATED ALWAYS AS (CASE WHEN del_flag = '0' THEN 1 ELSE NULL END) STORED,
  create_by bigint DEFAULT NULL COMMENT '创建人',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  create_dept bigint DEFAULT NULL COMMENT '创建部门',
  update_by bigint DEFAULT NULL COMMENT '更新人',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_business_application_object_active (tenant_id, application_id, object_id, logic_delete_active),
  KEY idx_ai_business_application_object_app (tenant_id, application_id, object_role, sort_order, del_flag),
  KEY idx_ai_business_application_object_object (tenant_id, object_id, del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务应用与业务对象关联';

SET @app_table_exists = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_app'
);
SET @application_id_exists = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_app'
    AND COLUMN_NAME = 'application_id'
);
SET @sql = IF(@app_table_exists > 0 AND @application_id_exists = 0,
  'ALTER TABLE ai_business_app ADD COLUMN application_id bigint DEFAULT NULL COMMENT ''归属业务应用聚合ID'' AFTER app_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @application_index_exists = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_app'
    AND INDEX_NAME = 'idx_ai_business_app_application'
);
SET @sql = IF(@app_table_exists > 0 AND @application_index_exists = 0,
  'ALTER TABLE ai_business_app ADD INDEX idx_ai_business_app_application (tenant_id, application_id, status, sort_order)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (
  tenant_id, dict_name, dict_type, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_name, seed.dict_type, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, '业务应用设计状态' dict_name,
         'ai_business_application_design_status' dict_type,
         '业务应用草稿、就绪、发布和变更状态' remark
  UNION ALL
  SELECT 1, '业务应用对象角色', 'ai_business_application_object_role',
         '业务对象在应用内的主对象、明细、引用和共享角色'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_type t
  WHERE t.tenant_id = seed.tenant_id
    AND t.dict_type = seed.dict_type
);

INSERT INTO sys_dict_data (
  tenant_id, dict_sort, dict_label, dict_value, dict_type,
  css_class, list_class, is_default, dict_status, remark,
  create_by, create_time, update_by, update_time, create_dept
)
SELECT seed.tenant_id, seed.dict_sort, seed.dict_label, seed.dict_value, seed.dict_type,
       NULL, seed.list_class, seed.is_default, 1, seed.remark,
       1, NOW(), 1, NOW(), 1
FROM (
  SELECT 1 tenant_id, 1 dict_sort, '草稿' dict_label, 'DRAFT' dict_value,
         'ai_business_application_design_status' dict_type, 'default' list_class, 'Y' is_default,
         '应用仍在设计中' remark
  UNION ALL SELECT 1, 2, '待发布', 'READY', 'ai_business_application_design_status', 'info', 'N', '应用已满足基础发布条件'
  UNION ALL SELECT 1, 3, '已发布', 'PUBLISHED', 'ai_business_application_design_status', 'success', 'N', '应用当前版本已发布'
  UNION ALL SELECT 1, 4, '有未发布变更', 'CHANGED', 'ai_business_application_design_status', 'warning', 'N', '已发布应用存在草稿变更'
  UNION ALL SELECT 1, 1, '主对象', 'PRIMARY', 'ai_business_application_object_role', 'success', 'Y', '应用的唯一主业务对象'
  UNION ALL SELECT 1, 2, '明细对象', 'DETAIL', 'ai_business_application_object_role', 'info', 'N', '主对象的明细或子对象'
  UNION ALL SELECT 1, 3, '引用对象', 'REFERENCE', 'ai_business_application_object_role', 'warning', 'N', '应用引用的共享查找对象'
  UNION ALL SELECT 1, 4, '共享对象', 'SHARED', 'ai_business_application_object_role', 'default', 'N', '应用使用的其他共享对象'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_dict_data d
  WHERE d.tenant_id = seed.tenant_id
    AND d.dict_type = seed.dict_type
    AND d.dict_value = seed.dict_value
);

SET @application_menu_id = (
  SELECT id
  FROM sys_resource
  WHERE tenant_id = 1
    AND resource_type = 2
    AND path = '/app-center'
    AND del_flag = 0
  ORDER BY id
  LIMIT 1
);

INSERT INTO sys_resource (
  tenant_id, resource_name, parent_id, resource_type, sort,
  is_external, open_target, is_public, menu_status, visible, perms,
  keep_alive, always_show, remark, create_by, create_time,
  update_by, update_time, create_dept, client_code
)
SELECT 1, seed.resource_name, @application_menu_id, 3, seed.sort,
       0, '_self', 0, 1, 1, seed.perms,
       0, 0, seed.remark, 1, NOW(), 1, NOW(), 1, 'pc'
FROM (
  SELECT '查看业务应用' resource_name, 51 sort, 'ai:businessApplication:list' perms, '查询业务应用聚合' remark
  UNION ALL SELECT '新增业务应用', 52, 'ai:businessApplication:add', '新增业务应用聚合'
  UNION ALL SELECT '编辑业务应用', 53, 'ai:businessApplication:edit', '编辑业务应用和对象编排'
  UNION ALL SELECT '启停业务应用', 54, 'ai:businessApplication:status', '启停业务应用聚合'
  UNION ALL SELECT '删除业务应用', 55, 'ai:businessApplication:delete', '逻辑删除业务应用聚合'
) seed
WHERE @application_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM sys_resource r
    WHERE r.tenant_id = 1
      AND r.perms = seed.perms
      AND r.del_flag = 0
  );

-- 继承旧访问入口的等价权限，避免给普通角色扩大授权范围。
INSERT INTO sys_role_resource (tenant_id, role_id, resource_id, create_time)
SELECT DISTINCT 1, old_role_resource.role_id, new_resource.id, NOW()
FROM (
  SELECT 'ai:businessApp:list' old_perms, 'ai:businessApplication:list' new_perms
  UNION ALL SELECT 'ai:businessApp:add', 'ai:businessApplication:add'
  UNION ALL SELECT 'ai:businessApp:edit', 'ai:businessApplication:edit'
  UNION ALL SELECT 'ai:businessApp:status', 'ai:businessApplication:status'
  UNION ALL SELECT 'ai:businessApp:delete', 'ai:businessApplication:delete'
) permission_mapping
INNER JOIN sys_resource old_resource
  ON old_resource.tenant_id = 1
 AND old_resource.perms = permission_mapping.old_perms
 AND old_resource.del_flag = 0
INNER JOIN sys_role_resource old_role_resource
  ON old_role_resource.tenant_id = 1
 AND old_role_resource.resource_id = old_resource.id
INNER JOIN sys_resource new_resource
  ON new_resource.tenant_id = 1
 AND new_resource.perms = permission_mapping.new_perms
 AND new_resource.del_flag = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_resource existing
  WHERE existing.tenant_id = 1
    AND existing.role_id = old_role_resource.role_id
    AND existing.resource_id = new_resource.id
);

-- 为每个主候选对象建立一个确定性默认应用。对象编码只用于生成稳定编码，不改变原对象。
INSERT INTO ai_business_application (
  id, tenant_id, application_code, application_name, suite_code,
  icon, description, status, design_status, options, del_flag,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT UUID_SHORT(), bo.tenant_id,
       CONCAT('migrated_', LEFT(MD5(CONCAT(bo.tenant_id, ':', bo.suite_code, ':', bo.object_code)), 24)),
       bo.object_name, bo.suite_code,
       bo.icon, COALESCE(bo.description, CONCAT('由存量业务对象“', bo.object_name, '”整理生成')),
       bo.status, 'DRAFT',
       JSON_OBJECT('migrationSource', 'BUSINESS_OBJECT', 'primaryObjectId', CAST(bo.id AS CHAR)),
       '0', bo.create_by, bo.create_time, bo.create_dept, bo.update_by, bo.update_time
FROM ai_business_object bo
WHERE bo.del_flag = '0'
  AND (
    UPPER(bo.object_type) IN ('MASTER', 'TRANSACTION')
    OR NOT EXISTS (
      SELECT 1
      FROM ai_business_object_relation relation_row
      WHERE relation_row.tenant_id = bo.tenant_id
        AND relation_row.suite_code = bo.suite_code
        AND relation_row.target_object_code = bo.object_code
        AND relation_row.status = 1
        AND UPPER(relation_row.relation_type) IN ('DETAIL', 'CHILD_LIST')
    )
  )
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_application existing
    WHERE existing.tenant_id = bo.tenant_id
      AND existing.del_flag = '0'
      AND existing.application_code = CONCAT(
        'migrated_', LEFT(MD5(CONCAT(bo.tenant_id, ':', bo.suite_code, ':', bo.object_code)), 24)
      )
  );

-- 主对象关联。
INSERT INTO ai_business_application_object (
  id, tenant_id, application_id, object_id, object_role, sort_order,
  options, del_flag, create_by, create_time, create_dept, update_by, update_time
)
SELECT UUID_SHORT(), bo.tenant_id, application_row.id, bo.id, 'PRIMARY', 0,
       JSON_OBJECT('migrationSource', 'PRIMARY_OBJECT'), '0',
       bo.create_by, NOW(), bo.create_dept, bo.update_by, NOW()
FROM ai_business_object bo
INNER JOIN ai_business_application application_row
  ON application_row.tenant_id = bo.tenant_id
 AND application_row.application_code = CONCAT(
   'migrated_', LEFT(MD5(CONCAT(bo.tenant_id, ':', bo.suite_code, ':', bo.object_code)), 24)
 )
 AND application_row.del_flag = '0'
WHERE bo.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM ai_business_application_object existing
    WHERE existing.tenant_id = bo.tenant_id
      AND existing.application_id = application_row.id
      AND existing.object_id = bo.id
      AND existing.del_flag = '0'
  );

-- 直接明细、引用和多对多对象关联到来源主对象的应用。
INSERT INTO ai_business_application_object (
  id, tenant_id, application_id, object_id, object_role, sort_order,
  options, del_flag, create_by, create_time, create_dept, update_by, update_time
)
SELECT UUID_SHORT(), candidate.tenant_id, candidate.application_id, candidate.object_id,
       CASE MIN(candidate.role_priority)
         WHEN 1 THEN 'DETAIL'
         WHEN 2 THEN 'REFERENCE'
         ELSE 'SHARED'
       END,
       MIN(candidate.sort_order),
       JSON_OBJECT('migrationSource', 'OBJECT_RELATION', 'relationId', CAST(MIN(candidate.relation_id) AS CHAR)),
       '0', MIN(candidate.create_by), NOW(), MIN(candidate.create_dept), MAX(candidate.update_by), NOW()
FROM (
  SELECT relation_row.tenant_id,
         application_row.id application_id,
         target_object.id object_id,
         relation_row.id relation_id,
         relation_row.sort_order,
         relation_row.create_by,
         relation_row.create_dept,
         relation_row.update_by,
         CASE UPPER(relation_row.relation_type)
           WHEN 'DETAIL' THEN 1
           WHEN 'CHILD_LIST' THEN 1
           WHEN 'REFERENCE' THEN 2
           ELSE 3
         END role_priority
  FROM ai_business_object_relation relation_row
  INNER JOIN ai_business_object source_object
    ON source_object.tenant_id = relation_row.tenant_id
   AND source_object.suite_code = relation_row.suite_code
   AND source_object.object_code = relation_row.source_object_code
   AND source_object.del_flag = '0'
  INNER JOIN ai_business_object target_object
    ON target_object.tenant_id = relation_row.tenant_id
   AND target_object.suite_code = relation_row.suite_code
   AND target_object.object_code = relation_row.target_object_code
   AND target_object.del_flag = '0'
  INNER JOIN ai_business_application application_row
    ON application_row.tenant_id = source_object.tenant_id
   AND application_row.application_code = CONCAT(
     'migrated_', LEFT(MD5(CONCAT(source_object.tenant_id, ':', source_object.suite_code, ':', source_object.object_code)), 24)
   )
   AND application_row.del_flag = '0'
  WHERE relation_row.status = 1
    AND UPPER(relation_row.relation_type) IN ('DETAIL', 'CHILD_LIST', 'REFERENCE', 'MANY_TO_MANY')
) candidate
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_application_object existing
  WHERE existing.tenant_id = candidate.tenant_id
    AND existing.application_id = candidate.application_id
    AND existing.object_id = candidate.object_id
    AND existing.del_flag = '0'
)
GROUP BY candidate.tenant_id, candidate.application_id, candidate.object_id;

-- 业务对象仅归属一个应用时，将现有访问入口绑定到该应用；多义对象不做猜测。
UPDATE ai_business_app entry_row
INNER JOIN (
  SELECT object_row.tenant_id,
         object_row.suite_code,
         object_row.object_code,
         MIN(association.application_id) application_id
  FROM ai_business_application_object association
  INNER JOIN ai_business_object object_row
    ON object_row.tenant_id = association.tenant_id
   AND object_row.id = association.object_id
   AND object_row.del_flag = '0'
  WHERE association.del_flag = '0'
  GROUP BY object_row.tenant_id, object_row.suite_code, object_row.object_code
  HAVING COUNT(DISTINCT association.application_id) = 1
) resolved_application
  ON resolved_application.tenant_id = entry_row.tenant_id
 AND resolved_application.suite_code = entry_row.suite_code
 AND resolved_application.object_code = entry_row.object_code
SET entry_row.application_id = resolved_application.application_id
WHERE entry_row.del_flag = '0'
  AND entry_row.application_id IS NULL;

-- 为未归属或多义的存量入口建立每业务域唯一的历史入口应用。
INSERT INTO ai_business_application (
  id, tenant_id, application_code, application_name, suite_code,
  icon, description, status, design_status, options, del_flag,
  create_by, create_time, create_dept, update_by, update_time
)
SELECT UUID_SHORT(), pending.tenant_id,
       CONCAT('legacy_', LEFT(MD5(CONCAT(pending.tenant_id, ':', pending.suite_code)), 24)),
       CONCAT(COALESCE(suite_row.suite_name, pending.suite_code), '历史入口'),
       pending.suite_code, 'ionicons5:ArchiveOutline',
       '承接迁移时无法唯一归属的存量访问入口', 1, 'DRAFT',
       JSON_OBJECT('migrationSource', 'LEGACY_ENTRIES'), '0',
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT tenant_id, suite_code
  FROM ai_business_app
  WHERE del_flag = '0'
    AND application_id IS NULL
  GROUP BY tenant_id, suite_code
) pending
LEFT JOIN ai_business_suite suite_row
  ON suite_row.tenant_id = pending.tenant_id
 AND suite_row.suite_code = pending.suite_code
 AND suite_row.del_flag = '0'
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_application existing
  WHERE existing.tenant_id = pending.tenant_id
    AND existing.application_code = CONCAT(
      'legacy_', LEFT(MD5(CONCAT(pending.tenant_id, ':', pending.suite_code)), 24)
    )
    AND existing.del_flag = '0'
);

UPDATE ai_business_app entry_row
INNER JOIN ai_business_application application_row
  ON application_row.tenant_id = entry_row.tenant_id
 AND application_row.application_code = CONCAT(
   'legacy_', LEFT(MD5(CONCAT(entry_row.tenant_id, ':', entry_row.suite_code)), 24)
 )
 AND application_row.del_flag = '0'
SET entry_row.application_id = application_row.id
WHERE entry_row.del_flag = '0'
  AND entry_row.application_id IS NULL;

-- 具备唯一主对象和启用入口的迁移应用可进入待发布状态。
UPDATE ai_business_application application_row
SET application_row.design_status = 'READY',
    application_row.update_time = NOW()
WHERE application_row.del_flag = '0'
  AND application_row.design_status = 'DRAFT'
  AND JSON_UNQUOTE(JSON_EXTRACT(application_row.options, '$.migrationSource')) = 'BUSINESS_OBJECT'
  AND EXISTS (
    SELECT 1
    FROM ai_business_application_object association
    WHERE association.tenant_id = application_row.tenant_id
      AND association.application_id = application_row.id
      AND association.object_role = 'PRIMARY'
      AND association.del_flag = '0'
  )
  AND EXISTS (
    SELECT 1
    FROM ai_business_app entry_row
    WHERE entry_row.tenant_id = application_row.tenant_id
      AND entry_row.application_id = application_row.id
      AND entry_row.status = 1
      AND entry_row.del_flag = '0'
  );
