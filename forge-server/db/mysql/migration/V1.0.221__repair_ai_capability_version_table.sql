-- 修复本地/存量库缺失 ai_capability_version 导致授权选项与版本解析 500 的问题。
-- V1.0.21 已记录成功但表可能被误删；本脚本幂等重建并对齐当前结构后回填当前版本快照。

CREATE TABLE IF NOT EXISTS `ai_capability_version` (
  `id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL DEFAULT 1,
  `capability_id` bigint NOT NULL,
  `version` varchar(32) NOT NULL,
  `input_schema` json NOT NULL,
  `output_schema` json NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_key` varchar(128) NOT NULL,
  `source_version` varchar(64) NOT NULL,
  `behavior` varchar(24) NOT NULL,
  `risk_level` varchar(16) NOT NULL,
  `required_actor_type` varchar(16) NOT NULL DEFAULT 'SERVICE' COMMENT '要求的调用主体类型：SERVICE/USER/BOTH',
  `visibility` varchar(16) NOT NULL,
  `policy_snapshot` json DEFAULT NULL,
  `schema_checksum` char(64) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PUBLISHED',
  `del_flag` bigint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0正常，删除后写主键',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_version_active` (`tenant_id`, `capability_id`, `version`, `del_flag`),
  KEY `idx_ai_capability_version_checksum` (`tenant_id`, `capability_id`, `schema_checksum`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI中枢能力不可变版本';

-- 若旧表缺列则补齐（CREATE IF NOT EXISTS 不会改已有表）
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ai_capability_version'
    AND COLUMN_NAME = 'required_actor_type'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE `ai_capability_version` ADD COLUMN `required_actor_type` varchar(16) NOT NULL DEFAULT ''SERVICE'' COMMENT ''要求的调用主体类型：SERVICE/USER/BOTH'' AFTER `risk_level`',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为仍缺当前版本快照的能力回填占位版本（空契约，仅恢复授权选项可读）
INSERT INTO `ai_capability_version` (
  `id`, `tenant_id`, `capability_id`, `version`, `input_schema`, `output_schema`,
  `source_type`, `source_key`, `source_version`, `behavior`, `risk_level`, `required_actor_type`,
  `visibility`, `policy_snapshot`, `schema_checksum`, `status`, `del_flag`,
  `create_by`, `create_time`, `create_dept`, `update_by`, `update_time`
)
SELECT
  c.`id`,
  c.`tenant_id`,
  c.`id`,
  c.`current_version`,
  CAST('{}' AS JSON),
  CAST('{}' AS JSON),
  c.`source_type`,
  c.`source_key`,
  c.`source_version`,
  c.`behavior`,
  c.`risk_level`,
  COALESCE(NULLIF(c.`required_actor_type`, ''), 'SERVICE'),
  c.`visibility`,
  NULL,
  c.`schema_checksum`,
  'PUBLISHED',
  0,
  c.`create_by`,
  COALESCE(c.`create_time`, CURRENT_TIMESTAMP),
  c.`create_dept`,
  c.`update_by`,
  COALESCE(c.`update_time`, CURRENT_TIMESTAMP)
FROM `ai_capability` c
WHERE c.`del_flag` = 0
  AND c.`current_version` IS NOT NULL
  AND c.`current_version` <> ''
  AND NOT EXISTS (
    SELECT 1
    FROM `ai_capability_version` v
    WHERE v.`tenant_id` = c.`tenant_id`
      AND v.`capability_id` = c.`id`
      AND v.`version` = c.`current_version`
      AND v.`del_flag` = 0
  );
