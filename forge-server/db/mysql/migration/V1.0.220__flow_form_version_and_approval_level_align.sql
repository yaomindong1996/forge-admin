-- 补齐流程表单发布版本表（入口/发布依赖）；对齐审批层级实体字段，避免 insert 因缺列失败。

CREATE TABLE IF NOT EXISTS `sys_flow_form_version` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `form_id` bigint NOT NULL COMMENT '流程表单ID',
  `form_key` varchar(128) NOT NULL COMMENT '表单Key',
  `form_name` varchar(128) NOT NULL COMMENT '表单名称',
  `form_category` varchar(64) DEFAULT NULL COMMENT '表单分类',
  `form_type` varchar(32) NOT NULL DEFAULT 'dynamic' COMMENT '表单类型',
  `version` int NOT NULL COMMENT '版本号',
  `form_schema` longtext COMMENT '表单Schema快照',
  `field_registry` json DEFAULT NULL COMMENT '字段目录快照',
  `form_config` json DEFAULT NULL COMMENT '表单配置快照',
  `default_data_mode` varchar(32) NOT NULL DEFAULT 'PROCESS_ONLY' COMMENT '默认数据模式',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `publish_by` bigint DEFAULT NULL COMMENT '发布人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_form_version` (`tenant_id`, `form_id`, `version`),
  KEY `idx_flow_form_version_key` (`tenant_id`, `form_key`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流程表单发布版本表';

SET @table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level'
);

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'required'
);
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
  'ALTER TABLE sys_flow_approval_level ADD COLUMN `required` tinyint DEFAULT 1 COMMENT ''是否必须审批'' AFTER `skip_condition`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'timeout_hours'
);
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
  'ALTER TABLE sys_flow_approval_level ADD COLUMN `timeout_hours` int DEFAULT NULL COMMENT ''超时时间（小时）'' AFTER `required`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'status'
);
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
  'ALTER TABLE sys_flow_approval_level ADD COLUMN `status` tinyint DEFAULT 1 COMMENT ''状态：0禁用 1启用'' AFTER `sort_order`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'update_time'
);
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
  'ALTER TABLE sys_flow_approval_level ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `create_time`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_flow_approval_level' AND COLUMN_NAME = 'create_by'
);
SET @sql = IF(@table_exists > 0 AND @col_exists = 0,
  'ALTER TABLE sys_flow_approval_level ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `update_time`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
