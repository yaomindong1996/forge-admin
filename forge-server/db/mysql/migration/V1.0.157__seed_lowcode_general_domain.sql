-- 补种低代码“通用业务域”(domain_code=general)。
-- 发布未绑定领域的应用时会回落到该域；原 V1.0.7 建表+种子脚本已迁到 db/backup，
-- 当前 Flyway 链路不再执行，导致发布报“通用业务域不存在”。

CREATE TABLE IF NOT EXISTS `ai_lowcode_domain` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级领域ID，0表示根节点',
  `domain_code` varchar(48) NOT NULL COMMENT '领域编码',
  `domain_name` varchar(128) NOT NULL COMMENT '领域名称',
  `domain_desc` varchar(500) DEFAULT NULL COMMENT '领域说明',
  `icon` varchar(128) DEFAULT NULL COMMENT '领域图标',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 DISABLED停用',
  `menu_parent_id` bigint DEFAULT NULL COMMENT '默认菜单父级ID',
  `table_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认表名前缀',
  `config_key_prefix` varchar(64) NOT NULL DEFAULT 'biz_' COMMENT '默认配置键前缀',
  `default_app_type` varchar(32) NOT NULL DEFAULT 'SINGLE' COMMENT '默认应用类型',
  `default_layout_type` varchar(64) NOT NULL DEFAULT 'simple-crud' COMMENT '默认页面模板',
  `default_table_mode` varchar(32) NOT NULL DEFAULT 'CREATE' COMMENT '默认建表模式',
  `domain_schema` json DEFAULT NULL COMMENT '领域扩展协议',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_lowcode_domain_code_active` (`tenant_id`, `domain_code`, `del_flag`),
  UNIQUE KEY `uk_ai_lowcode_domain_name_active` (`tenant_id`, `parent_id`, `domain_name`, `del_flag`),
  KEY `idx_ai_lowcode_domain_parent` (`tenant_id`, `parent_id`, `sort`),
  KEY `idx_ai_lowcode_domain_status` (`tenant_id`, `status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI低代码业务领域表';

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_lowcode_domain'
    AND column_name = 'del_flag'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE `ai_lowcode_domain` ADD COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO ai_lowcode_domain (
  id, tenant_id, parent_id, domain_code, domain_name, domain_desc, icon, sort, status,
  menu_parent_id, table_prefix, config_key_prefix, default_app_type,
  default_layout_type, default_table_mode, domain_schema,
  create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1900000000000000001, 1, 0, 'general', '通用业务域',
       '历史低代码应用默认归属的通用业务领域', 'ionicons5:AppsOutline', 0, 'ENABLED',
       NULL, 'biz_general_', 'general_', 'SINGLE', 'simple-crud', 'CREATE',
       JSON_OBJECT(
         'aiContext', JSON_OBJECT(
           'description', '通用业务域，用于承接历史低代码应用和未明确归属的业务对象',
           'terms', JSON_ARRAY(),
           'commonObjects', JSON_ARRAY(),
           'fieldNamingPreference', 'lowerCamel'
         ),
         'naming', JSON_OBJECT(
           'tablePrefix', 'biz_general_',
           'configKeyPrefix', 'general_',
           'objectCodeStyle', 'lower_snake'
         ),
         'defaults', JSON_OBJECT(
           'appType', 'SINGLE',
           'layoutType', 'simple-crud',
           'tableMode', 'CREATE'
         )
       ),
       1, NOW(), 1, NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM ai_lowcode_domain existing
  WHERE existing.tenant_id = 1
    AND existing.domain_code = 'general'
    AND existing.del_flag = 0
);

INSERT INTO ai_lowcode_domain (
  id, tenant_id, parent_id, domain_code, domain_name, domain_desc, icon, sort, status,
  menu_parent_id, table_prefix, config_key_prefix, default_app_type,
  default_layout_type, default_table_mode, domain_schema,
  create_by, create_time, update_by, update_time, create_dept, del_flag
)
SELECT 1900000000000000000 + t.id, t.id, 0, 'general', '通用业务域',
       '历史低代码应用默认归属的通用业务领域', 'ionicons5:AppsOutline', 0, 'ENABLED',
       NULL, 'biz_general_', 'general_', 'SINGLE', 'simple-crud', 'CREATE',
       JSON_OBJECT(
         'aiContext', JSON_OBJECT(
           'description', '通用业务域，用于承接历史低代码应用和未明确归属的业务对象'
         ),
         'naming', JSON_OBJECT(
           'tablePrefix', 'biz_general_',
           'configKeyPrefix', 'general_'
         )
       ),
       1, NOW(), 1, NOW(), 1, 0
FROM sys_tenant t
WHERE t.id <> 1
  AND t.del_flag = 0
  AND NOT EXISTS (
    SELECT 1
    FROM ai_lowcode_domain existing
    WHERE existing.tenant_id = t.id
      AND existing.domain_code = 'general'
      AND existing.del_flag = 0
  )
  AND NOT EXISTS (
    SELECT 1
    FROM ai_lowcode_domain taken
    WHERE taken.id = 1900000000000000000 + t.id
  );

UPDATE ai_crud_config c
INNER JOIN ai_lowcode_domain d
  ON d.tenant_id = c.tenant_id
 AND d.domain_code = 'general'
 AND d.del_flag = 0
SET c.domain_id = d.id,
    c.domain_code = 'general'
WHERE c.domain_id IS NULL
  AND c.mode = 'CONFIG'
  AND c.build_mode = 'LOWCODE';
