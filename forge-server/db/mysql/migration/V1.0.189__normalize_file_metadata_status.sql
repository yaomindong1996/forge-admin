-- sys_file_metadata.status 是历史表的逻辑删除标记：1=正常，0=已删除。
-- 将空值/异常值收敛为已删除（fail-closed），并固定非空默认值，确保实体、Mapper 与数据库语义一致。

SET @file_metadata_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_file_metadata'
);

SET @normalize_file_metadata_status_sql = IF(
    @file_metadata_table_exists > 0,
    'UPDATE sys_file_metadata SET status = 0 WHERE status IS NULL OR status NOT IN (0, 1)',
    'SELECT 1'
);
PREPARE normalize_file_metadata_status_stmt FROM @normalize_file_metadata_status_sql;
EXECUTE normalize_file_metadata_status_stmt;
DEALLOCATE PREPARE normalize_file_metadata_status_stmt;

SET @modify_file_metadata_status_sql = IF(
    @file_metadata_table_exists > 0,
    'ALTER TABLE sys_file_metadata MODIFY COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''逻辑删除状态(1-正常,0-已删除)''',
    'SELECT 1'
);
PREPARE modify_file_metadata_status_stmt FROM @modify_file_metadata_status_sql;
EXECUTE modify_file_metadata_status_stmt;
DEALLOCATE PREPARE modify_file_metadata_status_stmt;
