-- 数据集访问权限升级脚本
-- 适用于已初始化数据资产表的环境；新库请直接执行 data_tables.sql。

SET @column_exists := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_report_data_dataset'
    AND column_name = 'access_mode'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE ai_report_data_dataset ADD COLUMN access_mode VARCHAR(20) NOT NULL DEFAULT ''PUBLIC'' COMMENT ''访问模式：PUBLIC公开 PRIVATE私有'' AFTER publish_status',
  'SELECT ''ai_report_data_dataset.access_mode exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'ai_report_data_dataset'
    AND index_name = 'idx_data_dataset_access_mode'
);
SET @ddl := IF(@index_exists = 0,
  'ALTER TABLE ai_report_data_dataset ADD INDEX idx_data_dataset_access_mode (tenant_id, access_mode)',
  'SELECT ''idx_data_dataset_access_mode exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `ai_report_data_dataset_acl` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户ID',
  `dataset_id` bigint NOT NULL COMMENT '数据集ID',
  `subject_type` varchar(20) NOT NULL COMMENT '授权主体类型：USER/ROLE/ORG',
  `subject_id` bigint NOT NULL COMMENT '授权主体ID',
  `access_level` varchar(20) NOT NULL DEFAULT 'QUERY' COMMENT '访问级别：VIEW/QUERY/MANAGE',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_dataset_acl_subject` (`tenant_id`, `dataset_id`, `subject_type`, `subject_id`, `access_level`),
  KEY `idx_data_dataset_acl_dataset` (`tenant_id`, `dataset_id`),
  KEY `idx_data_dataset_acl_subject` (`tenant_id`, `subject_type`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台数据集授权';

UPDATE ai_report_data_dataset
SET access_mode = 'PUBLIC'
WHERE access_mode IS NULL OR access_mode = '';
