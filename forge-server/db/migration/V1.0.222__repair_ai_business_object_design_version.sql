-- 修复存量库缺失 ai_business_object_design_version 导致
-- GET /ai/capability/system-service/registration-source 列举低代码表单来源时 SQL 500。

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
  `designer_options_snapshot` json DEFAULT NULL COMMENT '表单优先设计器扩展快照',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI业务应用平台-业务对象设计版本表';

-- 旧表缺列时补齐（CREATE IF NOT EXISTS 不会改已有表）
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_business_object_design_version'
    AND COLUMN_NAME = 'designer_options_snapshot'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE `ai_business_object_design_version` ADD COLUMN `designer_options_snapshot` json DEFAULT NULL COMMENT ''表单优先设计器扩展快照'' AFTER `relation_snapshot`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
