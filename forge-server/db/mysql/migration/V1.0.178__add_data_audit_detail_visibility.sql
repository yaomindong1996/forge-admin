-- 数据变更审计策略增加业务详情入口控制。
-- 停用采集时保留历史；该字段只控制详情是否展示历史入口，不影响证据留存。
SET @data_audit_show_in_detail_ddl := IF(
    EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_data_audit_policy'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ai_data_audit_policy'
          AND COLUMN_NAME = 'show_in_detail'
    ),
    'ALTER TABLE ai_data_audit_policy ADD COLUMN show_in_detail TINYINT NOT NULL DEFAULT 1 COMMENT ''是否在业务详情展示变更记录：1展示 0隐藏'' AFTER reason_required',
    'SELECT 1'
);
PREPARE data_audit_show_in_detail_stmt FROM @data_audit_show_in_detail_ddl;
EXECUTE data_audit_show_in_detail_stmt;
DEALLOCATE PREPARE data_audit_show_in_detail_stmt;
