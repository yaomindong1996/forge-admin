-- 将可见生成列 logic_delete_active 替换为普通 del_flag 墓碑唯一标记。
-- 0 表示有效；删除后写当前行主键。生成列存在时才执行，已完成迁移的表安全跳过。
-- 每表先扩展 del_flag 类型并回填墓碑，再按 information_schema 中实际存在的索引原子替换唯一索引和生成列。

-- ai_agent
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_agent'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_agent_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_agent` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_agent` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_agent` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_agent_code_active` (`agent_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_app
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_app'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_app'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_app_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_app` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_app` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_app` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_app_code_active` (`tenant_id`, `app_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_application
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_application_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_application` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_application` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_application` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_application_code_active` (`tenant_id`, `application_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_application_object
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_object'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_object'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_application_object_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_application_object` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_application_object` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_application_object` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_application_object_active` (`tenant_id`, `application_id`, `object_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_application_publish_run
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_publish_run'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_publish_run'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_publish_run_key_active', 'uk_ai_business_publish_run_version_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_application_publish_run` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_application_publish_run` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_application_publish_run` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_publish_run_key_active` (`tenant_id`, `application_id`, `idempotency_key`, `del_flag`), ADD UNIQUE INDEX `uk_ai_business_publish_run_version_active` (`tenant_id`, `application_id`, `target_version_no`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_application_version
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_version'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_application_version'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_application_version_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_application_version` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_application_version` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_application_version` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_application_version_active` (`tenant_id`, `application_id`, `version_no`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_extension
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_extension'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_extension'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_extension_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_extension` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_extension` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_extension` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_extension_code_active` (`tenant_id`, `application_id`, `extension_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_extension_version
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_extension_version'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_extension_version'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_extension_version_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_extension_version` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_extension_version` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_extension_version` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_extension_version_active` (`tenant_id`, `extension_id`, `version_no`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_object
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_object'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_object_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_object` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_object` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_object` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_object_code_active` (`tenant_id`, `suite_code`, `object_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_business_suite
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_suite'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_business_suite'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_business_suite_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_business_suite` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_business_suite` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_business_suite` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_business_suite_code_active` (`tenant_id`, `suite_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_code_active', 'uk_ai_capability_tool_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_code_active` (`tenant_id`, `capability_code`, `del_flag`), ADD UNIQUE INDEX `uk_ai_capability_tool_active` (`tenant_id`, `protocol_tool_name`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_access_token
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_access_token'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_access_token'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_token_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_access_token` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_access_token` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_access_token` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_token_key_active` (`token_key_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_approval
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_approval'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_approval'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_capability_approval_idempotency', 'uk_capability_approval_request')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_approval` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_approval` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_approval` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_capability_approval_idempotency` (`tenant_id`, `client_id`, `capability_id`, `idempotency_key`, `del_flag`), ADD UNIQUE INDEX `uk_capability_approval_request` (`tenant_id`, `request_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_client
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_client'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_client'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_client_code_active', 'uk_ai_capability_client_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_client` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_client` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_client` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_client_code_active` (`tenant_id`, `client_code`, `del_flag`), ADD UNIQUE INDEX `uk_ai_capability_client_key_active` (`key_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_flow_action_log
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_flow_action_log'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_flow_action_log'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_cap_flow_action_idempotency', 'uk_cap_flow_action_request')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_flow_action_log` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_flow_action_log` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_flow_action_log` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_cap_flow_action_idempotency` (`tenant_id`, `client_id`, `capability_id`, `operation`, `idempotency_key`, `del_flag`), ADD UNIQUE INDEX `uk_cap_flow_action_request` (`tenant_id`, `request_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_grant
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_grant'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_grant'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_grant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_grant` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_grant` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_grant` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_grant_active` (`tenant_id`, `client_id`, `capability_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_oauth_redirect_uri
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_oauth_redirect_uri'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_oauth_redirect_uri'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_redirect_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_oauth_redirect_uri` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_oauth_redirect_uri` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_oauth_redirect_uri` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_redirect_active` (`tenant_id`, `client_id`, `redirect_uri_hash`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_policy
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_policy'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_policy'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_capability_policy_version')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_policy` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_policy` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_policy` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_capability_policy_version` (`tenant_id`, `capability_id`, `capability_version`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_capability_version
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_version'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_capability_version'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_capability_version_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_capability_version` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_capability_version` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_capability_version` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_capability_version_active` (`tenant_id`, `capability_id`, `version`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_code_rule
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_code_rule'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_code_rule'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_code_rule_code_active', 'uk_ai_code_rule_code')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_code_rule` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_code_rule` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_code_rule` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_code_rule_code_active` (`tenant_id`, `rule_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_code_rule_segment
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_code_rule_segment'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_code_rule_segment'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_code_rule_segment_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_code_rule_segment` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_code_rule_segment` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_code_rule_segment` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_code_rule_segment_key_active` (`tenant_id`, `rule_id`, `segment_key`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_crud_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_crud_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_crud_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_config_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_crud_config` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_crud_config` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_crud_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_config_key_active` (`config_key`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_lowcode_domain
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_lowcode_domain'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_lowcode_domain'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_lowcode_domain_code_active', 'uk_ai_lowcode_domain_name_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_lowcode_domain` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_lowcode_domain` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_lowcode_domain` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_lowcode_domain_code_active` (`tenant_id`, `domain_code`, `del_flag`), ADD UNIQUE INDEX `uk_ai_lowcode_domain_name_active` (`tenant_id`, `parent_id`, `domain_name`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_lowcode_model
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_lowcode_model'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_lowcode_model'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_lowcode_model_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_lowcode_model` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_lowcode_model` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_lowcode_model` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_lowcode_model_code_active` (`tenant_id`, `domain_id`, `model_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_model
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_model_provider_model_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_model` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_model` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_model` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_model_provider_model_active` (`provider_id`, `model_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_model_capability
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_capability'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_capability'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_model_capability_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_model_capability` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_model_capability` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_model_capability` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_model_capability_active` (`tenant_id`, `model_id`, `capability_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_model_route_policy
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_route_policy'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_route_policy'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_route_policy_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_model_route_policy` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_model_route_policy` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_model_route_policy` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_route_policy_code_active` (`tenant_id`, `policy_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_model_route_target
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_route_target'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_model_route_target'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_route_target_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_model_route_target` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_model_route_target` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_model_route_target` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_route_target_active` (`tenant_id`, `policy_id`, `model_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_page_template
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_page_template'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_page_template'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_template_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_page_template` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_page_template` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_page_template` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_template_key_active` (`template_key`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_prompt_template
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_prompt_template'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_prompt_template'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_ai_prompt_template_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_prompt_template` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_prompt_template` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_prompt_template` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_ai_prompt_template_code_active` (`tenant_id`, `template_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_report_data_business_definition
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_business_definition'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_business_definition'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_data_business_code_tenant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_report_data_business_definition` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_report_data_business_definition` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_report_data_business_definition` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_data_business_code_tenant_active` (`tenant_id`, `business_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_report_data_connection
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_connection'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_connection'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_data_connection_code_tenant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_report_data_connection` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_report_data_connection` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_report_data_connection` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_data_connection_code_tenant_active` (`tenant_id`, `connection_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_report_data_dataset
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dataset'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dataset'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_data_dataset_code_tenant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_report_data_dataset` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_report_data_dataset` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_report_data_dataset` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_data_dataset_code_tenant_active` (`tenant_id`, `dataset_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_report_data_dataset_category
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dataset_category'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dataset_category'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_data_dataset_category_code_tenant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_report_data_dataset_category` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_report_data_dataset_category` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_report_data_dataset_category` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_data_dataset_category_code_tenant_active` (`tenant_id`, `category_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ai_report_data_dimension
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dimension'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ai_report_data_dimension'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_data_dimension_code_tenant_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `ai_report_data_dimension` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `ai_report_data_dimension` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `ai_report_data_dimension` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_data_dimension_code_tenant_active` (`tenant_id`, `dimension_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sample_purchase_order
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sample_purchase_order'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sample_purchase_order'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_sample_purchase_order_business_key_active', 'uk_sample_purchase_order_no_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sample_purchase_order` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sample_purchase_order` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sample_purchase_order` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_sample_purchase_order_business_key_active` (`tenant_id`, `business_key`, `del_flag`), ADD UNIQUE INDEX `uk_sample_purchase_order_no_active` (`tenant_id`, `order_no`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_api_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_api_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_api_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_method_url_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_api_config` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_api_config` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_api_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_method_url_active` (`url_path`, `req_method`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_config_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_config` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_config` SET `del_flag` = `config_id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_config_key_active` (`tenant_id`, `config_key`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_data_scope_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_data_scope_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_data_scope_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_mapper_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_data_scope_config` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_data_scope_config` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_data_scope_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_mapper_active` (`tenant_id`, `mapper_method`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_data
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dict_data'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dict_data'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_dict_data_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_dict_data` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_dict_data` SET `del_flag` = `dict_code` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_dict_data` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_dict_data_active` (`tenant_id`, `dict_type`, `dict_value`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_dict_type
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dict_type'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_dict_type'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_dict_type_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_dict_type` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_dict_type` SET `del_flag` = `dict_id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_dict_type` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_dict_type_active` (`tenant_id`, `dict_type`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_employee
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_employee'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_employee'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_emp_no_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_employee` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_employee` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_employee` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_emp_no_active` (`emp_no`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_flow_node_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_node_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_flow_node_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_model_node_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_flow_node_config` MODIFY COLUMN `del_flag` varchar(64) NOT NULL DEFAULT ''0'' COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_flow_node_config` SET `del_flag` = `id` WHERE `del_flag` <> ''0''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_flow_node_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_model_node_active` (`model_id`, `node_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job_api_idempotency
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_api_idempotency'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_api_idempotency'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_job_api_idempotency_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_job_api_idempotency` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_job_api_idempotency` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_job_api_idempotency` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_job_api_idempotency_active` (`tenant_id`, `token_id`, `job_config_id`, `idempotency_key_hash`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job_api_token
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_api_token'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_api_token'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_job_api_token_key_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_job_api_token` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_job_api_token` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_job_api_token` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_job_api_token_key_active` (`token_key_id`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_job_config
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_config'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_job_config'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_job_name_group_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_job_config` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_job_config` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_job_config` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_job_name_group_active` (`job_name`, `job_group`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_message_biz_type
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_message_biz_type'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_message_biz_type'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_type_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_message_biz_type` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_message_biz_type` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_message_biz_type` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_type_active` (`tenant_id`, `biz_type`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_message_template
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_message_template'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_message_template'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_message_template` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_message_template` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_message_template` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_code_active` (`tenant_id`, `template_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_org
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_org'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_org'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_org_name_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_org` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_org` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_org` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_org_name_active` (`tenant_id`, `org_name`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_outbound_whitelist
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_outbound_whitelist'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_outbound_whitelist'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_outbound_whitelist_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_outbound_whitelist` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_outbound_whitelist` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_outbound_whitelist` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_outbound_whitelist_active` (`tenant_id`, `scene`, `protocol`, `host`, `port_start`, `port_end`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_post
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_post'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_post'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_org_post_active', 'uk_tenant_post_code_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_post` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_post` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_post` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_org_post_active` (`tenant_id`, `org_id`, `post_name`, `del_flag`), ADD UNIQUE INDEX `uk_tenant_post_code_active` (`tenant_id`, `post_code`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_resource
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_resource'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_resource_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_resource` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_resource` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_resource` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_resource_active` (`tenant_id`, `resource_type`, `perms`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_role
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_role'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_role'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_role_key_active', 'uk_tenant_role_name_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_role` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_role` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_role` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_role_key_active` (`tenant_id`, `role_key`, `del_flag`), ADD UNIQUE INDEX `uk_tenant_role_name_active` (`tenant_id`, `role_name`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_tenant
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_tenant'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_tenant'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('uk_tenant_name_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_tenant` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_tenant` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_tenant` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `uk_tenant_name_active` (`tenant_name`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- sys_user
SET @logic_delete_active_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'logic_delete_active'
);
SET @drop_index_clauses = (
    SELECT GROUP_CONCAT(
        DISTINCT CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`')
        ORDER BY INDEX_NAME
        SEPARATOR ', '
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND (
          COLUMN_NAME = 'logic_delete_active'
          OR INDEX_NAME IN ('sys_user_unique_active')
      )
);
SET @sql = IF(@logic_delete_active_exists > 0,
    'ALTER TABLE `sys_user` MODIFY COLUMN `del_flag` bigint NOT NULL DEFAULT 0 COMMENT ''逻辑删除标记：0正常，删除后写主键''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    'UPDATE `sys_user` SET `del_flag` = `id` WHERE `del_flag` <> 0',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@logic_delete_active_exists > 0,
    CONCAT(
        'ALTER TABLE `sys_user` ',
        IF(COALESCE(@drop_index_clauses, '') = '',
            '',
            CONCAT(@drop_index_clauses, ', ')),
        'DROP COLUMN `logic_delete_active`, ADD UNIQUE INDEX `sys_user_unique_active` (`tenant_id`, `username`, `del_flag`)'
    ),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

