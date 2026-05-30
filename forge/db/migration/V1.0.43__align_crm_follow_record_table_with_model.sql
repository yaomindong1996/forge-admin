-- Align crm_follow_record physical table columns with the CRM lowcode business model.
-- Canonical model fields:
-- subject, customer_id, opportunity_id, type, content, follow_time, assignee_id

SET @table_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'crm_follow_record'
);

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'crm_follow_record'
              AND COLUMN_NAME = 'subject'
        ) = 0,
        'ALTER TABLE crm_follow_record ADD COLUMN `subject` VARCHAR(256) NOT NULL DEFAULT '''' COMMENT ''主题'' AFTER `tenant_id`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'crm_follow_record'
              AND COLUMN_NAME = 'type'
        ) = 0,
        'ALTER TABLE crm_follow_record ADD COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT ''跟进方式'' AFTER `opportunity_id`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'crm_follow_record'
              AND COLUMN_NAME = 'content'
        ) = 0,
        'ALTER TABLE crm_follow_record ADD COLUMN `content` TEXT DEFAULT NULL COMMENT ''跟进内容'' AFTER `type`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        (
            SELECT COUNT(*)
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'crm_follow_record'
              AND COLUMN_NAME = 'assignee_id'
        ) = 0,
        'ALTER TABLE crm_follow_record ADD COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT ''负责人'' AFTER `follow_time`',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_follow_type = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'crm_follow_record'
      AND COLUMN_NAME = 'follow_type'
);

SET @has_follow_content = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'crm_follow_record'
      AND COLUMN_NAME = 'follow_content'
);

SET @has_follow_user_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'crm_follow_record'
      AND COLUMN_NAME = 'follow_user_id'
);

SET @sql = IF(
    @table_exists = 1 AND @has_follow_type > 0,
    'UPDATE crm_follow_record SET `type` = COALESCE(`type`, follow_type) WHERE `type` IS NULL AND follow_type IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1 AND @has_follow_content > 0,
    'UPDATE crm_follow_record SET `content` = COALESCE(`content`, follow_content) WHERE `content` IS NULL AND follow_content IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1 AND @has_follow_user_id > 0,
    'UPDATE crm_follow_record SET assignee_id = COALESCE(assignee_id, follow_user_id) WHERE assignee_id IS NULL AND follow_user_id IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    IF(
        @has_follow_content > 0,
        'UPDATE crm_follow_record SET subject = LEFT(COALESCE(NULLIF(subject, ''''), NULLIF(content, ''''), NULLIF(follow_content, ''''), ''跟进记录''), 256) WHERE subject IS NULL OR subject = ''''',
        'UPDATE crm_follow_record SET subject = LEFT(COALESCE(NULLIF(subject, ''''), NULLIF(content, ''''), ''跟进记录''), 256) WHERE subject IS NULL OR subject = '''''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `subject` VARCHAR(256) NOT NULL DEFAULT '''' COMMENT ''主题'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `customer_id` BIGINT DEFAULT NULL COMMENT ''关联客户'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `opportunity_id` BIGINT DEFAULT NULL COMMENT ''关联商机'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `type` VARCHAR(32) DEFAULT NULL COMMENT ''跟进方式'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `content` TEXT DEFAULT NULL COMMENT ''跟进内容'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `follow_time` DATETIME DEFAULT NULL COMMENT ''跟进时间'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `assignee_id` BIGINT DEFAULT NULL COMMENT ''负责人'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 0,
    'SELECT 1',
    'ALTER TABLE crm_follow_record MODIFY COLUMN `del_flag` CHAR(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND INDEX_NAME = 'idx_crm_follow_record_contact'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP INDEX idx_crm_follow_record_contact',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND INDEX_NAME = 'idx_crm_follow_record_user'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP INDEX idx_crm_follow_record_user',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND INDEX_NAME = 'idx_crm_follow_record_assignee'
    ) = 0,
    'ALTER TABLE crm_follow_record ADD INDEX idx_crm_follow_record_assignee (`tenant_id`, `assignee_id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'contact_id'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `contact_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1 AND @has_follow_type > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `follow_type`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1 AND @has_follow_content > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `follow_content`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'next_follow_time'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `next_follow_time`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'next_follow_content'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `next_follow_content`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1 AND @has_follow_user_id > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `follow_user_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'follow_user_name'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `follow_user_name`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'status'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `status`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    @table_exists = 1
    AND (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'crm_follow_record'
          AND COLUMN_NAME = 'options'
    ) > 0,
    'ALTER TABLE crm_follow_record DROP COLUMN `options`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
