-- 业务对象设计器基础数据结构。

SET @bo_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
);

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND COLUMN_NAME = 'design_status'
        ) = 0,
        'ALTER TABLE ai_business_object ADD COLUMN design_status varchar(32) NOT NULL DEFAULT ''DRAFT'' COMMENT ''设计状态：DRAFT草稿 DESIGNING设计中 READY待发布 PUBLISHED已发布 CHANGED有未发布变更'' AFTER options',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND COLUMN_NAME = 'config_key'
        ) = 0,
        'ALTER TABLE ai_business_object ADD COLUMN config_key varchar(128) DEFAULT NULL COMMENT ''关联低代码运行配置键'' AFTER design_status',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND COLUMN_NAME = 'last_publish_time'
        ) = 0,
        'ALTER TABLE ai_business_object ADD COLUMN last_publish_time datetime DEFAULT NULL COMMENT ''最近发布时间'' AFTER config_key',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND COLUMN_NAME = 'last_publish_version'
        ) = 0,
        'ALTER TABLE ai_business_object ADD COLUMN last_publish_version int DEFAULT NULL COMMENT ''最近发布版本号'' AFTER last_publish_time',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND COLUMN_NAME = 'designer_options'
        ) = 0,
        'ALTER TABLE ai_business_object ADD COLUMN designer_options json DEFAULT NULL COMMENT ''业务对象设计器配置'' AFTER last_publish_version',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @bo_table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'ai_business_object'
              AND INDEX_NAME = 'idx_ai_business_object_config_key'
        ) = 0,
        'ALTER TABLE ai_business_object ADD INDEX idx_ai_business_object_config_key (tenant_id, config_key)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `ai_business_object_design_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `object_id` bigint NOT NULL COMMENT '业务对象ID',
  `suite_code` varchar(48) NOT NULL COMMENT '业务套件编码',
  `object_code` varchar(48) NOT NULL COMMENT '业务对象编码',
  `config_id` bigint DEFAULT NULL COMMENT '关联低代码运行配置ID',
  `config_key` varchar(128) DEFAULT NULL COMMENT '关联低代码运行配置键',
  `crud_config_version_id` bigint DEFAULT NULL COMMENT '关联AI CRUD发布版本ID',
  `version_no` int NOT NULL COMMENT '对象设计版本号',
  `version_type` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '版本类型：draft/publish/rollback',
  `model_snapshot` json DEFAULT NULL COMMENT '模型Schema快照',
  `page_snapshot` json DEFAULT NULL COMMENT '页面Schema快照',
  `relation_snapshot` json DEFAULT NULL COMMENT '关系配置快照',
  `publish_status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED/FAILED',
  `publish_version` int DEFAULT NULL COMMENT '发布版本号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_design_version_no` (`tenant_id`, `object_id`, `version_no`),
  KEY `idx_ai_business_design_version_object` (`tenant_id`, `object_id`, `version_no`),
  KEY `idx_ai_business_design_version_crud` (`tenant_id`, `crud_config_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-业务对象设计版本表';

CREATE TABLE IF NOT EXISTS `ai_business_field_template` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `template_code` varchar(64) NOT NULL COMMENT '字段模板编码',
  `template_name` varchar(128) NOT NULL COMMENT '字段模板名称',
  `suite_code` varchar(48) NOT NULL DEFAULT 'COMMON' COMMENT '适用业务套件编码，COMMON为通用',
  `field_type` varchar(32) NOT NULL COMMENT '业务字段类型',
  `field_schema` json NOT NULL COMMENT '字段Schema',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `description` varchar(500) DEFAULT NULL COMMENT '模板说明',
  `options` json DEFAULT NULL COMMENT '扩展配置',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_business_field_template_code` (`tenant_id`, `template_code`),
  KEY `idx_ai_business_field_template_code` (`tenant_id`, `template_code`),
  KEY `idx_ai_business_field_template_suite` (`tenant_id`, `suite_code`, `status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI业务应用平台-业务字段模板表';

INSERT INTO ai_business_field_template (id, tenant_id, template_code, template_name, suite_code, field_type,
                                        field_schema, status, sort_order, description, options, create_by,
                                        create_time, create_dept, update_by, update_time)
SELECT seed.id, 1, seed.template_code, seed.template_name, 'COMMON', seed.field_type, seed.field_schema,
       1, seed.sort_order, seed.description, '{"builtin":true,"stage":"business-object-designer"}',
       1, NOW(), 1, 1, NOW()
FROM (
  SELECT 1910000000000000501 id, 'common_customer_name' template_code, '客户名称' template_name, 'TEXT' field_type,
         '{"field":"customerName","columnName":"customer_name","label":"客户名称","dataType":"varchar","length":128,"precision":2,"required":true,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","dictType":"","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":180,"remark":"客户名称","businessFieldType":"TEXT","fieldStatus":"ENABLED","sortOrder":1,"importable":true,"exportable":true}' field_schema,
         1 sort_order, '常用客户主字段，可作为对象显示字段' description
  UNION ALL SELECT 1910000000000000502, 'common_contact_phone', '联系电话', 'PHONE',
         '{"field":"contactPhone","columnName":"contact_phone","label":"联系电话","dataType":"varchar","length":32,"precision":2,"required":false,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"input","queryType":"like","dictType":"","sensitiveType":"PHONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":140,"remark":"联系电话","businessFieldType":"PHONE","fieldStatus":"ENABLED","sortOrder":2,"importable":true,"exportable":true}',
         2, '常用联系电话字段，默认启用手机号脱敏'
  UNION ALL SELECT 1910000000000000503, 'common_customer_level', '客户等级', 'DICT',
         '{"field":"customerLevel","columnName":"customer_level","label":"客户等级","dataType":"varchar","length":32,"precision":2,"required":false,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"select","queryType":"eq","dictType":"crm_customer_level","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":140,"remark":"客户等级","businessFieldType":"DICT","fieldStatus":"ENABLED","sortOrder":3,"importable":true,"exportable":true}',
         3, '客户分层字段，默认使用CRM客户等级字典'
  UNION ALL SELECT 1910000000000000504, 'common_owner_user', '负责人', 'USER',
         '{"field":"ownerUserId","columnName":"owner_user_id","label":"负责人","dataType":"bigint","length":null,"precision":null,"required":false,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"userSelect","queryType":"eq","dictType":"","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":140,"remark":"业务负责人","businessFieldType":"USER","fieldStatus":"ENABLED","sortOrder":4,"importable":true,"exportable":true}',
         4, '对象负责人字段，用于权限和数据归属'
  UNION ALL SELECT 1910000000000000505, 'common_owner_dept', '所属部门', 'DEPT',
         '{"field":"ownerDeptId","columnName":"owner_dept_id","label":"所属部门","dataType":"bigint","length":null,"precision":null,"required":false,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"orgTreeSelect","queryType":"eq","dictType":"","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":140,"remark":"所属部门","businessFieldType":"DEPT","fieldStatus":"ENABLED","sortOrder":5,"importable":true,"exportable":true}',
         5, '对象所属部门字段，用于部门维度筛选'
  UNION ALL SELECT 1910000000000000506, 'common_region_code', '所属地区', 'REGION',
         '{"field":"regionCode","columnName":"region_code","label":"所属地区","dataType":"varchar","length":32,"precision":2,"required":false,"defaultValue":null,"searchable":true,"listVisible":true,"formVisible":true,"componentType":"regionTreeSelect","queryType":"eq","dictType":"","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":160,"remark":"所属地区","businessFieldType":"REGION","fieldStatus":"ENABLED","sortOrder":6,"importable":true,"exportable":true}',
         6, '行政区划字段，支持区域树选择'
  UNION ALL SELECT 1910000000000000507, 'common_remark', '备注', 'MULTILINE',
         '{"field":"remark","columnName":"remark","label":"备注","dataType":"text","length":null,"precision":2,"required":false,"defaultValue":null,"searchable":false,"listVisible":true,"formVisible":true,"componentType":"textarea","queryType":"like","dictType":"","sensitiveType":"NONE","encryptAlgorithm":"","sortable":false,"primaryKey":false,"systemField":false,"readonly":false,"autoIncrement":false,"width":220,"remark":"备注","businessFieldType":"MULTILINE","fieldStatus":"ENABLED","sortOrder":7,"importable":true,"exportable":true}',
         7, '通用备注说明字段'
) seed
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_business_field_template t
  WHERE t.tenant_id = 1
    AND t.template_code = seed.template_code
);
